package com.talhanation.recruits.entities.ai.controller.ship;

import com.talhanation.recruits.compat.smallships.SmallShipsBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * Everything about the ship itself: what she is (size, drive, guns - read from
 * Small Ships when the captain takes the helm, and again now and then because
 * dockyard upgrades change it), how she is right now (hull, canvas, wind,
 * ammo - {@link #update} every second) and what follows from that (stopping
 * distance, speed under a sail state, how far a gun carries).
 *
 * Speeds are blocks per tick, turn rates degrees per tick. Every number the
 * captain steers or aims by comes from here - no magic numbers per ship type.
 */
public final class ShipProfile {

    public enum Ammo { BALL, CHAINED, GRAPE }

    /* ---- what she is ---- */
    public final float length, beam, airDraft;
    public final int requiredDepth;
    public final float maxSpeed, speedDecay, rotBrake, manoeuvreSpeed, oarFactor;
    public final boolean hasSails, hasOars;
    public final int cannonsPort, cannonsStarboard;

    /* ---- how she is, see update ---- */
    private float maxSpeedNow, sailSpeedFactor = 1.0F, windMultiplier = 1.0F, windStrength;
    private boolean headWind;
    private float damage, maxHealth = 1.0F, handRepairFloor, sailHealth, sailMaxHealth;
    private int sailCondition, balls;
    private final int[] ammo = new int[Ammo.values().length];
    private boolean fineGrain, sinking, dockyardWork, read;

    private ShipProfile(CompoundTag tag) {
        this.length = tag.getFloat("Length");
        this.beam = tag.getFloat("Beam");
        this.airDraft = tag.getFloat("AirDraft");
        this.requiredDepth = Math.max(1, Math.min(ShipPathfinder.MAX_DEPTH, tag.getInt("RequiredDepth")));
        this.maxSpeed = tag.getFloat("MaxSpeed");
        this.speedDecay = tag.getFloat("SpeedDecay");
        this.rotBrake = tag.getFloat("RotStep") + tag.getFloat("RotDecay");
        this.manoeuvreSpeed = tag.getFloat("ManoeuvreSpeed");
        this.hasSails = tag.getBoolean("HasSails");
        this.hasOars = tag.getBoolean("HasOars");
        this.oarFactor = tag.getFloat("OarFactor");
        this.cannonsPort = tag.getInt("CannonsPort");
        this.cannonsStarboard = tag.getInt("CannonsStarboard");
        this.maxSpeedNow = this.maxSpeed;
    }

    /** @return the profile of this ship, or null if it is none or Small Ships is not there */
    @Nullable
    public static ShipProfile of(@Nullable Entity ship) {
        CompoundTag tag = SmallShipsBridge.getProfile(ship);
        if (tag.getFloat("MaxSpeed") <= 0.0F || tag.getFloat("SpeedDecay") <= 0.0F) return null;
        ShipProfile profile = new ShipProfile(tag);
        profile.update(ship);
        return profile;
    }

    /**
     * Reads her condition anew - about once a second.
     *
     * @return true if the hull took damage since the last read
     */
    public boolean update(Entity ship) {
        CompoundTag c = SmallShipsBridge.getCondition(ship);
        if (c.isEmpty()) return false;
        float damage = c.getFloat("Damage");
        boolean hit = this.read && damage > this.damage + 0.5F;
        this.damage = damage;
        this.maxHealth = Math.max(1.0F, c.getFloat("MaxHealth"));
        this.handRepairFloor = c.getFloat("HandRepairFloor");
        this.sailCondition = c.getInt("SailCondition");
        this.sailHealth = c.getFloat("SailHealth");
        this.sailMaxHealth = c.getFloat("SailMaxHealth");
        CompoundTag ammo = c.getCompound("Ammo");
        for (Ammo type : Ammo.values()) this.ammo[type.ordinal()] = ammo.getInt(type.name());
        this.balls = c.getInt("CannonBalls");
        this.fineGrain = c.getInt("FineGrain") > 0;
        this.sinking = c.getBoolean("Sinking") || c.getBoolean("Sunken");
        this.dockyardWork = c.getBoolean("DockyardWork");
        float now = c.getFloat("MaxSpeedNow");
        this.maxSpeedNow = now > 0.0F ? now : this.maxSpeed;
        this.sailSpeedFactor = c.getFloat("SailSpeedFactor");
        this.windMultiplier = c.getFloat("WindMultiplier");
        this.windStrength = c.getFloat("WindStrength");
        this.headWind = c.getInt("WindZone") == 0;
        this.read = true;
        return hit;
    }

    /* ---------------- condition ---------------- */

    /** @return hull left, 0..1 */
    public float hull() {
        return 1.0F - Math.min(this.damage, this.maxHealth) / this.maxHealth;
    }

    /** @return true if a hand repair would still put hull back (up to 66 %) */
    public boolean canPatchHull() {
        return this.damage > this.handRepairFloor + 0.5F;
    }

    /** @return true if the canvas is torn and a patch would still help (a third of it at most) */
    public boolean canPatchSails() {
        return this.sailMaxHealth > 0.0F && this.sailHealth < this.sailMaxHealth / 3.0F;
    }

    /** @return 0 intact, 1 torn, 2 destroyed */
    public int sailCondition() {
        return this.sailCondition;
    }

    /** @return false with the canvas gone and no oars - she can only turn */
    public boolean canMakeWay() {
        return this.sailCondition < 2 || this.hasOars;
    }

    public int guns() {
        return this.cannonsPort + this.cannonsStarboard;
    }

    public int balls() {
        return this.balls;
    }

    public int ammo(Ammo type) {
        return this.ammo[type.ordinal()];
    }

    public boolean fineGrain() {
        return this.fineGrain;
    }

    public boolean isSinking() {
        return this.sinking;
    }

    public boolean inDockyard() {
        return this.dockyardWork;
    }

    /* ---------------- handling ---------------- */

    /** @return blocks/tick she can make right now - wind, damage and load included */
    public float maxSpeedNow() {
        return this.maxSpeedNow;
    }

    /** @return blocks she still makes with nothing driving her, see Ship#calculateSpeed */
    public float stoppingDistance(float speed) {
        return speed * speed / (2.0F * this.speedDecay);
    }

    /** @return the speed from which she just comes to rest within this distance, coasting */
    public float coastSpeedFor(float distance) {
        return distance <= 0.0F ? 0.0F : (float) Math.sqrt(2.0F * this.speedDecay * distance);
    }

    /** @return degrees the bow still swings while a turn of rotSpeed is braked with counter rudder */
    public float brakeAngle(float rotSpeed) {
        return rotSpeed * rotSpeed / (2.0F * this.rotBrake);
    }

    /** @return the drive under a sail state 1..4 - the formula of Ship#controlShip */
    public float sailSpeed(int sailState) {
        float share = sailState / 4.0F;
        float wind = 1.0F + (this.windMultiplier - 1.0F) * this.windStrength * share;
        return this.maxSpeedNow * share * this.sailSpeedFactor * wind;
    }

    /** @return true if oars (or creeping, sails 0 + W) serve better than canvas for this speed */
    public boolean rowFor(float wanted) {
        if (!this.hasSails || this.sailSpeedFactor <= 0.0F) return true;
        if (!this.hasOars) return false;
        float oarSpeed = this.maxSpeedNow * this.oarFactor;
        // oars faster than full canvas (head wind, torn sails), or good enough while the canvas suffers
        return oarSpeed >= this.sailSpeed(4) || (oarSpeed >= wanted * 0.9F && (this.headWind || this.sailSpeedFactor < 1.0F));
    }

    /**
     * @return the sail state for the wanted speed, with the canvas now set in
     * mind: a step up only once it falls clearly short, a step down only with
     * room to spare - small changes of the wanted speed leave the sails alone
     */
    public int sailStateFor(float wanted, int current) {
        int lowest = 4;
        for (int state = 1; state <= 4; state++) {
            if (this.sailSpeed(state) >= wanted * 0.97F) {
                lowest = state;
                break;
            }
        }
        if (current <= 0 || current > 4 || lowest == current) return lowest;
        if (lowest > current) return this.sailSpeed(current) >= wanted * 0.9F ? current : lowest;
        return this.sailSpeed(current - 1) >= wanted * 1.1F ? lowest : current;
    }

    /* ---------------- room she needs ---------------- */

    /** @return blocks either side of the keel line with which the hull just fits, centred */
    public double tightRadius() {
        return Math.max(1.0D, this.beam / 2.0D);
    }

    /** @return blocks either side for the hull with a block to spare */
    public int clearRadius() {
        return (int) Math.ceil(this.beam / 2.0F) + 1;
    }

    /** @return blocks around her centre the hull needs to swing round on the spot */
    public int swingRadius() {
        return (int) Math.ceil(this.length / 2.0F + 1.0F);
    }

    /* ---------------- guns ---------------- */

    /** @return the flight table of this ammo, null without Small Ships */
    @Nullable
    public static Gun gun(Ammo ammo, boolean fineGrain) {
        return Gun.of(ammo, fineGrain);
    }

    /** @return the shared rules of Small Ships (ballistics, fuze, dockyard), read once - null without Small Ships */
    @Nullable
    public static CompoundTag rules() {
        return Gun.rules();
    }

    /**
     * How a shot flies: per elevation (half degree steps) the flight tick by
     * tick, worked out once from the rules with the same drag and gravity
     * Small Ships uses. Asked for (distance, height) it gives the flattest
     * elevation that reaches it and the ticks of flight.
     */
    public static final class Gun {
        private static final float STEP = 0.5F;
        private static final int MAX_TICKS = 240, GRAPE_LIFE = 40;
        private static final double FLOOR = -40.0D, EFFECTIVE_SHARE = 0.6D;
        private static final Map<Ammo, Gun[]> TABLES = new EnumMap<>(Ammo.class);
        @Nullable private static CompoundTag rules;

        /** @param elevation degrees, @param ticks flight time */
        public record Solution(float elevation, float ticks) {}

        private final float minElevation;
        /** per elevation: horizontal distance and height after each tick */
        private final double[][] xs, ys;
        private final double maxRange;

        private Gun(float speed, float drag, float gravity, float minElevation, float maxElevation, int life) {
            this.minElevation = minElevation;
            int count = Math.round((maxElevation - minElevation) / STEP) + 1;
            this.xs = new double[count][];
            this.ys = new double[count][];
            double longest = 0.0D;
            for (int i = 0; i < count; i++) {
                double radians = Math.toRadians(minElevation + i * STEP);
                double vx = speed * Math.cos(radians), vy = speed * Math.sin(radians), x = 0.0D, y = 0.0D;
                double[] tx = new double[life + 1], ty = new double[life + 1];
                int ticks = 0;
                while (ticks < life && y > FLOOR) {
                    x += vx;
                    y += vy;
                    vx *= drag;
                    vy = vy * drag - gravity;
                    ticks++;
                    tx[ticks] = x;
                    ty[ticks] = y;
                }
                this.xs[i] = Arrays.copyOf(tx, ticks + 1);
                this.ys[i] = Arrays.copyOf(ty, ticks + 1);
                // the reach: where the flight sinks below a block under the muzzle
                double reach = this.xs[i][ticks];
                for (int k = 1; k <= ticks; k++) {
                    if (this.ys[i][k] < -1.0D) {
                        reach = this.xs[i][k - 1];
                        break;
                    }
                }
                longest = Math.max(longest, reach);
            }
            this.maxRange = longest;
        }

        @Nullable
        static Gun of(Ammo ammo, boolean fineGrain) {
            Gun[] pair = TABLES.get(ammo);
            if (pair == null) {
                CompoundTag rules = rules();
                if (rules == null) return null;
                CompoundTag type = rules.getCompound("AmmoTypes").getCompound(ammo.name());
                float speed = rules.getFloat("CannonSpeed") * (type.contains("Speed") ? type.getFloat("Speed") : 1.0F);
                float drag = rules.getFloat("ShotDrag"), gravity = rules.getFloat("ShotGravity");
                float min = rules.getFloat("AimMin"), max = rules.getFloat("AimMax");
                int life = ammo == Ammo.GRAPE ? GRAPE_LIFE : MAX_TICKS;
                pair = new Gun[]{new Gun(speed, drag, gravity, min, max, life),
                        new Gun(speed * rules.getFloat("FineGrainFactor"), drag, gravity, min, max, life)};
                TABLES.put(ammo, pair);
            }
            return pair[fineGrain ? 1 : 0];
        }

        @Nullable
        static CompoundTag rules() {
            if (rules == null) {
                CompoundTag tag = SmallShipsBridge.getRules();
                if (tag.getFloat("CannonSpeed") <= 0.0F) return null;
                rules = tag;
            }
            return rules;
        }

        /** @return blocks the shot carries at best, landing a block below the muzzle */
        public double maxRange() {
            return this.maxRange;
        }

        /** @return blocks up to which a moving target is still hit well */
        public double effectiveRange() {
            return this.maxRange * EFFECTIVE_SHARE;
        }

        /** @return the flattest elevation that reaches (distance, dy over the muzzle), null if out of reach */
        @Nullable
        public Solution solve(double distance, double dy) {
            int last = this.xs.length - 1;
            if (this.heightAt(last, distance) < dy) return null; // even the steepest shot falls short
            if (this.heightAt(0, distance) >= dy) return new Solution(this.minElevation, (float) this.ticksAt(0, distance));
            // the height at that distance grows with the elevation up to the aim limit
            int low = 0, high = last;
            while (high - low > 1) {
                int mid = (low + high) >>> 1;
                if (this.heightAt(mid, distance) >= dy) high = mid;
                else low = mid;
            }
            double below = this.heightAt(low, distance), above = this.heightAt(high, distance);
            double share = above - below < 1.0E-6D ? 1.0D : (dy - below) / (above - below);
            float ticks = (float) (this.ticksAt(low, distance) + share * (this.ticksAt(high, distance) - this.ticksAt(low, distance)));
            return new Solution(this.minElevation + (float) ((low + share) * STEP), ticks);
        }

        private double heightAt(int elevation, double distance) {
            double[] x = this.xs[elevation], y = this.ys[elevation];
            int k = index(x, distance);
            if (k < 0) return Double.NEGATIVE_INFINITY;
            return y[k - 1] + (distance - x[k - 1]) / Math.max(1.0E-6D, x[k] - x[k - 1]) * (y[k] - y[k - 1]);
        }

        private double ticksAt(int elevation, double distance) {
            double[] x = this.xs[elevation];
            int k = index(x, distance);
            if (k < 0) return x.length - 1;
            return k - 1 + (distance - x[k - 1]) / Math.max(1.0E-6D, x[k] - x[k - 1]);
        }

        /** @return the first tick at or past the distance (>= 1), -1 if the flight ends before */
        private static int index(double[] x, double distance) {
            if (x.length < 2 || x[x.length - 1] < distance) return -1;
            int low = 1, high = x.length - 1;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (x[mid] >= distance) high = mid;
                else low = mid + 1;
            }
            return low;
        }
    }
}
