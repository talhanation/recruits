package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class RecruitsHireTrade {
    public int cost;
    public int minLevel;
    public int chance;
    public int maxUses;
    public int uses;
    public ResourceLocation resourceLocation;
    public Component title;
    public Component description;
    public List<RecruitsTradeTag> tradeTagList;
    public RecruitsHireTrade(ResourceLocation resourceLocation, int cost, int minLevel, int chance, List<RecruitsTradeTag> tradeTagList) {
        this(resourceLocation, cost, minLevel, chance,  Component.empty(),  Component.empty(), tradeTagList);
    }
    public RecruitsHireTrade(ResourceLocation resourceLocation, int cost, int minLevel, int chance, Component title, Component description, List<RecruitsTradeTag> tradeTagList){
        this.resourceLocation = resourceLocation;
        this.cost = cost;
        this.minLevel = minLevel;
        this.chance = Mth.clamp(chance, 1, 100);
        this.maxUses = 1;
        this.uses = maxUses;
        this.title = title;
        this.description = description;
        this.tradeTagList = tradeTagList;
    }


    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("cost", cost);
        tag.putString("recruitType", resourceLocation.toString());
        tag.putInt("minLevel", minLevel);
        tag.putInt("chance", chance);
        tag.putInt("maxUses", maxUses);
        tag.putInt("uses", uses);

        ListTag tagList = new ListTag();
        if (tradeTagList != null && !tradeTagList.isEmpty()) {
            for (RecruitsTradeTag t : tradeTagList) {
                tagList.add(StringTag.valueOf(t.name()));
            }
        }
        tag.put("tradeTagList", tagList);

        return tag;
    }

    public static RecruitsHireTrade fromNbt(CompoundTag tag) {
        int cost = tag.getInt("cost");
        int minLevel = tag.getInt("minLevel");
        int chance = tag.getInt("chance");
        ResourceLocation recruitType = new ResourceLocation(tag.getString("recruitType"));
        int maxUses = tag.getInt("maxUses");
        int uses = tag.getInt("uses");

        List<RecruitsTradeTag> tradeTagList = new ArrayList<>();
        if (tag.contains("tradeTagList", Tag.TAG_LIST)) {
            ListTag list = tag.getList("tradeTagList", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String name = list.getString(i);
                tradeTagList.add(RecruitsTradeTag.valueOf(name));
            }
        }

        RecruitsHireTrade trade = new RecruitsHireTrade(recruitType, cost, minLevel, chance, tradeTagList);
        trade.maxUses = maxUses;
        trade.uses = uses;
        return trade;
    }
    public static CompoundTag listToNbt(List<RecruitsHireTrade> trades) {
        CompoundTag compound = new CompoundTag();
        if (trades == null) return compound;

        ListTag list = new ListTag();
        for (RecruitsHireTrade t : trades) {
            list.add(t.toNbt());
        }
        compound.put("Trades", list);
        return compound;
    }

    /** liest Liste aus CompoundTag */
    public static List<RecruitsHireTrade> listFromNbt(CompoundTag compound) {
        List<RecruitsHireTrade> out = new ArrayList<>();
        if (compound == null || !compound.contains("Trades", Tag.TAG_LIST)) {
            return out;
        }

        ListTag list = compound.getList("Trades", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            out.add(RecruitsHireTrade.fromNbt(entry));
        }
        return out;
    }

    public enum RecruitsTradeTag {
        MELEE,
        RANGED,
        CAVALRY,
        COMPANION,
        WORKER,
        FARMER,
        ANIMAL_FARMER;
        //INFANTRY_ADDON,
        //RANGED_ADDON;

        public static List<RecruitsTradeTag> getValues(){
            return List.of(values());
        }
    }
}


