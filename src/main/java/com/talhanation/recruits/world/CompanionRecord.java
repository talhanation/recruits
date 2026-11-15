package com.talhanation.recruits.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * What the server knows about one owned companion, also while his chunk is not loaded.
 * Kept up to date by {@link RecruitsCompanionManager}; other classes only read it.
 */
public class CompanionRecord {

    public final UUID uuid;
    public UUID owner;
    /** Entity type id, e.g. recruits:patrol_leader. */
    public ResourceLocation type;
    public String name;
    /** Dimension id, e.g. minecraft:overworld. */
    public ResourceLocation dimension;
    public BlockPos pos;
    /** Game time when the companion was last seen in the world. */
    public long lastSeen;
    /** Short status key (patrol state, messenger state, ...), empty if there is none. */
    public String status;
    public boolean atMission;
    /** In the world right now. Not saved: after a restart every companion counts as unloaded until it joins. */
    public transient boolean loaded;
    /**
     * His synced entity data as he was last seen, packed by {@code CompanionOrders#packSynced}. With it the owner's
     * client shows his screen while he is far away. Not sent with the companion list.
     */
    public byte[] synced = new byte[0];
    /** Orders a Messenger delivered while he was not in the world. Carried out when he is back, see {@code CompanionOrders}. */
    public ListTag pending = new ListTag();

    public CompanionRecord(UUID uuid) {
        this.uuid = uuid;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", uuid);
        tag.putUUID("owner", owner);
        tag.putString("type", type.toString());
        tag.putString("name", name);
        tag.putString("dimension", dimension.toString());
        tag.putLong("pos", pos.asLong());
        tag.putLong("lastSeen", lastSeen);
        tag.putString("status", status);
        tag.putBoolean("atMission", atMission);
        tag.putByteArray("synced", synced);
        if (!pending.isEmpty()) tag.put("pending", pending);
        return tag;
    }

    /** @return null if the tag is broken (missing uuid, owner or type) */
    public static CompanionRecord load(CompoundTag tag) {
        if (!tag.hasUUID("uuid") || !tag.hasUUID("owner")) return null;
        ResourceLocation type = ResourceLocation.tryParse(tag.getString("type"));
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
        if (type == null || dimension == null) return null;

        CompanionRecord record = new CompanionRecord(tag.getUUID("uuid"));
        record.owner = tag.getUUID("owner");
        record.type = type;
        record.name = tag.getString("name");
        record.dimension = dimension;
        record.pos = BlockPos.of(tag.getLong("pos"));
        record.lastSeen = tag.getLong("lastSeen");
        record.status = tag.getString("status");
        record.atMission = tag.getBoolean("atMission");
        record.synced = tag.getByteArray("synced");
        record.pending = tag.getList("pending", 10);
        return record;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(owner);
        buf.writeResourceLocation(type);
        buf.writeUtf(name, 256);
        buf.writeResourceLocation(dimension);
        buf.writeBlockPos(pos);
        buf.writeLong(lastSeen);
        buf.writeUtf(status, 64);
        buf.writeBoolean(atMission);
        buf.writeBoolean(loaded);
    }

    public static CompanionRecord fromBytes(FriendlyByteBuf buf) {
        CompanionRecord record = new CompanionRecord(buf.readUUID());
        record.owner = buf.readUUID();
        record.type = buf.readResourceLocation();
        record.name = buf.readUtf(256);
        record.dimension = buf.readResourceLocation();
        record.pos = buf.readBlockPos();
        record.lastSeen = buf.readLong();
        record.status = buf.readUtf(64);
        record.atMission = buf.readBoolean();
        record.loaded = buf.readBoolean();
        return record;
    }
}
