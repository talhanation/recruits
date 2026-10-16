package com.talhanation.recruits.compat.smallships;

import com.talhanation.recruits.Main;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

/**
 * The ONLY class in Recruits that talks to Small Ships - and only by name.
 *
 * Small Ships offers {@code com.talhanation.smallships.api.ShipBridge}: static
 * methods with vanilla and JDK types only. This class binds them once, the
 * first time it is used, as {@code static final} MethodHandles - after the JIT
 * that is as fast as a direct call. Nothing here or anywhere else in Recruits
 * may import a Small Ships class, so Recruits compiles and runs without it.
 *
 * Without Small Ships, with a Small Ships older than {@link #REQUIRED_VERSION}
 * or with a method missing, {@link #isAvailable()} is false and every method
 * answers neutrally: false, 0, an empty tag, the unchanged delta. A captain then
 * simply has no ship.
 *
 * Small Ships owns every rule (cooldowns, prices, limits). Through here a
 * captain only presses the keys a player has and pays what a player pays.
 */
public final class SmallShipsBridge {
    /** the ShipBridge.VERSION this class was written against */
    public static final int REQUIRED_VERSION = 1;
    private static final String BRIDGE_CLASS = "com.talhanation.smallships.api.ShipBridge";
    /**
     * Scoreboard tag that keeps a mob on a still deck instead of being walked
     * ashore - ShipBridge.STAY_ON_DECK_TAG. Plain vanilla (Entity#addTag), so
     * it works by this string alone.
     */
    public static final String STAY_ON_DECK_TAG = "smallships.stay_on_deck";

    private static boolean errorLogged;

    private SmallShipsBridge() {}

    /** @return true if Small Ships is loaded and offers every method used here. */
    public static boolean isAvailable() {
        return Main.isSmallShipsLoaded && Handles.AVAILABLE;
    }

    /* ---------------- recognition & state ---------------- */

    public static boolean isShip(@Nullable Entity entity) {
        if (entity == null || !isAvailable()) return false;
        try { return (boolean) Handles.IS_SHIP.invokeExact(entity); }
        catch (Throwable t) { return fail("isShip", t, false); }
    }

    /** @return the ship itself or the ship behind one of its hull parts (hits land on parts), else null. */
    @Nullable
    public static Entity getShipOf(@Nullable Entity entity) {
        if (entity == null || !isAvailable()) return null;
        try { return (Entity) Handles.GET_SHIP_OF.invokeExact(entity); }
        catch (Throwable t) { return fail("getShipOf", t, null); }
    }

    /** @return whoever stands at the helm and may steer - player or NPC - or null. */
    @Nullable
    public static Entity getHelmsman(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return null;
        try { return (Entity) Handles.GET_HELMSMAN.invokeExact(ship); }
        catch (Throwable t) { return fail("getHelmsman", t, null); }
    }

    /** @return true if the entity stands at the helm of this ship and may steer it. */
    public static boolean isHelmsman(@Nullable Entity ship, @Nullable Entity entity) {
        if (ship == null || entity == null || !isAvailable()) return false;
        try { return (boolean) Handles.IS_HELMSMAN.invokeExact(ship, entity); }
        catch (Throwable t) { return fail("isHelmsman", t, false); }
    }

    /** @return true if the ship is afloat, free, not in the dockyard and not going down. */
    public static boolean canMakeWay(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return false;
        try { return (boolean) Handles.CAN_MAKE_WAY.invokeExact(ship); }
        catch (Throwable t) { return fail("canMakeWay", t, false); }
    }

    /** @return blocks per tick along the bow, negative astern. */
    public static float getSpeed(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return 0.0F;
        try { return (float) Handles.GET_SPEED.invokeExact(ship); }
        catch (Throwable t) { return fail("getSpeed", t, 0.0F); }
    }

    /** @return degrees per tick, positive = to starboard. */
    public static float getRotSpeed(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return 0.0F;
        try { return (float) Handles.GET_ROT_SPEED.invokeExact(ship); }
        catch (Throwable t) { return fail("getRotSpeed", t, 0.0F); }
    }

    /** @return 0 = furled ... 4 = full sail. */
    public static int getSailState(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return 0;
        try { return (int) Handles.GET_SAIL_STATE.invokeExact(ship); }
        catch (Throwable t) { return fail("getSailState", t, 0); }
    }

    /** @return ticks until W/S may change the sail state again. */
    public static int getSailCooldown(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return 0;
        try { return (int) Handles.GET_SAIL_COOLDOWN.invokeExact(ship); }
        catch (Throwable t) { return fail("getSailCooldown", t, 0); }
    }

