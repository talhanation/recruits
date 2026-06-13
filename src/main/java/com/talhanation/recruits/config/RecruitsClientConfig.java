package com.talhanation.recruits.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

@Mod.EventBusSubscriber
public class RecruitsClientConfig {
    public enum MapPlayerIconStyle {
        VANILLA,
        OVERHAULED
    }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec CLIENT;
    public static ForgeConfigSpec.BooleanValue PlayVillagerAmbientSound;
    public static ForgeConfigSpec.BooleanValue CommandScreenToggle;
    public static ForgeConfigSpec.BooleanValue RecruitsLookLikeVillagers;
    public static ForgeConfigSpec.BooleanValue UpdateCheckerClientside;
    public static ForgeConfigSpec.BooleanValue DisableClaimGUIOverlay;
    public static ForgeConfigSpec.BooleanValue UpdateMapTiles;
    public static ForgeConfigSpec.BooleanValue WorldMapUpdateAroundPlayer;
    public static ForgeConfigSpec.BooleanValue WorldMapNightShading;
    public static ForgeConfigSpec.BooleanValue WorldMapShowCoordinates;
    public static ForgeConfigSpec.BooleanValue WorldMapClaimFill;
    public static ForgeConfigSpec.EnumValue<MapPlayerIconStyle> WorldMapPlayerIconStyle;

    static{
        BUILDER.comment("Recruits Config Client Side:").push("RecruitsClientSide");

        PlayVillagerAmbientSound = BUILDER.comment("""

                        ----Should Recruits Make Villager "Huh?" sound?----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("PlayVillagerAmbientSound", true);

        RecruitsLookLikeVillagers = BUILDER.comment("""

                        ----Should Recruits look like Villagers?----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("RecruitsLookLikeVillagers", true);

        CommandScreenToggle = BUILDER.comment("""
                        ----CommandScreenToggle----
                        \t(takes effect after restart)
                        \t
                        Should the key to open the command screen be toggled instead of held?""
                        default: false""")

                .worldRestart()
                .define("CommandScreenToggle", false);

        UpdateCheckerClientside = BUILDER.comment("""
                        ----UpdateCheckerClientside----
                        \t(takes effect after restart)
                        \t
                        Should the client side update checker be active?""
                        It is recommended to keep it enabled to receive information about new bug fixes and features.""
                        default: true""")

                .worldRestart()
                .define("UpdateCheckerClientside", true);

        DisableClaimGUIOverlay = BUILDER.comment("""
                        ----DisableClaimGUIOverlay----
                        \t(takes effect after restart)
                        \t
                        Should the GUI overlay with claim informations be disabled?""
                        default: false""")

                .worldRestart()
                .define("DisableClaimGUIOverlay", false);

        UpdateMapTiles = BUILDER.comment("""
                        ----UpdateMapTiles----
                        \t(takes effect after restart)
                        \t
                        Should the world map tiles be updated while playing?
                        Disable this if you experience performance issues with the map.""
                        default: true""")
                .worldRestart()
                .define("UpdateMapTiles", true);

        WorldMapUpdateAroundPlayer = BUILDER.comment("""
                        ----WorldMapUpdateAroundPlayer----
                        Load and refresh map chunks around the player.
                        default: true""")
                .define("WorldMapUpdateAroundPlayer", true);

        WorldMapNightShading = BUILDER.comment("""
                        ----WorldMapNightShading----
                        Darken the world map at night.
                        default: true""")
                .define("WorldMapNightShading", true);

        WorldMapShowCoordinates = BUILDER.comment("""
                        ----WorldMapShowCoordinates----
                        Show the coordinates and zoom readout on the world map.
                        default: true""")
                .define("WorldMapShowCoordinates", true);

        WorldMapClaimFill = BUILDER.comment("""
                        ----WorldMapClaimFill----
                        Draw the filled territory overlay on the world map.
                        default: true""")
                .define("WorldMapClaimFill", true);

        WorldMapPlayerIconStyle = BUILDER.comment("""
                        ----WorldMapPlayerIconStyle----
                        Player icon style on the world map.
                        default: OVERHAULED""")
                .defineEnum("WorldMapPlayerIconStyle", MapPlayerIconStyle.OVERHAULED);

        BUILDER.pop();
        CLIENT = BUILDER.build();
    }


    public static void loadConfig(ForgeConfigSpec spec, Path path) {
        CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();
        configData.load();
        spec.setConfig(configData);
    }
}
