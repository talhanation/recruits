package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RecruitsHireTrade {
    public int cost;
    public int minLevel;
    public int chance;
    public int uses;
    public ResourceLocation recruitType;
    public Component title;
    public Component description;
    public RecruitsHireTrade(ResourceLocation recruitType, int cost, int minLevel, int chance) {
        this(recruitType, cost, minLevel, chance,  Component.empty(),  Component.empty());
    }
    public RecruitsHireTrade(ResourceLocation recruitType, int cost, int minLevel, int chance, Component title, Component description){
        this.recruitType = recruitType;
        this.cost = cost;
        this.minLevel = minLevel;
        this.chance = Mth.clamp(chance, 1, 100);
        this.uses = 1;
        this.title = title;
        this.description = description;
    }


    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("cost", cost);
        tag.putString("recruitType", recruitType.toString());
        tag.putInt("minLevel", minLevel);
        tag.putInt("chance", chance);
        tag.putInt("uses", uses);
        return tag;
    }

    public static RecruitsHireTrade fromNbt(CompoundTag tag) {
        int cost = tag.getInt("cost");
        int minLevel = tag.getInt("minLevel");
        int chance = tag.getInt("chance");
        ResourceLocation recruitType = new ResourceLocation(tag.getString("recruitType"));
        int uses = tag.getInt("uses");

        RecruitsHireTrade trade = new RecruitsHireTrade(recruitType, cost, minLevel, chance);
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
}
