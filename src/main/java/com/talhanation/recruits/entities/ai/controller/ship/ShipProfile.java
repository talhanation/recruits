package com.talhanation.recruits.entities.ai.controller.ship;

import com.talhanation.recruits.compat.smallships.SmallShipsBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * What a ship IS: size, drive and handling, read once from Small Ships when the
 * captain takes the helm (SmallShipsBridge#getProfile) and again now and then,
 * because dockyard upgrades change the handling.
 *
 * Speeds are blocks per tick, turn rates degrees per tick. Every number the
 * captain steers by comes from here - no magic constants per ship type.
 */
public record ShipProfile(
        float length, float beam, float airDraft, float mass, int requiredDepth,
        float maxHealth, float sailMaxHealth,
        float maxSpeed, float acceleration, float speedDecay,
        float maxRotSpeed, float rotStep, float rotDecay,
        float manoeuvreSpeed, int sailCooldown,
        boolean hasSails, boolean hasOars, float oarFactor, float ramSelfDamage,
        float headWind, float sideWind, float tailWind,
        int cannonsPort, int cannonsStarboard) {

    /** @return the profile of this ship, or null if it is none or Small Ships is not there. */
    @Nullable
    public static ShipProfile of(@Nullable Entity ship) {
        return fromTag(SmallShipsBridge.getProfile(ship));
    }

    @Nullable
    public static ShipProfile fromTag(CompoundTag tag) {
        if (tag.getFloat("MaxSpeed") <= 0.0F || tag.getFloat("SpeedDecay") <= 0.0F) return null;
        return new ShipProfile(
                tag.getFloat("Length"), tag.getFloat("Beam"), tag.getFloat("AirDraft"), tag.getFloat("Mass"), tag.getInt("RequiredDepth"),
                tag.getFloat("MaxHealth"), tag.getFloat("SailMaxHealth"),
                tag.getFloat("MaxSpeed"), tag.getFloat("Acceleration"), tag.getFloat("SpeedDecay"),
                tag.getFloat("MaxRotSpeed"), tag.getFloat("RotStep"), tag.getFloat("RotDecay"),
                tag.getFloat("ManoeuvreSpeed"), tag.getInt("SailCooldown"),
                tag.getBoolean("HasSails"), tag.getBoolean("HasOars"), tag.getFloat("OarFactor"), tag.getFloat("RamSelfDamage"),
                tag.getFloat("HeadWind"), tag.getFloat("SideWind"), tag.getFloat("TailWind"),
                tag.getInt("CannonsPort"), tag.getInt("CannonsStarboard"));
    }

    /** turn rate lost per tick with the rudder laid AGAINST the turn */
    public float rotBrake() {
        return this.rotStep + this.rotDecay;
    }

    /** @return degrees the bow still swings while a turn of rotSpeed is braked with counter rudder */
    public float brakeAngle(float rotSpeed) {
        return rotSpeed * rotSpeed / (2.0F * this.rotBrake());
    }

    /** @return blocks a ship still makes with nothing driving her, see Ship#calculateSpeed */
    public float stoppingDistance(float speed) {
        return speed * speed / (2.0F * this.speedDecay);
    }

    /** @return the speed from which she just comes to rest within this distance, coasting */
    public float coastSpeedFor(float distance) {
        return distance <= 0.0F ? 0.0F : (float) Math.sqrt(2.0F * this.speedDecay * distance);
    }
}
