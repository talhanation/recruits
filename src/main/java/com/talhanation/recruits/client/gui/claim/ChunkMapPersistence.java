package com.talhanation.recruits.client.gui.claim;

import com.mojang.blaze3d.platform.NativeImage;
import com.talhanation.recruits.client.ClientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;public class ChunkMapPersistence {

    private static final int REGION_SIZE = 64; // 64x64 chunks pro region-file

    // Basis-Ordner: <gameDir>/recruits/chunkmaps/<storageId>/<dimension>/
    private static Path baseDir() {
        return FMLPaths.GAMEDIR.get().resolve("recruits").resolve("chunkmaps");
    }

    // storageId: für singleplayer Weltname oder multiplayer ip (ersetzt ":" -> "_")
    private static String detectStorageId() {
        try {
            Minecraft mc = Minecraft.getInstance();
            // Singleplayer (Integrierter Server vorhanden)
            if (mc.getSingleplayerServer() != null) {
                try {
                    String levelName = mc.getSingleplayerServer().getWorldData().getLevelName();
                    if (levelName != null && !levelName.isEmpty()) return sanitize(levelName);
                } catch (Exception ignored) {}
            }
            // Multiplayer ServerData
            ServerData sd = mc.getCurrentServer();
            if (sd != null && sd.ip != null && !sd.ip.isEmpty()) {
                return sanitize(sd.ip);
            }
        } catch (Throwable ignored) {}
        return "unknown";
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
    }

    private static Path dimDir(ResourceLocation dimension) {
        String storageId = detectStorageId();
        Path p = baseDir().resolve(storageId).resolve(dimension.getNamespace() + "_" + dimension.getPath());
        return p;
    }

    public static void ensureDirs(ResourceLocation dimension) throws IOException {
        Path path = dimDir(dimension);
        if (!Files.exists(path)) Files.createDirectories(path);
    }

    private static File regionFile(ResourceLocation dimension, int regionX, int regionZ) {
        Path p = dimDir(dimension).resolve("region_" + regionX + "_" + regionZ + ".nbt");
        return p.toFile();
    }

    private static int regionXForChunk(int chunkX) {
        // floorDiv (richtiges Verhalten mit negativen Koordinaten)
        return Math.floorDiv(chunkX, REGION_SIZE);
    }
    private static int regionZForChunk(int chunkZ) {
        return Math.floorDiv(chunkZ, REGION_SIZE);
    }

    /**
     * Speichert einen einzelnen Chunk in der passenden Region-Datei.
     * Überschreibt existierenden Chunk-Eintrag (ersetzt Pixel & timestamp).
     */
    public static void saveChunk(ResourceLocation dimension, ChunkPos pos, NativeImage image) {
        try {
            ensureDirs(dimension);

            int rx = regionXForChunk(pos.x);
            int rz = regionZForChunk(pos.z);
            File file = regionFile(dimension, rx, rz);

            CompoundTag regionTag = readRegionTag(file);

            CompoundTag chunksTag = regionTag.contains("chunks", Tag.TAG_COMPOUND) ? regionTag.getCompound("chunks") : new CompoundTag();

            // chunk key as absolute coords "x_z" (keine relativen coords)
            String key = pos.x + "_" + pos.z;
            CompoundTag chunkTag = new CompoundTag();

            // pixels -> int[256]
            int[] pixels = new int[16 * 16];
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    pixels[y * 16 + x] = image.getPixelRGBA(x, y);
                }
            }

            chunkTag.putIntArray("pixels", pixels);
            chunkTag.putInt("x", pos.x);
            chunkTag.putInt("z", pos.z);
            chunkTag.putLong("ts", System.currentTimeMillis());

            chunksTag.put(key, chunkTag);
            regionTag.put("chunks", chunksTag);

            // Metadaten
            regionTag.putString("dimension", dimension.toString());
            regionTag.putInt("regionX", rx);
            regionTag.putInt("regionZ", rz);
            regionTag.putLong("lastModified", System.currentTimeMillis());

            // Write compressed synchronously
            NbtIo.writeCompressed(regionTag, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lädt einen Chunk aus der entsprechenden Region-Datei, falls vorhanden.
     */
    public static Optional<NativeImage> loadChunk(ResourceLocation dimension, ChunkPos pos) {
        try {
            int rx = regionXForChunk(pos.x);
            int rz = regionZForChunk(pos.z);
            File file = regionFile(dimension, rx, rz);
            if (!file.exists()) return Optional.empty();

            CompoundTag regionTag = readRegionTag(file);
            if (!regionTag.contains("chunks", Tag.TAG_COMPOUND)) return Optional.empty();
            CompoundTag chunksTag = regionTag.getCompound("chunks");
            String key = pos.x + "_" + pos.z;
            if (!chunksTag.contains(key, Tag.TAG_COMPOUND)) return Optional.empty();

            CompoundTag chunkTag = chunksTag.getCompound(key);
            int[] pixels = chunkTag.getIntArray("pixels");
            if (pixels == null || pixels.length != 16 * 16) return Optional.empty();

            NativeImage image = new NativeImage(NativeImage.Format.RGBA, 16, 16, false);
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    image.setPixelRGBA(x, y, pixels[y * 16 + x]);
                }
            }
            image.untrack();
            return Optional.of(image);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Hilfsmethode: liest Region-CompoundTag (oder erstellt neues, falls Datei fehlt)
     */
    private static CompoundTag readRegionTag(File file) throws IOException {
        if (!file.exists()) {
            return new CompoundTag();
        }
        try {
            return NbtIo.readCompressed(file);
        } catch (IOException e) {
            // bei Lesefehler: liefere leeres Tag (Vermeidung Crash)
            e.printStackTrace();
            return new CompoundTag();
        }
    }

    /**
     * Optional: Entferne komplette Region (z. B. für Cleanup)
     */
    public static boolean deleteRegion(ResourceLocation dimension, int regionX, int regionZ) {
        File f = regionFile(dimension, regionX, regionZ);
        if (f.exists()) return f.delete();
        return false;
    }

    /**
     * Optional: Hilfsfunktion zum Umwandeln alter Einzeldateien in Region (nicht implementiert automatisch).
     * Du kannst diese nutzen, um vorhandene Einzel-NBTs zu mergen.
     */

    public static boolean chunkExists(ResourceLocation dimension, ChunkPos pos) {
        try {
            int rx = regionXForChunk(pos.x);
            int rz = regionZForChunk(pos.z);
            File file = regionFile(dimension, rx, rz);
            if (!file.exists()) return false;

            CompoundTag regionTag = readRegionTag(file);
            if (!regionTag.contains("chunks", Tag.TAG_COMPOUND)) return false;
            CompoundTag chunksTag = regionTag.getCompound("chunks");
            String key = pos.x + "_" + pos.z;
            return chunksTag.contains(key, Tag.TAG_COMPOUND);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}