package com.talhanation.recruits.governor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * One position of a claim that the Governor manages.
 * Melee and Ranged posts are held by exactly one recruit, Reserve and Patrol points by any number,
 * Treasury and Upkeep are container blocks; an Upkeep post can also be an entity with an inventory
 * (e.g. a storage area of Workers), then {@link #getEntity()} is set and the position is where it stands.
 */
public class GovernorPost {

    public enum Type {
        MELEE(0, "melee"),
        RANGED(1, "ranged"),
        RESERVE(2, "reserve"),
        PATROL(3, "patrol"),
        TREASURY(4, "treasury"),
        UPKEEP(5, "upkeep");

        private final int index;
        private final String key;

        Type(int index, String key) {
            this.index = index;
            this.key = key;
        }

        public int getIndex() {
            return index;
        }

        public String getKey() {
            return key;
        }

        /** Posts that are held by exactly one recruit. */
        public boolean isGuardPost() {
            return this == MELEE || this == RANGED;
        }

        public boolean isContainer() {
            return this == TREASURY || this == UPKEEP;
        }

        public Component getDisplayName() {
            return Component.translatable("gui.recruits.governor.post." + key);
        }

        public static Type fromIndex(int index) {
            for (Type type : values()) {
                if (type.index == index) return type;
            }
            return RESERVE;
        }
    }

    private final UUID id;
    private final Type type;
    private final BlockPos pos;
    private final int number;
    @Nullable
    private UUID assigned;
    @Nullable
    private final UUID entity;

    public GovernorPost(Type type, BlockPos pos, int number) {
        this(UUID.randomUUID(), type, pos, number, null, null);
    }

    public GovernorPost(Type type, BlockPos pos, int number, @Nullable UUID entity) {
        this(UUID.randomUUID(), type, pos, number, null, entity);
    }

    private GovernorPost(UUID id, Type type, BlockPos pos, int number, @Nullable UUID assigned, @Nullable UUID entity) {
        this.id = id;
        this.type = type;
        this.pos = pos.immutable();
        this.number = number;
        this.assigned = assigned;
        this.entity = entity;
    }

    /** The entity whose inventory is the upkeep (a storage area), null for a container block. */
    @Nullable
    public UUID getEntity() {
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public BlockPos getPos() {
        return pos;
    }

    public Vec3 getStandPos() {
        return Vec3.atBottomCenterOf(pos);
    }

    public int getNumber() {
        return number;
    }

    @Nullable
    public UUID getAssigned() {
        return assigned;
    }

    public void setAssigned(@Nullable UUID assigned) {
        this.assigned = assigned;
    }

    public boolean isFree() {
        return type.isGuardPost() && assigned == null;
    }

    public Component getLabel() {
        Component name = type.getDisplayName();
        if (type == Type.TREASURY) return name;
        return Component.literal(name.getString() + " " + number);
    }

    public String getCoordinateString() {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putInt("type", type.getIndex());
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putInt("number", number);
        if (assigned != null) tag.putUUID("assigned", assigned);
        if (entity != null) tag.putUUID("entity", entity);
        return tag;
    }

    public static GovernorPost fromNBT(CompoundTag tag) {
        UUID id = tag.hasUUID("id") ? tag.getUUID("id") : UUID.randomUUID();
        Type type = Type.fromIndex(tag.getInt("type"));
        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        int number = tag.getInt("number");
        UUID assigned = tag.hasUUID("assigned") ? tag.getUUID("assigned") : null;
        UUID entity = tag.hasUUID("entity") ? tag.getUUID("entity") : null;
        return new GovernorPost(id, type, pos, number, assigned, entity);
    }
}