    /** What the ship is (size, drive, cannons) - see ShipBridge#getProfile for the keys. Empty without a ship. */
    public static CompoundTag getProfile(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return new CompoundTag();
        try { return (CompoundTag) Handles.GET_PROFILE.invokeExact(ship); }
        catch (Throwable t) { return fail("getProfile", t, new CompoundTag()); }
    }

    /** The rules every ship shares (ballistics, ram limits, repair prices) - see ShipBridge#getRules for the keys. */
    public static CompoundTag getRules() {
        if (!isAvailable()) return new CompoundTag();
        try { return (CompoundTag) Handles.GET_RULES.invokeExact(); }
        catch (Throwable t) { return fail("getRules", t, new CompoundTag()); }
    }

    /** How the ship is right now (damage, wind, ammo) - see ShipBridge#getCondition for the keys. Empty without a ship. */
    public static CompoundTag getCondition(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return new CompoundTag();
        try { return (CompoundTag) Handles.GET_CONDITION.invokeExact(ship); }
        catch (Throwable t) { return fail("getCondition", t, new CompoundTag()); }
    }

    /* ---------------- input: the keys a player has ---------------- */

    /** W/S/A/D. Only takes effect while the captain stands at the helm. */
    public static void pressKeys(@Nullable Entity ship, boolean forward, boolean backward, boolean left, boolean right) {
        if (ship == null || !isAvailable()) return;
        try { Handles.PRESS_KEYS.invokeExact(ship, forward, backward, left, right); }
        catch (Throwable t) { fail("pressKeys", t, null); }
    }

    /** The sail key: furls set sails at once, or sets furled sails to state 1. */
    public static void pressSailKey(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return;
        try { Handles.PRESS_SAIL_KEY.invokeExact(ship); }
        catch (Throwable t) { fail("pressSailKey", t, null); }
    }

    /* ---------------- broadside ---------------- */

    public static void setBroadsideAim(@Nullable Entity ship, boolean starboard, float pitch, float rotation) {
        if (ship == null || !isAvailable()) return;
        try { Handles.SET_BROADSIDE_AIM.invokeExact(ship, starboard, pitch, rotation); }
        catch (Throwable t) { fail("setBroadsideAim", t, null); }
    }

    /** @return how many cannons of that side were triggered (cooldown, fuze and ammo apply). */
    public static int fireBroadside(@Nullable Entity ship, @Nullable LivingEntity shooter, boolean starboard) {
        if (ship == null || shooter == null || !isAvailable()) return 0;
        try { return (int) Handles.FIRE_BROADSIDE.invokeExact(ship, shooter, starboard); }
        catch (Throwable t) { return fail("fireBroadside", t, 0); }
    }

    /** The mounted guns: Slot, Starboard, X/Y/Z of the muzzle, Ready, Manned - see ShipBridge#getCannons. */
    public static ListTag getCannons(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return new ListTag();
        try { return (ListTag) Handles.GET_CANNONS.invokeExact(ship); }
        catch (Throwable t) { return fail("getCannons", t, new ListTag()); }
    }

    /** Fires the gun of one slot for the NPC sitting at its GUNNER post. @return true if triggered */
    public static boolean fireCannon(@Nullable Entity ship, @Nullable LivingEntity gunner, int slot) {
        if (ship == null || gunner == null || !isAvailable()) return false;
        try { return (boolean) Handles.FIRE_CANNON.invokeExact(ship, gunner, slot); }
        catch (Throwable t) { return fail("fireCannon", t, false); }
    }

    /** The ammo type an NPC shooter loads from now on: "BALL", "CHAINED" or "GRAPE". @return false if unknown */
    public static boolean setAmmoPreference(@Nullable Entity shooter, @Nullable String type) {
        if (shooter == null || type == null || !isAvailable()) return false;
        try { return (boolean) Handles.SET_AMMO_PREFERENCE.invokeExact(shooter, type); }
        catch (Throwable t) { return fail("setAmmoPreference", t, false); }
    }

    /* ---------------- repair: the players' prices and limits ---------------- */

    /** One hand repair paid from the container (1 iron nugget + 1 plank), up to the hand repair limit. */
    public static boolean repairHullByHand(@Nullable Entity ship, @Nullable Container payer) {
        if (ship == null || payer == null || !isAvailable()) return false;
        try { return (boolean) Handles.REPAIR_HULL_BY_HAND.invokeExact(ship, payer); }
        catch (Throwable t) { return fail("repairHullByHand", t, false); }
    }

    /** One sail patch paid from the container (1 string + 1 iron nugget), up to a third of the canvas. */
    public static boolean patchSails(@Nullable Entity ship, @Nullable Container payer) {
        if (ship == null || payer == null || !isAvailable()) return false;
        try { return (boolean) Handles.PATCH_SAILS.invokeExact(ship, payer); }
        catch (Throwable t) { return fail("patchSails", t, false); }
    }

