package com.talhanation.recruits.client.gui.worldmap.render;

import net.minecraft.resources.ResourceLocation;

/**
 * 1.20.5+ removed the textures/map/map_icons.png atlas. Every decoration is now its own texture
 * under textures/map/decorations. Maps the old atlas index to the new texture, so the icons can be
 * drawn with full UVs (0..1).
 */
public final class MapIconTextures {
    private static final String[] NAMES = {
            "player", "frame", "red_marker", "blue_marker", "target_x", "target_point",
            "player_off_map", "player_off_limits", "woodland_mansion", "ocean_monument"
    };

    private MapIconTextures() {
    }

    public static ResourceLocation byLegacyIndex(int index) {
        String name = index >= 0 && index < NAMES.length ? NAMES[index] : NAMES[0];
        return ResourceLocation.withDefaultNamespace("textures/map/decorations/" + name + ".png");
    }
}
