package com.talhanation.recruits.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

@Mod.EventBusSubscriber
public class RecruitsClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec CLIENT;
    public static ForgeConfigSpec.BooleanValue PlayVillagerAmbientSound;
    public static ForgeConfigSpec.BooleanValue CommandScreenToggle;
    public static ForgeConfigSpec.BooleanValue RecruitsLookLikeVillagers;
    public static ForgeConfigSpec.BooleanValue UpdateCheckerClientside;


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
