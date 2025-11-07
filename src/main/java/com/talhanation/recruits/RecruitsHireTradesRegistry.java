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
