package com.talhanation.recruits.client.gui.claim;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Region-based asynchronous chunk persistence.
 * Region size = 32x32 chunks.
 *
 * Public API:
 *   saveChunkAsync(dimension, pos, image)
 *   Optional<NativeImage> loadChunkSync(dimension, pos)
 *   boolean chunkExists(dimension, pos)
 *   void close()
 */
public class ChunkMapPersistence {
    private static final int REGION_SIZE = 32;
    private static final int MAX_CACHED_REGIONS = 64; // LRU cache size
    private static final long FLUSH_DEBOUNCE_MS = 800L; // debounce before flush, batching

    // Base: <gameDir>/recruits/chunkmaps/<storageId>/<dimension>/
    private static Path baseDir() {
        return FMLPaths.GAMEDIR.get().resolve("recruits").resolve("chunkmaps");
    }

    // storageId: world name for singleplayer or sanitized server ip
    private static String detectStorageId() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSingleplayerServer() != null) {
                try {
                    String levelName = mc.getSingleplayerServer().getWorldData().getLevelName();
                    if (levelName != null && !levelName.isEmpty()) return sanitize(levelName);
                } catch (Exception ignored) {}
            }
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
        return baseDir().resolve(storageId).resolve(dimension.getNamespace() + "_" + dimension.getPath());
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
        return Math.floorDiv(chunkX, REGION_SIZE);
    }

    private static int regionZForChunk(int chunkZ) {
        return Math.floorDiv(chunkZ, REGION_SIZE);
    }

    // ---------- Async writer infra ----------
    private static final ScheduledExecutorService WRITER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "recruits-chunkmap-writer");
        t.setDaemon(true);
        return t;
    });

    // queue of region keys that need flush; scheduling handles debounce
    private static final ConcurrentLinkedQueue<RegionKey> flushQueue = new ConcurrentLinkedQueue<>();

    // LRU cache for region CompoundTags (synchronized access)
    private static final LinkedHashMap<RegionKey, RegionEntry> regionCache = new LinkedHashMap<RegionKey, RegionEntry>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<RegionKey, RegionEntry> eldest) {
            if (size() > MAX_CACHED_REGIONS) {
                RegionEntry entry = eldest.getValue();
                // if dirty, schedule flush synchronously-ish (enqueue and let writer handle)
                if (entry != null && entry.isDirty()) {
                    enqueueFlush(eldest.getKey());
                }
                // Release memory reference (actual flush done async)
                return true;
            }
            return false;
        }
    };

    // synchronize access to regionCache map
    private static final Object CACHE_LOCK = new Object();

    // helper: schedule flush with debounce
    private static void enqueueFlush(RegionKey key) {
        // Add to queue; schedule worker to process queue after debounce.
        flushQueue.add(key);
        WRITER.schedule(ChunkMapPersistence::processFlushQueue, FLUSH_DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private static void processFlushQueue() {
        // drain queue unique keys
        Set<RegionKey> toFlush = new HashSet<>();
        RegionKey k;
        while ((k = flushQueue.poll()) != null) toFlush.add(k);
        if (toFlush.isEmpty()) return;

        for (RegionKey rk : toFlush) {
            RegionEntry entry;
            synchronized (CACHE_LOCK) {
                entry = regionCache.get(rk);
            }
            if (entry != null) {
                // perform flush
                WRITER.submit(() -> {
                    try {
                        entry.flushToDisk(rk);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } else {
                // region not in cache: try to load & flush? skip
            }
        }
    }

    // ---------- Public API ----------

    /**
     * Public API: non-blocking. Serializes & enqueues region update for async flush.
     */
    public static void saveChunkAsync(ResourceLocation dimension, ChunkPos pos, NativeImage image) {
        // prepare chunkTag (int[256]) synchronously (cheap)
        CompoundTag chunkTag = pixelImageToChunkTag(pos, image);

        int rx = regionXForChunk(pos.x);
        int rz = regionZForChunk(pos.z);
        RegionKey rk = new RegionKey(dimension, rx, rz);

        RegionEntry regionEntry;
        synchronized (CACHE_LOCK) {
            regionEntry = regionCache.get(rk);
            if (regionEntry == null) {
                // load existing region or create new
                CompoundTag regionTag = readRegionTagSafe(regionFile(dimension, rx, rz));
                regionEntry = new RegionEntry(regionTag);
                regionCache.put(rk, regionEntry);
            }
            // put/replace chunk compound into regionTag
            regionEntry.setChunkTag(pos, chunkTag);
        }

        // schedule flush of that region (debounced)
        enqueueFlush(rk);
    }

    /**
     * Blocking load (sync) - reads region file or returns from cache if present.
     * Useful for rendering path: call on client thread but it's IO-bound; keep load calls rare.
     */
    public static Optional<NativeImage> loadChunkSync(ResourceLocation dimension, ChunkPos pos) {
        int rx = regionXForChunk(pos.x);
        int rz = regionZForChunk(pos.z);
        RegionKey rk = new RegionKey(dimension, rx, rz);

        // Try cache first
        RegionEntry regionEntry;
        synchronized (CACHE_LOCK) {
            regionEntry = regionCache.get(rk);
            if (regionEntry == null) {
                // load file synchronously
                CompoundTag regionTag = readRegionTagSafe(regionFile(dimension, rx, rz));
                if (regionTag == null || !regionTag.contains("chunks", Tag.TAG_COMPOUND)) {
                    return Optional.empty();
                }
                regionEntry = new RegionEntry(regionTag);
                regionCache.put(rk, regionEntry);
            }
        }

        // extract chunk tag from regionEntry
        CompoundTag chunkTag = regionEntry.getChunkTag(pos);
        if (chunkTag == null) return Optional.empty();

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
    }

    /**
     * Compatibility helper: if existing code calls saveChunk(...) synchronously, it will delegate to async.
     */
    public static void saveChunk(ResourceLocation dimension, ChunkPos pos, NativeImage image) {
        saveChunkAsync(dimension, pos, image);
    }

    /**
     * Synchronous existence check. Checks cache then file.
     */
    public static boolean chunkExists(ResourceLocation dimension, ChunkPos pos) {
        int rx = regionXForChunk(pos.x);
        int rz = regionZForChunk(pos.z);
        RegionKey rk = new RegionKey(dimension, rx, rz);

        synchronized (CACHE_LOCK) {
            RegionEntry entry = regionCache.get(rk);
            if (entry != null && entry.hasChunk(pos)) return true;
        }

        File f = regionFile(dimension, rx, rz);
        if (!f.exists()) return false;

        try {
            CompoundTag regionTag = readRegionTagSafe(f);
            if (regionTag == null) return false;
            if (!regionTag.contains("chunks", Tag.TAG_COMPOUND)) return false;
            CompoundTag chunks = regionTag.getCompound("chunks");
            String key = chunkKey(pos);
            return chunks.contains(key, Tag.TAG_COMPOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Call on shutdown/unload to flush and stop writer.
     */
    public static void close() {
        // flush all cached regions synchronously
        List<RegionKey> keys;
        synchronized (CACHE_LOCK) {
            keys = new ArrayList<>(regionCache.keySet());
        }
        for (RegionKey k : keys) {
            RegionEntry e;
            synchronized (CACHE_LOCK) {
                e = regionCache.get(k);
            }
            if (e != null) {
                try {
                    e.flushToDisk(k);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        WRITER.shutdown();
        try {
            if (!WRITER.awaitTermination(2, TimeUnit.SECONDS)) {
                WRITER.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            WRITER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ---------- Helpers & region internals ----------

    private static String chunkKey(ChunkPos pos) {
        return pos.x + "_" + pos.z;
    }

    private static CompoundTag pixelImageToChunkTag(ChunkPos pos, NativeImage image) {
        CompoundTag chunkTag = new CompoundTag();
        int[] pixels = new int[16 * 16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                pixels[y * 16 + x] = image.getPixelRGBA(x, y);
            }
        }
        chunkTag.putIntArray("pixels", pixels);
        chunkTag.putInt("x", pos.x);
        chunkTag.putInt("z", pos.z);
        chunkTag.putLong("ts", Instant.now().toEpochMilli());
        return chunkTag;
    }

    /**
     * Read region file safely. If file missing -> returns new CompoundTag()
     * If parse error -> rename file to .corrupt and return new CompoundTag()
     */
    private static CompoundTag readRegionTagSafe(File file) {
        if (!file.exists()) return new CompoundTag();
        try {
            CompoundTag t = NbtIo.readCompressed(file);
            return t == null ? new CompoundTag() : t;
        } catch (IOException e) {
            e.printStackTrace();
            // try to rename corrupt file to preserve for debugging
            try {
                Path source = file.toPath();
                Path corrupt = source.resolveSibling(source.getFileName().toString() + ".corrupt");
                Files.move(source, corrupt, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return new CompoundTag();
        }
    }

    // write regionTag atomically to disk (tmp -> move)
    private static void writeRegionTagAtomic(File file, CompoundTag regionTag) throws IOException {
        ensureDirs(new ResourceLocation(regionTag.getString("dimension") == null || regionTag.getString("dimension").isEmpty() ? "minecraft:overworld" : regionTag.getString("dimension")));
        Path target = file.toPath();
        Path tmp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.TRUNCATE_EXISTING)) {
            NbtIo.writeCompressed(regionTag, os);
            os.flush();
        }
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException amne) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // small RegionKey record
    private static class RegionKey {
        final ResourceLocation dim;
        final int rx;
        final int rz;
        RegionKey(ResourceLocation dim, int rx, int rz) { this.dim = dim; this.rx = rx; this.rz = rz; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RegionKey)) return false;
            RegionKey r = (RegionKey) o;
            return rx == r.rx && rz == r.rz && Objects.equals(dim, r.dim);
        }
        @Override
        public int hashCode() { return Objects.hash(dim, rx, rz); }
    }

    // RegionEntry holds the in-memory CompoundTag for a region and dirty flag
    private static class RegionEntry {
        private final CompoundTag regionTag;
        private volatile boolean dirty = false;

        RegionEntry(CompoundTag tag) {
            this.regionTag = tag != null ? tag : new CompoundTag();
        }

        boolean isDirty() { return dirty; }

        synchronized void setChunkTag(ChunkPos pos, CompoundTag chunkTag) {
            CompoundTag chunks = regionTag.contains("chunks", Tag.TAG_COMPOUND) ? regionTag.getCompound("chunks") : new CompoundTag();
            String key = chunkKey(pos);
            chunks.put(key, chunkTag);
            regionTag.put("chunks", chunks);
            regionTag.putLong("lastModified", Instant.now().toEpochMilli());
            regionTag.putString("dimension", regionTag.contains("dimension") ? regionTag.getString("dimension") : "unknown");
            dirty = true;
        }

        synchronized CompoundTag getChunkTag(ChunkPos pos) {
            if (!regionTag.contains("chunks", Tag.TAG_COMPOUND)) return null;
            CompoundTag chunks = regionTag.getCompound("chunks");
            String key = chunkKey(pos);
            if (!chunks.contains(key, Tag.TAG_COMPOUND)) return null;
            return chunks.getCompound(key);
        }

        synchronized boolean hasChunk(ChunkPos pos) {
            if (!regionTag.contains("chunks", Tag.TAG_COMPOUND)) return false;
            return regionTag.getCompound("chunks").contains(chunkKey(pos), Tag.TAG_COMPOUND);
        }

        synchronized void flushToDisk(RegionKey rk) throws IOException {
            if (!dirty) return;
            // Ensure dimension stored in tag
            regionTag.putString("dimension", rk.dim.toString());
            regionTag.putInt("regionX", rk.rx);
            regionTag.putInt("regionZ", rk.rz);
            regionTag.putLong("lastModified", Instant.now().toEpochMilli());
            File f = regionFile(rk.dim, rk.rx, rk.rz);
            writeRegionTagAtomic(f, regionTag);
            dirty = false;
        }
    }
}
