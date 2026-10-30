package com.talhanation.recruits.util;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;

/**
 * Item stacks saved by Recruits for 1.20.1 ({id, Count, tag}) are stored in places the vanilla data fixer
 * does not know (modded entity inventories, faction save data). Without an upgrade they lose their
 * count and all data (enchantments, durability, banner patterns) when a 1.20.1 world is loaded in 1.21.1.
 */
public final class LegacyItemStackUtil {
    /** Data version of Minecraft 1.20.1 */
    private static final int LEGACY_DATA_VERSION = 3465;

    private LegacyItemStackUtil() {
    }

    public static boolean isLegacy(CompoundTag tag) {
        return tag.contains("Count", Tag.TAG_ANY_NUMERIC) && !tag.contains("count");
    }

    public static CompoundTag upgradeIfLegacy(CompoundTag tag) {
        if (tag == null || !isLegacy(tag)) return tag;
        try {
            Tag upgraded = DataFixers.getDataFixer().update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, (Tag) tag),
                    LEGACY_DATA_VERSION, SharedConstants.getCurrentVersion().getDataVersion().getVersion()).getValue();
            return upgraded instanceof CompoundTag compoundTag ? compoundTag : tag;
        } catch (Exception e) {
            return tag;
        }
    }

    public static ItemStack parse(HolderLookup.Provider provider, CompoundTag tag) {
        return ItemStack.parseOptional(provider, upgradeIfLegacy(tag));
    }
}
