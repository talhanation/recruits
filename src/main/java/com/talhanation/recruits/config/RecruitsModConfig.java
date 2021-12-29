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
    public static final int NEW_VERSION = 4;


    public static ForgeConfigSpec.BooleanValue PlayVillagerAmbientSound;
    public static ForgeConfigSpec.BooleanValue RenderNameTagforOwner;
    public static ForgeConfigSpec.BooleanValue OverrideIronGolemSpawn;
    public static ForgeConfigSpec.BooleanValue PillagerFriendlyFire;
    public static ForgeConfigSpec.BooleanValue PillagerSpawn;
    public static ForgeConfigSpec.BooleanValue PillagerAttackMonsters;
    public static ForgeConfigSpec.BooleanValue ShouldPillagersRaidNaturally;
    public static ForgeConfigSpec.BooleanValue MonstersAttackPillagers;
    public static ForgeConfigSpec.IntValue MaxSpawnRecruitsInVillage;
    public static ForgeConfigSpec.IntValue MaxRecruitsForPlayer;
    public static ForgeConfigSpec.IntValue RecruitsMaxXpForLevelUp;

    static{
        VERSION = BUILDER.comment("\n" +"##Version, do not change!##")
                .defineInRange("Version", 0, 0, Integer.MAX_VALUE);

        BUILDER.comment("Recruits Config:").push("Recruits");

        PlayVillagerAmbientSound = BUILDER.comment("\n" + "----Should Recruits Make Villager Huh? sound?----" + "\n" +
                "\t" + "(takes effect after restart)" + "\n" +
                "\t" + "default: true")
                .worldRestart()
                .define("PlayVillagerAmbientSound", true);

        RenderNameTagforOwner = BUILDER.comment("\n" + "----Should Recruits Name Tag Render for the owner----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("RenderNameTagforOwner", true);

        OverrideIronGolemSpawn = BUILDER.comment("\n" + "----Should Recruits instead of Iron Golems spawn in Villages ----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("OverrideIronGolemSpawn", true);

        MaxSpawnRecruitsInVillage = BUILDER.comment("\n" +"Max Recruits in a Village Chunk to Spawn" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: 1")
                .worldRestart()
                .defineInRange("MaxSpawnRecruitsInVillage", 1, 1, 10);

        PillagerFriendlyFire = BUILDER.comment("\n" + "----Should Pillagers do Friendlyfire ----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: false")
                .worldRestart()
                .define("PillagerFriendlyFire", false);

        PillagerSpawn = BUILDER.comment("\n" + "----Should Pillagers Spawn naturally ----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("PillagerSpawn", true);

        PillagerAttackMonsters= BUILDER.comment("\n" + "----Should Pillagers attack Monsters----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("PillagerAttackMonsters", true);

        MonstersAttackPillagers= BUILDER.comment("\n" + "----Should Monsters attack Pillagers----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("MonstersAttackPillagers", true);

        ShouldPillagersRaidNaturally= BUILDER.comment("\n" + "----Should Pillagers attack all Living----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("ShouldPillagersRaidNaturally", true);

        RecruitsMaxXpForLevelUp = BUILDER.comment("\n" +"Max XP a Recruit needs to Level Up" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: 200")
                .worldRestart()
                .defineInRange("RecruitsMaxXpForLevelUp", 250, 50, 10000);


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