    /** What a dockyard repair costs right now: Costs (list of Item|Tag + Count), Time (ticks). Empty without a ship. */
    public static CompoundTag getDockyardRepairCosts(@Nullable Entity ship, boolean hull, boolean sails) {
        if (ship == null || !isAvailable()) return new CompoundTag();
        try { return (CompoundTag) Handles.GET_DOCKYARD_REPAIR_COSTS.invokeExact(ship, hull, sails); }
        catch (Throwable t) { return fail("getDockyardRepairCosts", t, new CompoundTag()); }
    }

    public static boolean isDockyard(@Nullable Level level, @Nullable BlockPos pos) {
        if (level == null || pos == null || !isAvailable()) return false;
        try { return (boolean) Handles.IS_DOCKYARD.invokeExact(level, pos); }
        catch (Throwable t) { return fail("isDockyard", t, false); }
    }

    /** Starts a dockyard repair paid from the container - same costs and work time as the players' button. */
    public static boolean startDockyardRepair(@Nullable Level level, @Nullable BlockPos dockyard, @Nullable Entity ship,
                                              @Nullable Container payer, boolean hull, boolean sails) {
        if (level == null || dockyard == null || ship == null || payer == null || !isAvailable()) return false;
        try { return (boolean) Handles.START_DOCKYARD_REPAIR.invokeExact(level, dockyard, ship, payer, hull, sails); }
        catch (Throwable t) { return fail("startDockyardRepair", t, false); }
    }

    /* ---------------- crew ---------------- */

    /** The stations: Id, Type, Slot, X/Y/Z, Blocked, Occupant (UUID) - see ShipBridge#getSeats. */
    public static ListTag getSeats(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return new ListTag();
        try { return (ListTag) Handles.GET_SEATS.invokeExact(ship); }
        catch (Throwable t) { return fail("getSeats", t, new ListTag()); }
    }

    /** Moves a passenger already aboard to another free station. @return true if he sits there now */
    public static boolean takeSeat(@Nullable Entity ship, @Nullable Entity passenger, int seatId) {
        if (ship == null || passenger == null || !isAvailable()) return false;
        try { return (boolean) Handles.TAKE_SEAT.invokeExact(ship, passenger, seatId); }
        catch (Throwable t) { return fail("takeSeat", t, false); }
    }

    /* ---------------- geometry ---------------- */

    /** @return the world boxes of the hull, masts left out - their tops are the deck. Empty without a ship. */
    @SuppressWarnings("unchecked")
    public static List<AABB> getHullBoxes(@Nullable Entity ship) {
        if (ship == null || !isAvailable()) return List.of();
        try { return (List<AABB>) Handles.GET_HULL_BOXES.invokeExact(ship); }
        catch (Throwable t) { return fail("getHullBoxes", t, List.of()); }
    }

    /** @return the part of delta the hull (masts included) could move against blocks and other hulls. */
    public static Vec3 sweep(@Nullable Entity ship, Vec3 delta) {
        if (ship == null || !isAvailable()) return delta;
        try { return (Vec3) Handles.SWEEP.invokeExact(ship, delta); }
        catch (Throwable t) { return fail("sweep", t, delta); }
    }

    /** @return the other ship this hull would stand in after moving by delta, or null. */
    @Nullable
    public static Entity findShipAhead(@Nullable Entity ship, Vec3 delta) {
        if (ship == null || !isAvailable()) return null;
        try { return (Entity) Handles.FIND_SHIP_AHEAD.invokeExact(ship, delta); }
        catch (Throwable t) { return fail("findShipAhead", t, null); }
    }

    /* ---------------- internals ---------------- */

    /** Logs the first failure with its stack trace, every later one quietly. */
    private static <T> T fail(String method, Throwable t, T fallback) {
        if (!errorLogged) {
            errorLogged = true;
            Main.LOGGER.error("Small Ships bridge call {} failed", method, t);
        } else {
            Main.LOGGER.debug("Small Ships bridge call {} failed: {}", method, t.toString());
        }
        return fallback;
    }

    /**
     * Loaded on first use only - isAvailable() checks Main.isSmallShipsLoaded
     * first, so without Small Ships this class is never even initialised.
     * The fields are static final on purpose: the JIT treats them as constants.
     */
    private static final class Handles {
        private static boolean complete = true;
        private static final Class<?> BRIDGE = findBridge();

