package com.talhanation.recruits.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One group on a simulated journey: the leader and his company are out of the world and only this record moves.
 * Moved, put back into the world and saved by the {@link SimulatedTravelManager}; other classes only read it.
 */
public class SimulatedTravel {
    public final UUID leader;
    @Nullable
    public final UUID owner;
    /** Dimension id, e.g. minecraft:overworld. */
    public final ResourceLocation dimension;
    public double x;
    public double y;
    public double z;
    /** Heading in degrees, for the facing when the group is put back. */
    public float yaw;
    /** Replaced when the leader gets a new journey while he is out of the world, see {@code SimulatedTravelManager}. */
    public TravelPlan plan;
    /** Blocks per second. */
    public final double speed;
    /** Game time (overworld) of the last step. */
    public long lastStep;
    /** Game time (overworld) when the group left the world; the time away is made up for when it comes back. */
    public final long packedAt;
    /** Blocks the group stays out of the world no matter who is near, after it was stuck in the terrain. */
    public double skip;
    /** Blocks around the group in which a player counts as watching it. */
    public final int watchRange;
    /** The leader first, then the company. Each tag is a root vehicle with its passengers. */
    public final List<CompoundTag> entities;
    /** Standing at the end of what the plan has right now. Not saved, worked out on every step. */
    public transient boolean atDestination;

    public SimulatedTravel(UUID leader, @Nullable UUID owner, ResourceLocation dimension, double x, double y, double z, float yaw,
                           TravelPlan plan, double speed, long packedAt, double skip, int watchRange, List<CompoundTag> entities) {
        this.leader = leader;
        this.owner = owner;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.plan = plan;
        this.speed = speed;
        this.packedAt = packedAt;
        this.lastStep = packedAt;
        this.skip = skip;
        this.watchRange = watchRange;
        this.entities = entities;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Leader", leader);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putString("Dimension", dimension.toString());
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        tag.putFloat("Yaw", yaw);
        tag.put("Plan", plan.save());
        tag.putDouble("Speed", speed);
        tag.putLong("LastStep", lastStep);
        tag.putLong("PackedAt", packedAt);
        tag.putDouble("Skip", skip);
        tag.putInt("WatchRange", watchRange);
        ListTag list = new ListTag();
        list.addAll(entities);
        tag.put("Entities", list);
        return tag;
    }

    /** @return null if the tag is broken (no leader, no dimension or no entities) */
    @Nullable
    public static SimulatedTravel load(CompoundTag tag) {
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (!tag.hasUUID("Leader") || dimension == null) return null;

        List<CompoundTag> entities = new ArrayList<>();
        ListTag list = tag.getList("Entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) entities.add(list.getCompound(i));
        if (entities.isEmpty()) return null;

        SimulatedTravel travel = new SimulatedTravel(tag.getUUID("Leader"), tag.hasUUID("Owner") ? tag.getUUID("Owner") : null, dimension,
                tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"), tag.getFloat("Yaw"),
                TravelPlan.load(tag.getCompound("Plan")), tag.getDouble("Speed"), tag.getLong("PackedAt"),
                tag.getDouble("Skip"), tag.getInt("WatchRange"), entities);
        travel.lastStep = tag.getLong("LastStep");
        return travel;
    }
}
