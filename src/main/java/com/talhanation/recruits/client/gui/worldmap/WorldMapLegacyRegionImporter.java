package com.talhanation.recruits.client.gui.worldmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

final class WorldMapLegacyRegionImporter {
    static final String IMAGE_EXTENSION = ".png";

    private WorldMapLegacyRegionImporter() {
    }

    static int[] read(File file) {
        if (!isUsable(file)) return null;

        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null
                    || image.getWidth() != WorldMapRegionTile.REGION_PIXEL_SIZE
                    || image.getHeight() != WorldMapRegionTile.REGION_PIXEL_SIZE) {
                return null;
            }

            int[] pixels = image.getRGB(
                    0,
                    0,
                    WorldMapRegionTile.REGION_PIXEL_SIZE,
                    WorldMapRegionTile.REGION_PIXEL_SIZE,
                    null,
                    0,
                    WorldMapRegionTile.REGION_PIXEL_SIZE
            );
            for (int index = 0; index < pixels.length; index++) {
                pixels[index] = argbToNativeRgba(pixels[index]);
            }
            return pixels;
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    static boolean isUsable(File file) {
        return file != null
                && file.getName().endsWith(IMAGE_EXTENSION)
                && file.exists()
                && file.length() > 0;
    }

    private static int argbToNativeRgba(int argb) {
        return argb & 0xFF00FF00
                | argb >>> 16 & 0x000000FF
                | argb << 16 & 0x00FF0000;
    }
}
