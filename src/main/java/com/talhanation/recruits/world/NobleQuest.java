package com.talhanation.recruits.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Ein Tagesangebot eines Dorfes. Drei Arten: Lieferung, Monsterjagd, Kopfgeld.
 */
public class NobleQuest {

    public enum Type {
        DELIVERY,
        MONSTER_HUNT,
        BOUNTY;

        public static Type byId(int id) {
            Type[] values = values();
            return id >= 0 && id < values.length ? values[id] : DELIVERY;
        }
    }

    public UUID id;
    public Type type;
    /** Lieferung: Item-ID oder "#tag". Monsterjagd: EntityType-ID. Kopfgeld: UUID des Ziels. */
    public String target;
    /** Kopfgeld: Name des Ziels. Sonst leer. */
    public String targetName = "";
    /** Kopfgeld auf einen Fraktionsleader: stringID der Fraktion. Sonst leer. */
    public String targetFaction = "";
    public int count;
    public int rewardReputation;
    /** Spieler, die dieses Angebot heute schon angenommen haben. Nur serverseitig. */
    public final Set<UUID> takenBy = new HashSet<>();

    public NobleQuest(Type type, String target, int count, int rewardReputation) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.target = target;
        this.count = count;
        this.rewardReputation = rewardReputation;
    }

    private NobleQuest() {
    }

    public NobleQuest copy() {
        NobleQuest quest = new NobleQuest();
        quest.id = this.id;
        quest.type = this.type;
        quest.target = this.target;
        quest.targetName = this.targetName;
        quest.targetFaction = this.targetFaction;
        quest.count = this.count;
        quest.rewardReputation = this.rewardReputation;
        return quest;
    }

    public boolean isFactionBounty() {
        return type == Type.BOUNTY && targetFaction != null && !targetFaction.isEmpty();
    }

    public UUID getTargetUUID() {
        try {
            return UUID.fromString(target);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /* ===================== Lieferung ===================== */

    public boolean matchesDeliveryItem(ItemStack stack) {
        if (stack.isEmpty() || target == null) return false;
        if (target.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(target.substring(1));
            if (tagId == null) return false;
            return stack.is(TagKey.create(Registries.ITEM, tagId));
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && key.toString().equals(target);
    }

    /** Zählt passende Items im Hauptinventar (gleiche Menge prüft Client und Server). */
    public int countDeliveryItems(Player player) {
        int amount = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matchesDeliveryItem(stack)) amount += stack.getCount();
        }
        return amount;
    }

    /** Nimmt {@link #count} passende Items aus dem Hauptinventar. Vorher mit {@link #countDeliveryItems} prüfen. */
    public void removeDeliveryItems(Player player) {
        int toRemove = count;
        for (ItemStack stack : player.getInventory().items) {
            if (toRemove <= 0) break;
            if (!matchesDeliveryItem(stack)) continue;
            int take = Math.min(toRemove, stack.getCount());
            stack.shrink(take);
            toRemove -= take;
        }
        player.getInventory().setChanged();
    }

    /* ===================== Anzeige ===================== */

    public Component getTargetDisplayName() {
        switch (type) {
            case DELIVERY -> {
                if (target.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(target.substring(1));
                    String path = tagId != null ? tagId.getPath() : target;
                    return Component.translatable("gui.recruits.noble.quest.tag." + path);
                }
                ResourceLocation id = ResourceLocation.tryParse(target);
                Item item = id != null ? ForgeRegistries.ITEMS.getValue(id) : null;
                return item != null ? item.getDescription() : Component.literal(target);
            }
            case MONSTER_HUNT -> {
                return EntityType.byString(target)
                        .map(EntityType::getDescription)
                        .orElse(Component.literal(target));
            }
            default -> {
                return Component.literal(targetName == null || targetName.isEmpty() ? "?" : targetName);
            }
        }
    }

    public Component getDescription() {
        return switch (type) {
            case DELIVERY -> Component.translatable("gui.recruits.noble.quest.delivery", count, getTargetDisplayName());
            case MONSTER_HUNT -> Component.translatable("gui.recruits.noble.quest.monster_hunt", count, getTargetDisplayName());
            case BOUNTY -> isFactionBounty()
                    ? Component.translatable("gui.recruits.noble.quest.bounty_leader", getTargetDisplayName())
                    : Component.translatable("gui.recruits.noble.quest.bounty", getTargetDisplayName());
        };
    }

    /* ===================== NBT ===================== */

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putInt("type", type.ordinal());
        tag.putString("target", target == null ? "" : target);
        tag.putString("targetName", targetName == null ? "" : targetName);
        tag.putString("targetFaction", targetFaction == null ? "" : targetFaction);
        tag.putInt("count", count);
        tag.putInt("reward", rewardReputation);

        ListTag taken = new ListTag();
        for (UUID uuid : takenBy) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", uuid);
            taken.add(entry);
        }
        tag.put("takenBy", taken);
        return tag;
    }

    public static NobleQuest fromNBT(CompoundTag tag) {
        NobleQuest quest = new NobleQuest();
        quest.id = tag.hasUUID("id") ? tag.getUUID("id") : UUID.randomUUID();
        quest.type = Type.byId(tag.getInt("type"));
        quest.target = tag.getString("target");
        quest.targetName = tag.getString("targetName");
        quest.targetFaction = tag.getString("targetFaction");
        quest.count = tag.getInt("count");
        quest.rewardReputation = tag.getInt("reward");

        ListTag taken = tag.getList("takenBy", Tag.TAG_COMPOUND);
        for (int i = 0; i < taken.size(); i++) {
            CompoundTag entry = taken.getCompound(i);
            if (entry.hasUUID("uuid")) quest.takenBy.add(entry.getUUID("uuid"));
        }
        return quest;
    }

    /* ===================== Netzwerk (ohne takenBy) ===================== */

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeByte(type.ordinal());
        buf.writeUtf(target == null ? "" : target);
        buf.writeUtf(targetName == null ? "" : targetName);
        buf.writeUtf(targetFaction == null ? "" : targetFaction);
        buf.writeVarInt(count);
        buf.writeVarInt(rewardReputation);
    }

    public static NobleQuest fromBytes(FriendlyByteBuf buf) {
        NobleQuest quest = new NobleQuest();
        quest.id = buf.readUUID();
        quest.type = Type.byId(buf.readByte());
        quest.target = buf.readUtf();
        quest.targetName = buf.readUtf();
        quest.targetFaction = buf.readUtf();
        quest.count = buf.readVarInt();
        quest.rewardReputation = buf.readVarInt();
        return quest;
    }
}
