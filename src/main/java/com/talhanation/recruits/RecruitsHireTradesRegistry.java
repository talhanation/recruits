package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.world.RecruitsHireTrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RecruitsHireTradesRegistry {
    private static final List<RecruitsHireTrade> TRADES = Collections.synchronizedList(new ArrayList<>());

    public static void register(RecruitsHireTrade trade) {
        Objects.requireNonNull(trade, "trade");
        TRADES.add(trade);
    }

    public static List<RecruitsHireTrade> getAll() {
        synchronized (TRADES) {
            return List.copyOf(TRADES);
        }
    }

    public static void registerBaseTrades(){
        register(new RecruitsHireTrade(ModEntityTypes.RECRUIT.getId(), RecruitsServerConfig.RecruitCost.get(),1, 60));
        register(new RecruitsHireTrade(ModEntityTypes.RECRUIT_SHIELDMAN.getId(), RecruitsServerConfig.ShieldmanCost.get(),1, 50));
        register(new RecruitsHireTrade(ModEntityTypes.BOWMAN.getId(), RecruitsServerConfig.BowmanCost.get(),1, 50));
        register(new RecruitsHireTrade(ModEntityTypes.CROSSBOWMAN.getId(), RecruitsServerConfig.CrossbowmanCost.get(),1, 50));
        register(new RecruitsHireTrade(ModEntityTypes.NOMAD.getId(), RecruitsServerConfig.NomadCost.get(),2, 50));
        register(new RecruitsHireTrade(ModEntityTypes.HORSEMAN.getId(), RecruitsServerConfig.HorsemanCost.get(),2, 50));
        register(new RecruitsHireTrade(ModEntityTypes.MESSENGER.getId(),32,3, 50));
        register(new RecruitsHireTrade(ModEntityTypes.SCOUT.getId(), 32,4, 50));
        register(new RecruitsHireTrade(ModEntityTypes.CAPTAIN.getId(), 64,5, 50));
        register(new RecruitsHireTrade(ModEntityTypes.PATROL_LEADER.getId(), 64,5, 50));
    }
}
