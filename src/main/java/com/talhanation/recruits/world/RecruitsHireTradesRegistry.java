package com.talhanation.recruits.world;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.init.ModEntityTypes;
import net.minecraft.network.chat.Component;

import java.util.*;
public class RecruitsHireTradesRegistry {

    public RecruitsHireTrade RECRUIT = new RecruitsHireTrade(ModEntityTypes.RECRUIT.getId(), RecruitsServerConfig.RecruitCost.get(),TITLE_RECRUIT, DESCRIPTION_RECRUIT);
    public RecruitsHireTrade SHIELDMAN = new RecruitsHireTrade(ModEntityTypes.RECRUIT_SHIELDMAN.getId(), RecruitsServerConfig.ShieldmanCost.get(), TITLE_SHIELDMAN, DESCRIPTION_SHIELDMAN);
    public RecruitsHireTrade BOWMAN = new RecruitsHireTrade(ModEntityTypes.BOWMAN.getId(), RecruitsServerConfig.BowmanCost.get(), TITLE_BOWMAN, DESCRIPTION_BOWMAN);
    public RecruitsHireTrade CROSSBOWMAN = new RecruitsHireTrade(ModEntityTypes.CROSSBOWMAN.getId(), RecruitsServerConfig.CrossbowmanCost.get(), TITLE_CROSSBOWMAN, DESCRIPTION_CROSSBOWMAN);

    public RecruitsHireTrade NOMAD = new RecruitsHireTrade(ModEntityTypes.NOMAD.getId(), RecruitsServerConfig.NomadCost.get(), TITLE_NOMAD, DESCRIPTION_NOMAD);
    public RecruitsHireTrade HORSEMAN = new RecruitsHireTrade(ModEntityTypes.HORSEMAN.getId(), RecruitsServerConfig.HorsemanCost.get(), TITLE_HORSEMAN, DESCRIPTION_HORSEMAN);

    public RecruitsHireTrade MESSENGER = new RecruitsHireTrade(ModEntityTypes.MESSENGER.getId(),32, TITLE_MESSENGER, DESCRIPTION_MESSENGER);
    public RecruitsHireTrade SCOUT = new RecruitsHireTrade(ModEntityTypes.SCOUT.getId(), 32, TITLE_SCOUT, DESCRIPTION_SCOUT);

    public RecruitsHireTrade CAPTAIN = new RecruitsHireTrade(ModEntityTypes.CAPTAIN.getId(), 64, TITLE_CAPTAIN, DESCRIPTION_CAPTAIN);
    public RecruitsHireTrade PATROL_LEADER = new RecruitsHireTrade(ModEntityTypes.PATROL_LEADER.getId(), 64, TITLE_PATROL_LEADER, DESCRIPTION_PATROL_LEADER);

    private static final Map<Integer, Map<Integer, List<String>>> TRADES = new HashMap<>();
    public static void registerTrades() {
        // === Trader Type 0 ===
        addTrade(0, 1, "recruits:recruit", "shieldman");
        addTrade(0, 2, "horseman");
        addTrade(0, 3, "captain", "commander");

        // === Trader Type 1 ===
        addTrade(1, 1, "recruit", "bowman");
        addTrade(1, 2, "nomad");
        addTrade(1, 3, "messenger", "scout");

        // === Trader Type 2 ===
        addTrade(2, 1, "recruit", "crossbowman");
        addTrade(2, 2, "shieldman");
        addTrade(2, 3, "commander", "scout");

        // === Trader Type 3 (Workers Addon) ===
        addTrade(3, 1, "miner", "lumberjack");
        addTrade(3, 2, "builder");
        addTrade(3, 3, "recruit");

        // === Trader Type 4 (Workers Addon) ===
        addTrade(4, 1, "farmer", "lumberjack");
        addTrade(4, 2, "miner");
        addTrade(4, 3, "recruit");
    }

    public static void addTrade(int traderType, int level, String... units) {
        TRADES.computeIfAbsent(traderType, k -> new HashMap<>())
                .put(level, Arrays.asList(units));
    }

    /**
     * Gibt die Liste der möglichen Trades (z. B. Unit-Namen) für einen Trader-Typ und Level zurück.
     */
    public static List<String> getTrades(int traderType, int level) {
        return TRADES.getOrDefault(traderType, Collections.emptyMap())
                .getOrDefault(level, Collections.emptyList());
    }

    /**
     * Optional: Liefert zufälligen Trade aus der Palette.
     */
    public static Optional<String> getRandomTrade(int traderType, int level, Random random) {
        List<String> list = getTrades(traderType, level);
        if (list.isEmpty()) return Optional.empty();
        return Optional.of(list.get(random.nextInt(list.size())));
    }


    private static final Component TITLE_RECRUIT = Component.translatable("description.recruits.title.recruit");
    private static final Component TITLE_SHIELDMAN = Component.translatable("description.recruits.title.shieldman");
    private static final Component TITLE_BOWMAN = Component.translatable("description.recruits.title.bowman");
    private static final Component TITLE_CROSSBOWMAN = Component.translatable("description.recruits.title.crossbowman");
    private static final Component TITLE_NOMAD = Component.translatable("description.recruits.title.nomad");
    private static final Component TITLE_HORSEMAN = Component.translatable("description.recruits.title.horseman");
    private static final Component TITLE_MESSENGER = Component.translatable("description.recruits.title.messenger");
    private static final Component TITLE_SCOUT = Component.translatable("description.recruits.title.scout");
    private static final Component TITLE_CAPTAIN = Component.translatable("description.recruits.title.captain");
    private static final Component TITLE_PATROL_LEADER = Component.translatable("description.recruits.title.commander");

    private static final Component DESCRIPTION_RECRUIT = Component.translatable("description.recruits.recruit");
    private static final Component DESCRIPTION_SHIELDMAN = Component.translatable("description.recruits.shieldman");
    private static final Component DESCRIPTION_BOWMAN = Component.translatable("description.recruits.bowman");
    private static final Component DESCRIPTION_CROSSBOWMAN = Component.translatable("description.recruits.crossbowman");
    private static final Component DESCRIPTION_NOMAD = Component.translatable("description.recruits.nomad");
    private static final Component DESCRIPTION_HORSEMAN = Component.translatable("description.recruits.horseman");
    private static final Component DESCRIPTION_MESSENGER = Component.translatable("description.recruits.messenger");
    private static final Component DESCRIPTION_SCOUT = Component.translatable("description.recruits.scout");
    private static final Component DESCRIPTION_CAPTAIN = Component.translatable("description.recruits.captain");
    private static final Component DESCRIPTION_PATROL_LEADER = Component.translatable("description.recruits.commander");

}