        static final MethodHandle IS_SHIP = find("isShip", boolean.class, Entity.class);
        static final MethodHandle GET_SHIP_OF = find("getShipOf", Entity.class, Entity.class);
        static final MethodHandle GET_HELMSMAN = find("getHelmsman", Entity.class, Entity.class);
        static final MethodHandle IS_HELMSMAN = find("isHelmsman", boolean.class, Entity.class, Entity.class);
        static final MethodHandle CAN_MAKE_WAY = find("canMakeWay", boolean.class, Entity.class);
        static final MethodHandle GET_SPEED = find("getSpeed", float.class, Entity.class);
        static final MethodHandle GET_ROT_SPEED = find("getRotSpeed", float.class, Entity.class);
        static final MethodHandle GET_SAIL_STATE = find("getSailState", int.class, Entity.class);
        static final MethodHandle GET_SAIL_COOLDOWN = find("getSailCooldown", int.class, Entity.class);
        static final MethodHandle GET_PROFILE = find("getProfile", CompoundTag.class, Entity.class);
        static final MethodHandle GET_RULES = find("getRules", CompoundTag.class);
        static final MethodHandle GET_CONDITION = find("getCondition", CompoundTag.class, Entity.class);
        static final MethodHandle PRESS_KEYS = find("pressKeys", void.class, Entity.class, boolean.class, boolean.class, boolean.class, boolean.class);
        static final MethodHandle PRESS_SAIL_KEY = find("pressSailKey", void.class, Entity.class);
        static final MethodHandle SET_BROADSIDE_AIM = find("setBroadsideAim", void.class, Entity.class, boolean.class, float.class, float.class);
        static final MethodHandle FIRE_BROADSIDE = find("fireBroadside", int.class, Entity.class, LivingEntity.class, boolean.class);
        static final MethodHandle GET_CANNONS = find("getCannons", ListTag.class, Entity.class);
        static final MethodHandle FIRE_CANNON = find("fireCannon", boolean.class, Entity.class, LivingEntity.class, int.class);
        static final MethodHandle SET_AMMO_PREFERENCE = find("setAmmoPreference", boolean.class, Entity.class, String.class);
        static final MethodHandle REPAIR_HULL_BY_HAND = find("repairHullByHand", boolean.class, Entity.class, Container.class);
        static final MethodHandle PATCH_SAILS = find("patchSails", boolean.class, Entity.class, Container.class);
        static final MethodHandle GET_DOCKYARD_REPAIR_COSTS = find("getDockyardRepairCosts", CompoundTag.class, Entity.class, boolean.class, boolean.class);
        static final MethodHandle IS_DOCKYARD = find("isDockyard", boolean.class, Level.class, BlockPos.class);
        static final MethodHandle START_DOCKYARD_REPAIR = find("startDockyardRepair", boolean.class, Level.class, BlockPos.class, Entity.class, Container.class, boolean.class, boolean.class);
        static final MethodHandle GET_SEATS = find("getSeats", ListTag.class, Entity.class);
        static final MethodHandle TAKE_SEAT = find("takeSeat", boolean.class, Entity.class, Entity.class, int.class);
        static final MethodHandle GET_HULL_BOXES = find("getHullBoxes", List.class, Entity.class);
        static final MethodHandle SWEEP = find("sweep", Vec3.class, Entity.class, Vec3.class);
        static final MethodHandle FIND_SHIP_AHEAD = find("findShipAhead", Entity.class, Entity.class, Vec3.class);

        static final boolean AVAILABLE = BRIDGE != null && complete && checkVersion();

        @Nullable
        private static Class<?> findBridge() {
            try {
                return Class.forName(BRIDGE_CLASS);
            } catch (Throwable t) {
                Main.LOGGER.warn("Small Ships is loaded but has no {} - captains cannot sail it. Small Ships 2.0.1 or newer is required.", BRIDGE_CLASS);
                return null;
            }
        }

        @Nullable
        private static MethodHandle find(String name, Class<?> returnType, Class<?>... parameters) {
            if (BRIDGE == null) return null;
            try {
                return MethodHandles.publicLookup().findStatic(BRIDGE, name, MethodType.methodType(returnType, parameters));
            } catch (Throwable t) {
                complete = false;
                Main.LOGGER.warn("Small Ships bridge has no method {}{} - ship compat disabled.", name, MethodType.methodType(returnType, parameters));
                return null;
            }
        }

        private static boolean checkVersion() {
            try {
                int version = BRIDGE.getField("VERSION").getInt(null);
                if (version >= REQUIRED_VERSION) return true;
                Main.LOGGER.warn("Small Ships bridge version {} is older than the required {} - ship compat disabled.", version, REQUIRED_VERSION);
            } catch (Throwable t) {
                Main.LOGGER.warn("Small Ships bridge has no VERSION - ship compat disabled.");
            }
            return false;
        }
    }
}
