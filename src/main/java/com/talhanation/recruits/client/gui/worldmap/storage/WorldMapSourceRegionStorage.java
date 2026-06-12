package com.talhanation.recruits.client.gui.worldmap.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * .rmap layout: deflate(header, chunk bitset, palettes, chunk samples).
 */
public final class WorldMapSourceRegionStorage {
    static final String DATA_EXTENSION = ".rmap";

    private static final int MAGIC = 0x52535243;
    private static final int VERSION = 1;
    private static final int CHUNK_COUNT =
            WorldMapRegion.CHUNKS_PER_REGION * WorldMapRegion.CHUNKS_PER_REGION;
    private static final int BITSET_LONGS = CHUNK_COUNT / Long.SIZE;
    private static final int MAX_UNSIGNED_SHORT = 0xFFFF;

    private WorldMapSourceRegionStorage() {}

    static boolean isUsable(File file) {
        if (file == null
                || !file.getName().endsWith(DATA_EXTENSION)
                || !file.exists()
                || file.length() <= 0L) {
            return false;
        }

        try (DataInputStream input =
                new DataInputStream(
                        new BufferedInputStream(
                                new InflaterInputStream(new FileInputStream(file))))) {
            return input.readInt() == MAGIC
                    && input.readInt() == VERSION
                    && input.readInt() == WorldMapRegion.CHUNKS_PER_REGION
                    && input.readInt() == WorldMapRegion.PIXELS_PER_CHUNK;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    static WorldMapSourceRegion read(File file) {
        if (!isUsable(file)) return null;

        try (DataInputStream input =
                new DataInputStream(
                        new BufferedInputStream(
                                new InflaterInputStream(new FileInputStream(file))))) {
            if (input.readInt() != MAGIC
                    || input.readInt() != VERSION
                    || input.readInt() != WorldMapRegion.CHUNKS_PER_REGION
                    || input.readInt() != WorldMapRegion.PIXELS_PER_CHUNK) {
                return null;
            }

            long[] presentChunks = new long[BITSET_LONGS];
            for (int index = 0; index < presentChunks.length; index++) {
                presentChunks[index] = input.readLong();
            }

            int[] blockPalette = readPalette(input);
            int[] biomePalette = readPalette(input);
            WorldMapSourceRegion region = WorldMapSourceRegion.blank();
            for (int chunkIndex = 0; chunkIndex < CHUNK_COUNT; chunkIndex++) {
                if (!isPresent(presentChunks, chunkIndex)) continue;

                WorldMapSourceChunk chunk = readChunk(input, blockPalette, biomePalette);
                if (chunk == null) return null;

                region.updateChunk(
                        chunk,
                        chunkIndex % WorldMapRegion.CHUNKS_PER_REGION,
                        chunkIndex / WorldMapRegion.CHUNKS_PER_REGION);
            }
            return region;
        } catch (EOFException ignored) {
            return null;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    public static void write(File file, WorldMapSourceRegion.Snapshot snapshot) throws IOException {
        if (snapshot == null || snapshot.chunks() == null) {
            throw new IOException("Missing world map source region");
        }

        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temporaryFile = new File(file.getParentFile(), file.getName() + ".new");

        try {
            writeData(temporaryFile, snapshot);
            replaceFile(temporaryFile, file);
        } finally {
            if (temporaryFile.exists()) {
                temporaryFile.delete();
            }
        }
    }

    private static void writeData(File file, WorldMapSourceRegion.Snapshot snapshot) throws IOException {
        Palette blockPalette = new Palette();
        Palette biomePalette = new Palette();
        // Local palettes keep repeated block/biome ids cheap.
        long[] presentChunks = buildPalettesAndBitset(snapshot, blockPalette, biomePalette);

        try (DataOutputStream output =
                new DataOutputStream(
                        new BufferedOutputStream(
                                new DeflaterOutputStream(
                                        new FileOutputStream(file),
                                        new Deflater(Deflater.BEST_SPEED))))) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(WorldMapRegion.CHUNKS_PER_REGION);
            output.writeInt(WorldMapRegion.PIXELS_PER_CHUNK);
            for (long bits : presentChunks) {
                output.writeLong(bits);
            }
            writePalette(output, blockPalette.values);
            writePalette(output, biomePalette.values);

            WorldMapSourceChunk[] chunks = snapshot.chunks();
            for (int chunkIndex = 0; chunkIndex < CHUNK_COUNT; chunkIndex++) {
                if (!isPresent(presentChunks, chunkIndex)) continue;

                WorldMapSourceChunk chunk = chunks[chunkIndex];
                writeSamples(output, chunk.surfaceStates(), blockPalette, false);
                writeSamples(output, chunk.surfaceBiomes(), biomePalette, true);
                writeHeights(output, chunk.surfaceHeights(), chunk.surfaceStates());
                writeSamples(output, chunk.underlayStates(), blockPalette, false);
                writeSamples(output, chunk.underlayBiomes(), biomePalette, true);
                writeHeights(output, chunk.underlayHeights(), chunk.underlayStates());
            }
        }
    }

    private static WorldMapSourceChunk readChunk(
            DataInputStream input, int[] blockPalette, int[] biomePalette) throws IOException {
        int[] surfaceStates = readSamples(input, blockPalette);
        int[] surfaceBiomes = readSamples(input, biomePalette);
        int[] surfaceHeights = readHeights(input, surfaceStates);
        int[] underlayStates = readSamples(input, blockPalette);
        int[] underlayBiomes = readSamples(input, biomePalette);
        int[] underlayHeights = readHeights(input, underlayStates);

        WorldMapSourceChunk.Builder builder = WorldMapSourceChunk.builder();
        for (int index = 0; index < WorldMapSourceChunk.SAMPLE_COUNT; index++) {
            if (surfaceStates[index] >= 0) {
                builder.setSurface(index, surfaceStates[index], surfaceBiomes[index], surfaceHeights[index]);
            }
            if (underlayStates[index] >= 0) {
                builder.setUnderlay(index, underlayStates[index], underlayBiomes[index], underlayHeights[index]);
            }
        }
        return builder.build();
    }

    private static int[] readPalette(DataInputStream input) throws IOException {
        int count = input.readInt();
        if (count < 0 || count > MAX_UNSIGNED_SHORT) throw new IOException("Invalid palette size");

        int[] palette = new int[count];
        for (int index = 0; index < count; index++) {
            palette[index] = input.readInt();
        }
        return palette;
    }

    private static void writePalette(DataOutputStream output, ArrayList<Integer> palette) throws IOException {
        output.writeInt(palette.size());
        for (Integer value : palette) {
            output.writeInt(value);
        }
    }

    private static int[] readSamples(DataInputStream input, int[] palette) throws IOException {
        int[] values = new int[WorldMapSourceChunk.SAMPLE_COUNT];
        for (int index = 0; index < values.length; index++) {
            int paletteIndex = input.readUnsignedShort();
            if (paletteIndex == 0) {
                values[index] = WorldMapSourceChunk.MISSING;
                continue;
            }
            if (paletteIndex > palette.length) throw new IOException("Invalid palette reference");
            values[index] = palette[paletteIndex - 1];
        }
        return values;
    }

    private static int[] readHeights(DataInputStream input, int[] states) throws IOException {
        int[] heights = new int[WorldMapSourceChunk.SAMPLE_COUNT];
        for (int index = 0; index < heights.length; index++) {
            int height = input.readShort();
            heights[index] = states[index] < 0 ? WorldMapSourceChunk.MISSING : height;
        }
        return heights;
    }

    private static void writeSamples(
            DataOutputStream output, int[] samples, Palette palette, boolean allowMissingPalette) throws IOException {
        for (int sample : samples) {
            if (sample < 0) {
                output.writeShort(0);
                continue;
            }
            int paletteIndex = palette.index(sample);
            if (paletteIndex < 0 && allowMissingPalette) {
                output.writeShort(0);
            } else if (paletteIndex < 0) {
                throw new IOException("Missing world map source palette entry");
            } else {
                output.writeShort(paletteIndex + 1);
            }
        }
    }

    private static void writeHeights(DataOutputStream output, int[] heights, int[] states) throws IOException {
        for (int index = 0; index < heights.length; index++) {
            output.writeShort(states[index] < 0 ? Short.MIN_VALUE : clampToShort(heights[index]));
        }
    }

    private static long[] buildPalettesAndBitset(
            WorldMapSourceRegion.Snapshot snapshot, Palette blockPalette, Palette biomePalette) throws IOException {
        long[] presentChunks = new long[BITSET_LONGS];
        WorldMapSourceChunk[] chunks = snapshot.chunks();
        if (chunks.length != CHUNK_COUNT) throw new IOException("Invalid source region chunk count");

        for (int chunkIndex = 0; chunkIndex < chunks.length; chunkIndex++) {
            WorldMapSourceChunk chunk = chunks[chunkIndex];
            if (chunk == null) continue;

            setPresent(presentChunks, chunkIndex);
            addSamples(blockPalette, chunk.surfaceStates());
            addSamples(blockPalette, chunk.underlayStates());
            addSamples(biomePalette, chunk.surfaceBiomes());
            addSamples(biomePalette, chunk.underlayBiomes());
        }
        return presentChunks;
    }

    private static void addSamples(Palette palette, int[] samples) throws IOException {
        for (int sample : samples) {
            if (sample >= 0) {
                palette.add(sample);
            }
        }
    }

    private static boolean isPresent(long[] bits, int chunkIndex) {
        return (bits[chunkIndex / Long.SIZE] & (1L << (chunkIndex % Long.SIZE))) != 0L;
    }

    private static void setPresent(long[] bits, int chunkIndex) {
        bits[chunkIndex / Long.SIZE] |= 1L << (chunkIndex % Long.SIZE);
    }

    private static int clampToShort(int value) {
        return Math.max(Short.MIN_VALUE + 1, Math.min(Short.MAX_VALUE, value));
    }

    private static void replaceFile(File temporaryFile, File destinationFile) throws IOException {
        try {
            Files.move(
                    temporaryFile.toPath(),
                    destinationFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            moveReplacing(temporaryFile, destinationFile);
        } catch (IOException atomicMoveFailure) {
            try {
                moveReplacing(temporaryFile, destinationFile);
            } catch (IOException fallbackFailure) {
                fallbackFailure.addSuppressed(atomicMoveFailure);
                throw fallbackFailure;
            }
        }
    }

    private static void moveReplacing(File sourceFile, File destinationFile) throws IOException {
        Files.move(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static final class Palette {
        private final ArrayList<Integer> values = new ArrayList<>();
        private final Map<Integer, Integer> indexes = new HashMap<>();

        void add(int value) throws IOException {
            if (indexes.containsKey(value)) return;
            if (values.size() >= MAX_UNSIGNED_SHORT) throw new IOException("World map source palette is too large");

            indexes.put(value, values.size());
            values.add(value);
        }

        int index(int value) {
            return indexes.getOrDefault(value, -1);
        }
    }
}
