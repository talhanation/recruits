package com.talhanation.recruits.client.gui.worldmap.storage;

import com.talhanation.recruits.Main;
import net.minecraft.client.multiplayer.ClientLevel;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

final class WorldMapRegionRepository {
    private final Set<String> persistedRegionSources = new HashSet<>();

    private File regionDir;

    void open(File worldMapDir) {
        this.regionDir = new File(worldMapDir, "regions");
        this.regionDir.mkdirs();
        rebuildSourceIndex();
    }

    void close() {
        persistedRegionSources.clear();
        regionDir = null;
    }

    boolean isReady() {
        return regionDir != null;
    }

    File regionDir() {
        return regionDir;
    }

    int indexedRegionCount() {
        return persistedRegionSources.size();
    }

    int indexedSourceDataCount() {
        return persistedRegionSources.size();
    }

    boolean hasSource(String regionKey) {
        return persistedRegionSources.contains(regionKey);
    }

    void recordSource(String regionKey) {
        persistedRegionSources.add(regionKey);
    }

    void recordSourceData(String regionKey) {
        recordSource(regionKey);
    }

    Set<String> indexedSourceDataRegionKeysSnapshot() {
        return new HashSet<>(persistedRegionSources);
    }

    WorldMapRegionLoadResult readRegionIfPresent(ClientLevel level, int regionX, int regionZ) {
        WorldMapSourceRegion source = readSourceIfPresent(regionX, regionZ);
        if (source == null) return null;

        WorldMapRegionPixels pixels = source.rebuildPixels(level, regionX, regionZ);
        return pixels == null ? null : new WorldMapRegionLoadResult(pixels, source);
    }

    WorldMapSourceRegion readSourceIfPresent(int regionX, int regionZ) {
        WorldMapSourceRegion source = WorldMapSourceRegionStorage.read(regionFile(regionX, regionZ));
        if (source == null || !source.hasAnySource()) return null;

        persistedRegionSources.add(WorldMapRegionKey.of(regionX, regionZ));
        return source;
    }

    void removeMissingSource(String regionKey, int regionX, int regionZ) {
        if (!WorldMapSourceRegionStorage.isUsable(regionFile(regionX, regionZ))) {
            persistedRegionSources.remove(regionKey);
        }
    }

    File regionFile(int regionX, int regionZ) {
        return sourceFile(regionDir, regionX, regionZ);
    }

    private void rebuildSourceIndex() {
        persistedRegionSources.clear();
        // Old .png cache is ignored here.
        recoverInterruptedRegionSaves(regionDir);
        indexSourceDir(regionDir);
    }

    private void recoverInterruptedRegionSaves(File sourceDir) {
        if (sourceDir == null || !sourceDir.isDirectory()) return;

        File[] temporaryFiles = sourceDir.listFiles(file ->
                file.getName().endsWith(WorldMapSourceRegionStorage.DATA_EXTENSION + ".new"));
        if (temporaryFiles == null) return;

        for (File temporaryFile : temporaryFiles) {
            String originalName = temporaryFile.getName().substring(0, temporaryFile.getName().length() - 4);
            File destinationFile = new File(sourceDir, originalName);
            if (destinationFile.exists()) {
                temporaryFile.delete();
                continue;
            }
            recoverInterruptedRegionSave(temporaryFile, destinationFile);
        }
    }

    private void recoverInterruptedRegionSave(File temporaryFile, File destinationFile) {
        try {
            try {
                Files.move(temporaryFile.toPath(), destinationFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile.toPath(), destinationFile.toPath());
            }
        } catch (IOException exception) {
            Main.LOGGER.debug("Could not recover interrupted world map region save {}", temporaryFile, exception);
        }
    }

    private void indexSourceDir(File sourceDir) {
        if (sourceDir == null || !sourceDir.isDirectory()) return;

        File[] files = sourceDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            String regionKey = sourceDataKeyFromFileName(file);
            if (regionKey != null) {
                persistedRegionSources.add(regionKey);
            }
        }
    }

    private static String sourceDataKeyFromFileName(File file) {
        if (!WorldMapSourceRegionStorage.isUsable(file)) return null;

        String name = file.getName();
        return regionKeyFromCoords(
                name.substring(0, name.length() - WorldMapSourceRegionStorage.DATA_EXTENSION.length()));
    }

    private static String regionKeyFromCoords(String coords) {
        int separator = coords.indexOf('_');
        if (separator <= 0 || separator >= coords.length() - 1) return null;

        try {
            int regionX = Integer.parseInt(coords.substring(0, separator));
            int regionZ = Integer.parseInt(coords.substring(separator + 1));
            return WorldMapRegionKey.of(regionX, regionZ);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static File sourceFile(File sourceDir, int regionX, int regionZ) {
        return sourceDir == null
                ? null
                : new File(sourceDir, regionX + "_" + regionZ + WorldMapSourceRegionStorage.DATA_EXTENSION);
    }

    record WorldMapRegionLoadResult(WorldMapRegionPixels pixels, WorldMapSourceRegion source) {}
}
