package com.talhanation.recruits.client.gui.worldmap.storage;

import com.talhanation.recruits.Main;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

final class WorldMapRegionRepository {
    private final Set<String> persistedRegionSources = new HashSet<>();

    private File worldMapDir;
    private File regionDir;
    private File legacyLevelZeroDir;

    void open(File worldMapDir) {
        this.worldMapDir = worldMapDir;
        this.regionDir = new File(worldMapDir, "regions");
        this.legacyLevelZeroDir = new File(this.regionDir, "l0");
        this.regionDir.mkdirs();
        rebuildSourceIndex();
    }

    void close() {
        persistedRegionSources.clear();
        worldMapDir = null;
        regionDir = null;
        legacyLevelZeroDir = null;
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

    boolean hasSource(String regionKey) {
        return persistedRegionSources.contains(regionKey);
    }

    void recordSource(String regionKey) {
        persistedRegionSources.add(regionKey);
    }

    WorldMapRegionPixels readPixelsIfPresent(int regionX, int regionZ) {
        File activeRegionFile = regionFile(regionX, regionZ);
        for (File candidate : loadCandidates(regionX, regionZ)) {
            WorldMapRegionStorage.ReadResult result = WorldMapRegionStorage.read(candidate);
            if (result.status() == WorldMapRegionStorage.ReadStatus.SUCCESS) {
                return result.pixels();
            }
            if (result.status() == WorldMapRegionStorage.ReadStatus.CORRUPT) {
                quarantineCorruptRegion(candidate);
            }

            int[] pixels = WorldMapLegacyRegionImporter.read(candidate);
            if (pixels == null) continue;

            migrateLegacyRegion(activeRegionFile, pixels);
            return WorldMapRegionPixels.fromPackedPixels(pixels);
        }
        return null;
    }

    void removeMissingSource(String regionKey, int regionX, int regionZ) {
        for (File candidate : loadCandidates(regionX, regionZ)) {
            if (WorldMapRegionStorage.isUsable(candidate)
                    || WorldMapLegacyRegionImporter.isUsable(candidate)) {
                return;
            }
        }
        persistedRegionSources.remove(regionKey);
    }

    File regionFile(int regionX, int regionZ) {
        return regionFile(regionDir, regionX, regionZ);
    }

    private void rebuildSourceIndex() {
        persistedRegionSources.clear();
        recoverInterruptedRegionSaves(regionDir);
        indexSourceDir(regionDir);
        recoverInterruptedRegionSaves(legacyLevelZeroDir);
        indexSourceDir(legacyLevelZeroDir);
        if (worldMapDir != null && !worldMapDir.equals(regionDir)) {
            recoverInterruptedRegionSaves(worldMapDir);
            indexSourceDir(worldMapDir);
        }
    }

    private void recoverInterruptedRegionSaves(File sourceDir) {
        if (sourceDir == null || !sourceDir.isDirectory()) return;

        File[] temporaryFiles = sourceDir.listFiles(file ->
                file.getName().endsWith(WorldMapRegionStorage.DATA_EXTENSION + ".new")
                        || file.getName().endsWith(WorldMapLegacyRegionImporter.IMAGE_EXTENSION + ".new"));
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
            String regionKey = regionKeyFromFileName(file);
            if (regionKey != null) {
                persistedRegionSources.add(regionKey);
            }
        }
    }

    private static String regionKeyFromFileName(File file) {
        if (!WorldMapRegionStorage.isUsable(file) && !WorldMapLegacyRegionImporter.isUsable(file))
            return null;

        String name = file.getName();
        String extension;
        if (name.endsWith(WorldMapRegionStorage.DATA_EXTENSION)) {
            extension = WorldMapRegionStorage.DATA_EXTENSION;
        } else if (name.endsWith(WorldMapLegacyRegionImporter.IMAGE_EXTENSION)) {
            extension = WorldMapLegacyRegionImporter.IMAGE_EXTENSION;
        } else {
            return null;
        }

        String coords = name.substring(0, name.length() - extension.length());
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

    private void quarantineCorruptRegion(File file) {
        if (file == null || !file.exists()) return;

        File quarantined = new File(file.getParentFile(), file.getName() + ".corrupt-" + System.currentTimeMillis());
        try {
            Files.move(file.toPath(), quarantined.toPath());
            Main.LOGGER.warn("Moved corrupt world map region {} to {}", file, quarantined);
        } catch (IOException exception) {
            Main.LOGGER.warn("Could not quarantine corrupt world map region {}", file, exception);
        }
    }

    private void migrateLegacyRegion(File activeRegionFile, int[] pixels) {
        if (activeRegionFile == null) return;

        try {
            WorldMapRegionStorage.write(activeRegionFile, pixels);
        } catch (Exception exception) {
            Main.LOGGER.debug("Could not migrate legacy world map region {}", activeRegionFile, exception);
        }
    }

    private File[] loadCandidates(int regionX, int regionZ) {
        return new File[] {
            regionFile(regionDir, regionX, regionZ),
            legacyRegionFile(regionDir, regionX, regionZ),
            regionFile(legacyLevelZeroDir, regionX, regionZ),
            legacyRegionFile(legacyLevelZeroDir, regionX, regionZ),
            regionFile(worldMapDir, regionX, regionZ),
            legacyRegionFile(worldMapDir, regionX, regionZ)
        };
    }

    private static File regionFile(File sourceDir, int regionX, int regionZ) {
        return sourceDir == null
                ? null
                : new File(sourceDir, regionX + "_" + regionZ + WorldMapRegionStorage.DATA_EXTENSION);
    }

    private static File legacyRegionFile(File sourceDir, int regionX, int regionZ) {
        return sourceDir == null
                ? null
                : new File(sourceDir, regionX + "_" + regionZ + WorldMapLegacyRegionImporter.IMAGE_EXTENSION);
    }
}
