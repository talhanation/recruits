package com.talhanation.recruits.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.talhanation.recruits.world.RecruitsTeamSavedData;
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
    public static final int NEW_VERSION = 26;
    public static ForgeConfigSpec.BooleanValue PlayVillagerAmbientSound;
    public static ForgeConfigSpec.BooleanValue RecruitTablesPOIReleasing;
    public static ForgeConfigSpec.BooleanValue OverrideIronGolemSpawn;
    public static ForgeConfigSpec.BooleanValue PillagerFriendlyFire;
    public static ForgeConfigSpec.BooleanValue PillagerSpawn;
    public static ForgeConfigSpec.BooleanValue PillagerAttackMonsters;
    public static ForgeConfigSpec.BooleanValue ShouldPillagersRaidNaturally;
    public static ForgeConfigSpec.BooleanValue MonstersAttackPillagers;
    public static ForgeConfigSpec.IntValue MaxSpawnRecruitsInVillage;
    public static ForgeConfigSpec.IntValue MaxRecruitsForPlayer;
    public static ForgeConfigSpec.IntValue RecruitsMaxXpForLevelUp;
    public static ForgeConfigSpec.IntValue RecruitsMaxXpLevel;
    public static ForgeConfigSpec.BooleanValue PillagerIncreasedCombatRange;
    public static ForgeConfigSpec.BooleanValue VindicatorSpawnItems;
    public static ForgeConfigSpec.BooleanValue DisableVanillaTeamCommands;
    public static ForgeConfigSpec.BooleanValue PillagerSpawnItems;
    public static ForgeConfigSpec.IntValue MaxAssassinCount;
    public static ForgeConfigSpec.IntValue RecruitCost;
    public static ForgeConfigSpec.IntValue BowmanCost;
    public static ForgeConfigSpec.IntValue ShieldmanCost;
    public static ForgeConfigSpec.IntValue NomadCost;
    public static ForgeConfigSpec.IntValue HorsemanCost;
    public static ForgeConfigSpec.IntValue CrossbowmanCost;
    public static ForgeConfigSpec.DoubleValue RecruitFollowStartDistance;
    public static ForgeConfigSpec.ConfigValue<List<String>> TargetBlackList;
    public static ForgeConfigSpec.ConfigValue<List<String>> MountWhiteList;
    public static ForgeConfigSpec.ConfigValue<List<String>> StartArmorList;
    public static ForgeConfigSpec.ConfigValue<List<String>> RecruitHandEquipment;
    public static ForgeConfigSpec.ConfigValue<List<String>> ShieldmanHandEquipment;
    public static ForgeConfigSpec.ConfigValue<List<String>> BowmanHandEquipment;
    public static ForgeConfigSpec.ConfigValue<List<String>> CrossbowmanHandEquipment;
    public static ForgeConfigSpec.ConfigValue<List<String>> HorsemanHandEquipment;
    public static ForgeConfigSpec.ConfigValue<List<String>> NomadHandEquipment;
    public static ForgeConfigSpec.ConfigValue<List<String>> AcceptedDamagesourceImmunity;
    public static ForgeConfigSpec.BooleanValue AggroRecruitsBlockEvents;
    public static ForgeConfigSpec.BooleanValue NeutralRecruitsBlockEvents;
    public static ForgeConfigSpec.BooleanValue ShouldRecruitPatrolsSpawn;
    public static ForgeConfigSpec.BooleanValue ShouldPillagerPatrolsSpawn;
    public static ForgeConfigSpec.DoubleValue RecruitPatrolsSpawnChance;
    public static ForgeConfigSpec.DoubleValue PillagerPatrolsSpawnChance;
    public static ForgeConfigSpec.ConfigValue<String> RecruitCurrency;
    public static ForgeConfigSpec.BooleanValue RecruitsLookLikeVillagers;
    public static ForgeConfigSpec.BooleanValue NoDamageImmunity;
    public static ForgeConfigSpec.IntValue PillagerPatrolSpawnInterval;
    public static ForgeConfigSpec.IntValue RecruitPatrolSpawnInterval;
    public static ForgeConfigSpec.BooleanValue GlobalTeamFriendlyFireSetting;
    public static ForgeConfigSpec.BooleanValue GlobalTeamSeeFriendlyInvisibleSetting;
    public static ForgeConfigSpec.BooleanValue GlobalTeamSetting;
    public static ForgeConfigSpec.BooleanValue CommandScreenToggle;
    public static ForgeConfigSpec.BooleanValue RecruitHorseUnitsHorse;
    public static ArrayList<String> BLACKLIST = new ArrayList<>(
            Arrays.asList("minecraft:creeper", "minecraft:ghast"));
    public static ArrayList<String> MOUNTS = new ArrayList<>(
            Arrays.asList("minecraft:horse", "minecraft:llama", "minecraft:pig", "minecraft:boat", "minecraft:minecart", "smallships:cog", "smallships:brigg", "smallships:galley", "camels:camel"));
    public static ArrayList<String> START_ARMOR = new ArrayList<>();
    public static ArrayList<String> RECRUIT_HAND = new ArrayList<>(
            Arrays.asList("minecraft:wooden_sword", ""));
    public static ArrayList<String> SHIELDMAN_HAND = new ArrayList<>(
            Arrays.asList("minecraft:wooden_axe", "minecraft:shield"));
    public static ArrayList<String> HORSEMAN_HAND = new ArrayList<>(
            Arrays.asList("minecraft:stone_sword", "minecraft:shield"));
    public static ArrayList<String> BOWMAN_HAND = new ArrayList<>(
            Arrays.asList("minecraft:bow", ""));
    public static ArrayList<String> NOMAD_HAND = new ArrayList<>(
            Arrays.asList("minecraft:bow", ""));

    public static ArrayList<String> CROSSBOWMAN_HAND = new ArrayList<>(
            Arrays.asList("minecraft:crossbow", ""));

    public static ArrayList<String> DAMAGESOURCE = new ArrayList<>(
            Arrays.asList("inFire", "lava", "sweetBerryBush", "cactus", "lightningBolt", "inWall", "hotFloor", "outOfWorld", "drown"));//add drowning


    public static ArrayList<String> list = new ArrayList<>();

    static{
        VERSION = BUILDER.comment("\n" +"##Version, do not change!##")
                .defineInRange("Version", 0, 0, Integer.MAX_VALUE);

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

        /*
        Recruits Config
         */
        BUILDER.pop();
        BUILDER.comment("Recruits Config:").push("Recruits");

        RecruitCurrency = BUILDER.comment("""

                        ----Currency----
                        \t(takes effect after restart)
                        \tThe Item defined here, will be used to hire recruits. For example: ["minecraft:diamond"]\tdefault: ["minecraft:emerald"]""")
                .worldRestart()
                .define("RecruitCurrency", "minecraft:emerald");

        RecruitsMaxXpForLevelUp = BUILDER.comment("""

                        ----Max XP a Recruit needs to Level Up.----
                        \t(takes effect after restart)
                        \tdefault: 250""")
                .worldRestart()
                .defineInRange("RecruitsMaxXpForLevelUp", 250, 50, 10000);

        RecruitsMaxXpLevel = BUILDER.comment("""
                        ----The max. Level a recruit can get.-----
                        \t(takes effect after restart)
                        \tdefault: 20""")
                .worldRestart()
                .defineInRange("RecruitsMaxXpLevel", 20, 10, 9999);

        RecruitFollowStartDistance = BUILDER.comment("""

                        ----Distance Recruits will start to follow its owner----
                        \t(takes effect after restart)
                        \tdefault: 20.0""")
                .worldRestart()
                .defineInRange("RecruitFollowStartDistance", 20.0, 0, 100.0);

        MaxRecruitsForPlayer = BUILDER.comment("""
                        ----Max amount a player can recruit----
                        \t(takes effect after restart)
                        \tdefault: 64""")
                .worldRestart()
                .defineInRange("MaxRecruitsForPlayer", 64, 1, 1280);

        TargetBlackList = BUILDER.comment("""
                        ----Target Blacklist----
                        \t(takes effect after restart)
                        \tEntities in this list won't be targeted at all, for example: ["minecraft:creeper", "minecraft:sheep"]""")
                .worldRestart()
                .define("TargetBlackList", BLACKLIST);

        MountWhiteList = BUILDER.comment("""
                        ----Mount Whitelist----
                        \t(takes effect after restart)
                        \tONLY Entities in this list can be mounted by a recruit, for example: ["minecraft:boat", "smallships:cog"]""")
                .worldRestart()
                .define("MountWhitelist", MOUNTS);

        RecruitCost = BUILDER.comment("""

                        The amount of currency required to hire a recruit.
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("RecruitCost", 4, 0, 999);

        BowmanCost = BUILDER.comment("""

                        The amount of currency required to hire a bowman.
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("BowmanCost", 6, 0, 999);

        CrossbowmanCost = BUILDER.comment("""

                        The amount of currency required to hire a crossbowman.
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("CrossbowmanCost", 8, 0, 999);

        ShieldmanCost = BUILDER.comment("""

                        The amount of currency required to hire a shieldman.
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("ShieldmanCost", 10, 0, 999);

        HorsemanCost = BUILDER.comment("""

                        The amount of currency required to hire a horseman.
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("HorsemanCost", 20, 0, 999);

        NomadCost = BUILDER.comment("""

                        The amount of currency required to hire a nomad.
                        \t(takes effect after restart)
                        \tdefault: 15""")
                .worldRestart()
                .defineInRange("NomadCost", 19, 0, 999);

        RecruitHorseUnitsHorse = BUILDER.comment("""
                        ----RecruitHorseUnitsHorse----
                        \t(takes effect after restart)
                        \t
                        Should the Horse units spawn with a horse?""
                        default: true""")

                .worldRestart()
                .define("RecruitHorseUnitsHorse", true);

        /*
        Village Config
         */
        BUILDER.pop();
        BUILDER.comment("Recruit Village Config:").push("Villages");

        RecruitTablesPOIReleasing = BUILDER.comment("""

                        ----Should Villager Recruits that were created with Tables release the POI for other Villagers?----
                        ----True -> allows multiple villagers to become a recruit with one table.----
                        ----False -> only one villager can become a recruit with one table.----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("RecruitTablesPOIReleasing", true);

        OverrideIronGolemSpawn = BUILDER.comment("""

                        ----Should Recruits instead of Iron Golems spawn in Villages ----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("OverrideIronGolemSpawn", true);

        MaxSpawnRecruitsInVillage = BUILDER.comment("""

                        Max Recruits in a Village Chunk to Spawn
                        \t(takes effect after restart)
                        \tdefault: 1""")
                .worldRestart()
                .defineInRange("MaxSpawnRecruitsInVillage", 1, 1, 10);

         /*
        Equipment Config
         */
        BUILDER.pop();
        BUILDER.comment("Recruit Equipment Config:").push("Equipment");

        StartArmorList = BUILDER.comment("""

                        ----Start armor ----
                        \t(takes effect after restart)
                        \tItems in this list will be equipped to a new spawned recruits in this order: ["head", "chest", "legs", "feet"]
                        \tFor example: ["minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots"]""")
                .worldRestart()
                .define("StartArmorList", START_ARMOR);

        RecruitHandEquipment = BUILDER.comment("""

                        ----Recruit start hand equipment ----
                        \t(takes effect after restart)
                        \tItems in this list will be equipped to a new spawned recruit, in this following order: ["main-hand", "off-hand"]""")
                .worldRestart()
                .define("RecruitStartHandEquipment", RECRUIT_HAND);

        ShieldmanHandEquipment = BUILDER.comment("""

                        ----Shieldman start hand equipment ----
                        \t(takes effect after restart)
                        \tItems in this list will be equipped to a new spawned shieldman, in this following order: ["main-hand", "off-hand" ]""")
                .worldRestart()
                .define("ShieldmanStartHandEquipment", SHIELDMAN_HAND);

        BowmanHandEquipment = BUILDER.comment("""

                        ----Bowman start hand equipment ----
                        \t(takes effect after restart)
                        \tItems in this list will be equipped to a new spawned bowman, in this following order: ["main-hand", "off-hand" ]""")
                .worldRestart()
                .define("BowmanStartHandEquipment", BOWMAN_HAND);

        CrossbowmanHandEquipment = BUILDER.comment("""

                        ----Crossbowman start hand equipment ----
                        \t(takes effect after restart)
                        \tItems in this list will be equipped to a new spawned crossbowman, in this following order: ["main-hand", "off-hand" ]""")
                .worldRestart()
                .define("CrossbowmanStartHandEquipment", CROSSBOWMAN_HAND);

        HorsemanHandEquipment = BUILDER.comment("""

                        ----Horseman start hand equipment ----
                        \t(takes effect after restart)
                        \tItems in this list will be equipped to a new spawned shieldman, in this following order: ["main-hand", "off-hand" ]""")
                .worldRestart()
                .define("HorsemanHandEquipment", HORSEMAN_HAND);

        NomadHandEquipment = BUILDER.comment("""

                        ----Nomad start hand equipment ----
                        \t(takes effect after restart)
                        \tItems in this list will be equipped to a new spawned nomad, in this following order: ["main-hand", "off-hand" ]""")
                .worldRestart()
                .define("NomadHandEquipment", NOMAD_HAND);

        /*
        Pillager Config
         */

        BUILDER.pop();
        BUILDER.comment("Monster and Pillager Config:").push("Hostiles");

        PillagerFriendlyFire = BUILDER.comment("""

                        ----Should Pillagers do friendly fire ----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("PillagerFriendlyFire", true);

        PillagerSpawn = BUILDER.comment("""

                        ----Should Pillagers spawn naturally ----
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("PillagerSpawn", false);

        PillagerAttackMonsters= BUILDER.comment("""

                        ----Should Pillagers attack Monsters----
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("PillagerAttackMonsters", false);

        MonstersAttackPillagers= BUILDER.comment("""

                        ----Should Monsters attack Pillagers----
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("MonstersAttackPillagers", false);

        ShouldPillagersRaidNaturally= BUILDER.comment("""

                        ----Should Pillagers attack all Living----
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("ShouldPillagersRaidNaturally", false);

        PillagerIncreasedCombatRange= BUILDER.comment("""

                        ----Should Pillagers have increased Combat Range, so they can shoot from far away----
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("PillagerIncreasedCombatRange", false);

        VindicatorSpawnItems= BUILDER.comment("""

                        ----Should Vindicators can spawn with shield and sword and AI to use these----
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("VindicatorSpawnItems", false);

        PillagerSpawnItems= BUILDER.comment("""

                        ----Should Pillagers can spawn with shield and sword and AI to use these----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("PillagerSpawnItems", false);

        /*
        Block Event Config
         */

        BUILDER.pop();
        BUILDER.comment("Block Event Config:").push("BlockEvents");

        AggroRecruitsBlockEvents= BUILDER.comment("""

                        ----Should Aggressive Recruits attack enemy players that are placing, interacting or breaking blocks immediately?----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("AggroRecruitsBlockEvents", true);

        NeutralRecruitsBlockEvents= BUILDER.comment("""

                        ----Should Neutral Recruits attack enemy players that are placing, interacting or breaking blocks immediately?----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("NeutralRecruitsBlockEvents", true);

        /*
        Patrol Config
         */

        BUILDER.pop();
        BUILDER.comment("Recruit Patrols Config:").push("Patrols");

        ShouldRecruitPatrolsSpawn= BUILDER.comment("""

                        ----Should Recruits spawn as Patrols in the world?----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("ShouldRecruitPatrolsSpawn", true);

        RecruitPatrolsSpawnChance= BUILDER.comment("""

                        ----Chance that a Recruit Patrol can spawn. (higher values = higher chance to spawn)----
                        \t(takes effect after restart)
                        \tdefault: 15.0""")
                .worldRestart()
                .defineInRange("RecruitPatrolsSpawnChance", 15.0D, 0.0D, 100.0D);

        RecruitPatrolSpawnInterval = BUILDER.comment("""

                        The interval in minutes a Recruit Patrol and Caravan can spawn.
                        \t(takes effect after restart)
                        \tdefault: 30""")
                .worldRestart()
                .defineInRange("RecruitPatrolSpawnInterval", 30, 1, 60);

        ShouldPillagerPatrolsSpawn = BUILDER.comment("""

                        ----Should modded Pillager Patrols spawn in the world?----
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("ShouldPillagerPatrolsSpawn", false);

        PillagerPatrolsSpawnChance = BUILDER.comment("""

                        ----Chance that a modded Pillager Patrol can spawn. (higher values = higher chance to spawn)----
                        \t(takes effect after restart)
                        \tdefault: 25.0""")
                .worldRestart()
                .defineInRange("PillagerPatrolsSpawnChance", 25.0D, 0.0D, 100.0D);

        PillagerPatrolSpawnInterval = BUILDER.comment("""

                        The interval in minutes a Pillager Patrol can spawn.
                        \t(takes effect after restart)
                        \tdefault: 45""")
                .worldRestart()
                .defineInRange("PillagerPatrolSpawnInterval", 45, 1, 60);


        /*
        MaxAssassinCount = BUILDER.comment("\n" +"WIP: Max Assassins to buy from the Assassin Leader" + "\n" +
                        "\t" + "(takes effect after restart)" + "\n" +
                        "\t" + "default: 16")
                .worldRestart()
                .defineInRange("MaxAssassinCount", 16, 1, 64);

         */

        /*
        Equipment Config
         */

        BUILDER.pop();
        BUILDER.comment("General Damage Config:").push("Damage");

        NoDamageImmunity = BUILDER.comment("""
                        ----No damage Immunity----
                        \tShould Immunity between hits be disabled?
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("NoDamageImmunity", false);

        AcceptedDamagesourceImmunity = BUILDER.comment("""
                        ----List of damagesource that accept immunity ----
                        \t(takes effect after restart)
                        \tDamagesource in this list will apply a immunity of 0.5s to the entity like normal.""")
                .worldRestart()
                .define("AcceptedDamagesourceImmunity", DAMAGESOURCE);
       /*
        Teams Config
        */

        BUILDER.pop();
        BUILDER.comment("Recruit Teams Config:").push("Teams");

        DisableVanillaTeamCommands = BUILDER.comment("""
                        ----Should specific vanilla team commands be disabled?----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("DisableVanillaTeamCommands", true);

        BUILDER.comment("Global Team Settings").push("Global Team Settings");

        GlobalTeamSetting = BUILDER.comment("""

                        ----Should Recruits override following team settings on world start for all teams?----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("GlobalTeamSetting", true);

        GlobalTeamFriendlyFireSetting = BUILDER.comment("""
                        ----Override Friendly fire true/false for all teams on world start----
                        \t(takes effect after restart)
                        \tdefault: false""")
                .worldRestart()
                .define("GlobalTeamFriendlyFireSetting", false);

        GlobalTeamSeeFriendlyInvisibleSetting = BUILDER.comment("""
                        ----Override SeeFriendlyInvisible true/false for all teams on world start----
                        \t(takes effect after restart)
                        \tdefault: true""")
                .worldRestart()
                .define("GlobalTeamSeeFriendlyInvisibleSetting", true);

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

