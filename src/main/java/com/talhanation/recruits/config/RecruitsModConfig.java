package com.talhanation.recruits.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber
public class RecruitsModConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec CONFIG;
    public static ForgeConfigSpec.IntValue VERSION;
    public static final int NEW_VERSION = 11;

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
    public static ForgeConfigSpec.BooleanValue PillagerIncreasedCombatRange;
    public static ForgeConfigSpec.BooleanValue VindicatorSpawnItems;
    public static ForgeConfigSpec.BooleanValue PillagerSpawnItems;
    public static ForgeConfigSpec.IntValue MaxAssassinCount;
    public static ForgeConfigSpec.DoubleValue RecruitFollowStartDistance;
    public static ForgeConfigSpec.ConfigValue<List<String>> TargetBlackList;
    public static ForgeConfigSpec.ConfigValue<List<String>> MountWhiteList;
    public static ForgeConfigSpec.BooleanValue AggroRecruitsBlockEvents;

    public static ArrayList<String> MOUNTS = new ArrayList<>(
            Arrays.asList("minecraft:horse", "minecraft:llama", "minecraft:pig", "minecraft:boat", "minecraft:minecart", "smallships:cog", "smallships:brigg", "camels:camel"));

    public static ArrayList<String> list = new ArrayList<>();

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
                        "\t" + "default: 250")
                .worldRestart()
                .defineInRange("RecruitsMaxXpForLevelUp", 250, 50, 10000);

        PillagerIncreasedCombatRange= BUILDER.comment("\n" + "----Should Pillagers have increased Combat Range, so they can shoot from far away----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("PillagerIncreasedCombatRange", true);

        VindicatorSpawnItems= BUILDER.comment("\n" + "----Should Vindicators can spawn with shield and sword and AI to use these----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("VindicatorSpawnItems", true);

        PillagerSpawnItems= BUILDER.comment("\n" + "----Should Pillagers can spawn with shield and sword and AI to use these----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("PillagerSpawnItems", true);

        MaxAssassinCount = BUILDER.comment("\n" +"WIP: Max Assassins to buy from the Assassin Leader" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: 16")
                .worldRestart()
                .defineInRange("MaxAssassinCount", 16, 1, 64);


        RecruitFollowStartDistance = BUILDER.comment("\n" +"Distance Recruits will start to follow its owner" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: 9.0")
                .worldRestart()
                .defineInRange("RecruitFollowStartDistance", 9.0, 4.0, 16.0);

        MaxRecruitsForPlayer = BUILDER.comment("\n" +"Max amount a player can recruit" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: 64")
                .worldRestart()
                .defineInRange("MaxRecruitsForPlayer", 64, 1, 1280);

        TargetBlackList = BUILDER.comment("\n" + "----Target Blacklist----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "Entities in this list won't be targeted at all, for example: [\"minecraft:creeper\", \"minecraft:sheep\"]")
                .worldRestart()
                .define("Target BlackList", new ArrayList<>());

        MountWhiteList = BUILDER.comment("\n" + "----Mount Whitelist----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "Entities in this list won't be targeted at all, for example: [\"minecraft:boat\", \"smallships:cog\"]")
                .worldRestart()
                .define("Mount Whitelist", MOUNTS);

        AggroRecruitsBlockEvents= BUILDER.comment("\n" + "----Should Aggressive Recruits attack immediately enemy players that are placing or breaking blocks?----" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: true")
                .worldRestart()
                .define("AggroRecruitsBlockEvents", true);

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

