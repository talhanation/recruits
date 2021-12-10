package com.talhanation.recruits.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;

@Mod.EventBusSubscriber
public class RecruitsModConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec CONFIG;
    public static ForgeConfigSpec.IntValue VERSION;
    public static final int NEW_VERSION = 2;


    public static ForgeConfigSpec.BooleanValue PlayVillagerAmbientSound;
    public static ForgeConfigSpec.BooleanValue RenderNameTagforOwner;
    public static ForgeConfigSpec.BooleanValue OverrideIronGolemSpawn;

    static{
        VERSION = BUILDER.comment("\n" +"##Version, do not change!##")
                .defineInRange("Version", 0, 0, Integer.MAX_VALUE);

        BUILDER.comment("Recruits Config:").push("Recruits");

        PlayVillagerAmbientSound = BUILDER.comment("\n" + "----Should Recruits Make Villager Huh? sound?----" + "\n" +
                "\t" + "(takes effect after restart)" + "\n" +
                "\t" + "default: true")
                .define("PlayVillagerAmbientSound", true);

        RenderNameTagforOwner = BUILDER.comment("\n" + "----Should Recruits Name Tag Render for the owner----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .define("RenderNameTagforOwner", true);

        OverrideIronGolemSpawn = BUILDER.comment("\n" + "----Should Recruits instead of Iron Golems spawn in Villages ----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: false")
                .define("OverrideIronGolemSpawn", false);


    CONFIG = BUILDER.build();
}

    public static void loadConfig(ForgeConfigSpec spec, Path path) {
        CommentedFileConfig configData = CommentedFileConfig.builder(path)
                .sync()
                .autosave()
                .writingMode(WritingMode.REPLACE)
                .build();
        configData.load();
        spec.setConfig(configData);
        if (VERSION.get() != NEW_VERSION) {
            configData = CommentedFileConfig.builder(path)
                    .sync()
                    .autosave()
                    .writingMode(WritingMode.REPLACE)
                    .build();
            spec.setConfig(configData);
            VERSION.set(NEW_VERSION);
            configData.save();
        }
    }

}

