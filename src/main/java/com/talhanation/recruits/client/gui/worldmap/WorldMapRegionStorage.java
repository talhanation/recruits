package com.talhanation.recruits.client.gui.worldmap;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Stores the live map cache as render-ready pixels.
 */
final class WorldMapRegionStorage {
    static final String DATA_EXTENSION = ".rmap";

    private static final int MAGIC = 0x524D4150;
    private static final int VERSION = 1;
    private static final int HEADER_BYTES = Integer.BYTES * 5;
    private static final int PIXEL_COUNT =
            WorldMapRegionTile.REGION_PIXEL_SIZE * WorldMapRegionTile.REGION_PIXEL_SIZE;
    private static final int FILE_BYTES = HEADER_BYTES + PIXEL_COUNT * Integer.BYTES;
    private static final ThreadLocal<ByteBuffer> IO_BUFFER = ThreadLocal.withInitial(
            () -> ByteBuffer.allocateDirect(FILE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    );

    private WorldMapRegionStorage() {
    }

    static ReadResult read(File file) {
        if (file == null || !file.exists() || !file.getName().endsWith(DATA_EXTENSION)) {
            return ReadResult.missing();
        }
        if (file.length() != FILE_BYTES) {
            return ReadResult.corrupt();
        }
        return readData(file);
    }

    static void write(File file, int[] pixels) throws IOException {
        if (pixels == null || pixels.length != PIXEL_COUNT) {
            throw new IOException("Invalid world map region pixel count");
        }
        write(file, pixelBuffer -> pixelBuffer.put(pixels));
    }

    static void write(File file, WorldMapRegionPixels.Snapshot pixels) throws IOException {
        if (pixels == null) {
            throw new IOException("Missing world map region pixels");
        }
        write(file, pixels::writeTo);
    }

    private static void write(File file, PixelWriter pixelWriter) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temporaryFile = new File(file.getParentFile(), file.getName() + ".new");
        ByteBuffer buffer = acquireBuffer();
        buffer.putInt(MAGIC);
        buffer.putInt(VERSION);
        buffer.putInt(WorldMapRegionTile.REGION_PIXEL_SIZE);
        buffer.putInt(WorldMapRegionTile.REGION_PIXEL_SIZE);
        buffer.putInt(PIXEL_COUNT);
        pixelWriter.write(buffer.asIntBuffer());
        buffer.position(FILE_BYTES);
        buffer.flip();

        try {
            try (FileChannel channel = FileChannel.open(
                    temporaryFile.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            }
            replaceFile(temporaryFile, file);
        } finally {
            if (temporaryFile.exists()) {
                temporaryFile.delete();
            }
        }
    }

    static boolean isUsable(File file) {
        return file != null
                && file.getName().endsWith(DATA_EXTENSION)
                && file.exists()
                && file.length() == FILE_BYTES;
    }

    private static ReadResult readData(File file) {
        ByteBuffer buffer = acquireBuffer();
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
                // Read the fixed-size region file fully.
            }
        } catch (IOException ignored) {
            return ReadResult.ioFailure();
        }
        if (buffer.hasRemaining()) return ReadResult.corrupt();

        buffer.flip();
        if (buffer.getInt() != MAGIC || buffer.getInt() != VERSION
                || buffer.getInt() != WorldMapRegionTile.REGION_PIXEL_SIZE
                || buffer.getInt() != WorldMapRegionTile.REGION_PIXEL_SIZE
                || buffer.getInt() != PIXEL_COUNT) {
            return ReadResult.corrupt();
        }

        WorldMapRegionPixels pixels = WorldMapRegionPixels.fromStorage(buffer.asIntBuffer());
        return pixels == null ? ReadResult.corrupt() : ReadResult.success(pixels);
    }

    private static void replaceFile(File temporaryFile, File destinationFile) throws IOException {
        try {
            Files.move(
                    temporaryFile.toPath(),
                    destinationFile.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
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

    private static ByteBuffer acquireBuffer() {
        ByteBuffer buffer = IO_BUFFER.get();
        buffer.clear();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer;
    }

    record ReadResult(WorldMapRegionPixels pixels, ReadStatus status) {
        private static ReadResult success(WorldMapRegionPixels pixels) {
            return new ReadResult(pixels, ReadStatus.SUCCESS);
        }

        private static ReadResult missing() {
            return new ReadResult(null, ReadStatus.MISSING);
        }

        private static ReadResult corrupt() {
            return new ReadResult(null, ReadStatus.CORRUPT);
        }

        private static ReadResult ioFailure() {
            return new ReadResult(null, ReadStatus.IO_FAILURE);
        }
    }

    enum ReadStatus {
        SUCCESS,
        MISSING,
        CORRUPT,
        IO_FAILURE
    }

    @FunctionalInterface
    private interface PixelWriter {
        void write(java.nio.IntBuffer target);
    }
}
