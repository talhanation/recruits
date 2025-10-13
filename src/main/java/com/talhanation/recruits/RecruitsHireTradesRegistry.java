package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.world.RecruitsHireTrade;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
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
        return List.copyOf(TRADES);
    }
    @Nullable
    public static RecruitsHireTrade getByResourceLocation(ResourceLocation resourceLocation){
        for(RecruitsHireTrade trade : TRADES){
            if(trade.resourceLocation.equals(resourceLocation)){
                return trade;
            }
        }
        return null;
    }

    public static void registerBaseTrades(){
        register(new RecruitsHireTrade(ModEntityTypes.RECRUIT.getId(), RecruitsServerConfig.RecruitCost.get(),1, 70, TITLE_RECRUIT, DESCRIPTION_RECRUIT, RecruitsHireTrade.RecruitsTradeTag.getValues()));

        register(new RecruitsHireTrade(ModEntityTypes.RECRUIT_SHIELDMAN.getId(), RecruitsServerConfig.ShieldmanCost.get(),1, 50, TITLE_SHIELDMAN, DESCRIPTION_SHIELDMAN,List.of(RecruitsHireTrade.RecruitsTradeTag.MELEE)));
        register(new RecruitsHireTrade(ModEntityTypes.BOWMAN.getId(), RecruitsServerConfig.BowmanCost.get(),1, 50, TITLE_BOWMAN, DESCRIPTION_BOWMAN, List.of(RecruitsHireTrade.RecruitsTradeTag.RANGED)));
        register(new RecruitsHireTrade(ModEntityTypes.CROSSBOWMAN.getId(), RecruitsServerConfig.CrossbowmanCost.get(),1, 50, TITLE_CROSSBOWMAN, DESCRIPTION_CROSSBOWMAN, List.of(RecruitsHireTrade.RecruitsTradeTag.RANGED)));

        register(new RecruitsHireTrade(ModEntityTypes.NOMAD.getId(), RecruitsServerConfig.NomadCost.get(),2, 50, TITLE_NOMAD, DESCRIPTION_NOMAD, List.of(RecruitsHireTrade.RecruitsTradeTag.RANGED, RecruitsHireTrade.RecruitsTradeTag.CAVALRY)));
        register(new RecruitsHireTrade(ModEntityTypes.HORSEMAN.getId(), RecruitsServerConfig.HorsemanCost.get(),2, 50, TITLE_HORSEMAN, DESCRIPTION_HORSEMAN, List.of(RecruitsHireTrade.RecruitsTradeTag.MELEE, RecruitsHireTrade.RecruitsTradeTag.CAVALRY)));

        //register(new RecruitsHireTrade(ModEntityTypes.MESSENGER.getId(),32,4, 50, TITLE_MESSENGER, DESCRIPTION_MESSENGER, List.of(RecruitsHireTrade.RecruitsTradeTag.MELEE, RecruitsHireTrade.RecruitsTradeTag.RANGED)));
        //register(new RecruitsHireTrade(ModEntityTypes.SCOUT.getId(), 32,4, 50, TITLE_SCOUT, DESCRIPTION_SCOUT, List.of(RecruitsHireTrade.RecruitsTradeTag.MELEE, RecruitsHireTrade.RecruitsTradeTag.RANGED)));

        //register(new RecruitsHireTrade(ModEntityTypes.CAPTAIN.getId(), 64,4, 50, TITLE_CAPTAIN, DESCRIPTION_CAPTAIN, List.of(RecruitsHireTrade.RecruitsTradeTag.MELEE, RecruitsHireTrade.RecruitsTradeTag.RANGED)));
        //register(new RecruitsHireTrade(ModEntityTypes.PATROL_LEADER.getId(), 64,4, 50, TITLE_PATROL_LEADER, DESCRIPTION_PATROL_LEADER, List.of(RecruitsHireTrade.RecruitsTradeTag.MELEE, RecruitsHireTrade.RecruitsTradeTag.RANGED)));
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
