package com.talhanation.recruits.compat.smallships;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.SiegeEngineerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Reflection wrapper for the ground cannon of Small Ships ({@code GroundCannonEntity}, 2.0.2), the same way
 * {@link com.talhanation.recruits.compat.siegeweapons.Ballista} wraps the Siege Weapons ballista.
 *
 * Temporary: ShipBridge has no ground cannon methods yet. When a future Small Ships adds them, this belongs
 * into {@link SmallShipsBridge} and the reflection on Small Ships internals goes away.
 *
 * Only members Small Ships declares itself are reflected - their names are not obfuscated in production.
 * Everything is bound once; without Small Ships (or with a changed class) {@link #isAvailable()} is false
 * and every method answers neutrally.
 *
 * Two things GroundCannonEntity does not do for an NPC gunner, handled here:
 * <ul>
 *   <li>Ammo: {@code GroundCannonEntity#trigger} only takes balls from a Player or an ICannonBallSource driver.
 *   {@link #fire()} does the same steps with the engineer's inventory: his preferred ball (CannonAmmoSelection,
 *   like the mouse wheel), fine grain powder x1.5 if he carries it, then {@code Cannon#triggerFuze}. Cooldown,
 *   fuze delay, muzzle speed and the projectile itself stay Small Ships'.</li>
 *   <li>Barrel: {@code testEntityIntersection} pulls every non-player that touches the gun into the barrel and the
 *   next shot fires it. {@link #handleRecruitInBarrel} takes recruits out again - they are never ammo.</li>
 * </ul>
 */
public final class GroundCannon {

    /** barrel elevation limits (GroundCannonEntity.PITCH_MIN/MAX), entity x rot convention: negative = up */
    public static final float PITCH_MIN = -30.0F;
    public static final float PITCH_MAX = 10.0F;
    /** what one tick of A/D and of the barrel keys turn, the same for a player */
    public static final float YAW_PER_TICK = 1.0F;
    public static final float PITCH_PER_TICK = 0.75F;

    private static final String NAMESPACE = "smallships";
    private static final Set<String> AMMO = Set.of("cannon_ball", "chained_shot", "grape_shot");
    private static final String FINE_GRAIN_POWDER = "fine_grain_powder";

    private static boolean errorLogged;

    public final Entity entity;
    private final LivingEntity gunner;

    public GroundCannon(Entity entity, LivingEntity gunner) {
        this.entity = entity;
        this.gunner = gunner;
    }

    public Entity getEntity() {
        return entity;
    }

    /* ---------------- recognition ---------------- */

    public static boolean isAvailable() {
        return Main.isSmallShipsLoaded && Main.isSmallShipsCompatible && R.AVAILABLE;
    }

    public static boolean isGroundCannon(@Nullable Entity entity) {
        return entity != null && isAvailable() && R.CANNON_ENTITY.isInstance(entity);
    }

    /* ---------------- driver & barrel ---------------- */

    /** @return whoever works the gun (stands behind it), not the one in the barrel. */
    @Nullable
    public static Entity getDriver(@Nullable Entity cannon) {
        if (!isGroundCannon(cannon)) return null;
        try { return (Entity) R.GET_DRIVER.invoke(cannon); }
        catch (Throwable t) { return fail("getDriver", t, null); }
    }

    public boolean isGunnerDriver() {
        return getDriver(entity) == gunner;
    }

    @SuppressWarnings("unchecked")
    public static boolean isInBarrel(@Nullable Entity cannon, Entity passenger) {
        if (!isGroundCannon(cannon)) return false;
        try {
            Optional<UUID> uuid = (Optional<UUID>) R.GET_BARREL_UUID.invoke(cannon);
            return uuid != null && uuid.isPresent() && uuid.get().equals(passenger.getUUID());
        }
        catch (Throwable t) { return fail("getEntityInBarrelUUID", t, false); }
    }

    /** Turns the one in the barrel into the driver (the gun only tells the two apart by this UUID). */
    public static boolean clearBarrel(Entity cannon) {
        if (!isGroundCannon(cannon) || R.SET_BARREL_UUID == null) return false;
        try { R.SET_BARREL_UUID.invoke(cannon, (Object) null); return true; }
        catch (Throwable t) { return fail("setEntityInBarrelUUID", t, false); }
    }

    /**
     * Called every tick for a recruit riding a ground cannon. If the gun pulled him into its barrel (it does
     * that with every non-player touching it), he is taken out again and set down beside the gun - otherwise
     * the next shot fires him. A siege engineer that was ordered onto this gun and walked into it before he
     * could mount takes the gun instead.
     */
    public static void handleRecruitInBarrel(AbstractRecruitEntity recruit, Entity cannon) {
        if (!isInBarrel(cannon, recruit)) return;

        if (recruit instanceof SiegeEngineerEntity && recruit.getShouldMount() && getDriver(cannon) == null && clearBarrel(cannon)) {
            recruit.setShouldMount(false);
            return;
        }

        recruit.stopRiding();
        // beside the gun, never in front of the muzzle; 1.5 blocks is outside the barrel test (half width 0.5 + 0.2)
        Vec3 side = Vec3.directionFromRotation(0.0F, cannon.getYRot() + (recruit.getRandom().nextBoolean() ? 90.0F : -90.0F));
        Vec3 spot = cannon.position().add(side.scale(1.5D));
        if (recruit.level().noCollision(recruit, recruit.getDimensions(recruit.getPose()).makeBoundingBox(spot))) {
            recruit.teleportTo(spot.x, spot.y, spot.z);
        }
    }

    /* ---------------- controls: only the keys a player has ---------------- */

    /** W/S drive the carriage, A/D turn gun and carriage. */
    public void pressKeys(boolean forward, boolean backward, boolean left, boolean right) {
        if (!isAvailable()) return;
        try { R.UPDATE_CONTROLS.invoke(entity, forward, backward, left, right, gunner); }
        catch (Throwable t) { fail("updateControls", t, null); }
    }

    /** Barrel elevation keys. Up = steeper (x rot goes negative). */
    public void pressBarrelKeys(boolean up, boolean down) {
        if (!isAvailable()) return;
        try { R.UPDATE_BARREL_CONTROLS.invoke(entity, up, down, gunner); }
        catch (Throwable t) { fail("updateBarrelControls", t, null); }
    }

    public void releaseKeys() {
        pressKeys(false, false, false, false);
        pressBarrelKeys(false, false);
    }

    /* ---------------- state ---------------- */

    @Nullable
    private Object core() throws Throwable {
        return R.GET_CANNON.invoke(entity);
    }

    /** @return true if a shot could be started now (not reloading, no fuze burning). */
    public boolean isReady() {
        if (!isAvailable()) return false;
        try {
            Object core = core();
            return core != null && !(boolean) R.IS_COOLDOWN.invoke(core) && !(boolean) R.IS_FUZING.invoke(core);
        }
        catch (Throwable t) { return fail("isReady", t, false); }
    }

    public boolean isFuzing() {
        if (!isAvailable()) return false;
        try {
            Object core = core();
            return core != null && (boolean) R.IS_FUZING.invoke(core);
        }
        catch (Throwable t) { return fail("isFuzing", t, false); }
    }

    /** @return where the ball leaves the barrel right now. */
    public Vec3 getMuzzle() {
        if (isAvailable()) {
            try {
                Object core = core();
                if (core != null) {
                    Vector3d end = (Vector3d) R.GET_BARREL_END.invoke(core);
                    // the core only learns the position in the cannon tick - before the first one it is 0,0,0
                    if (end != null && (end.x != 0 || end.y != 0 || end.z != 0)) return new Vec3(end.x, end.y, end.z);
                }
            }
            catch (Throwable t) { fail("getBarrelEndPoint", t, null); }
        }
        return entity.position().add(0, 0.8D, 0);
    }

    /** @return health in percent (0..100). */
    public float getHealthPercent() {
        if (!isAvailable()) return 100F;
        try {
            float health = (float) R.GET_HEALTH.invoke(entity);
            float max = (float) R.GET_MAX_HEALTH.invoke(entity);
            return max <= 0 ? 100F : health / max * 100F;
        }
        catch (Throwable t) { return fail("getHealth", t, 100F); }
    }

    /* ---------------- ammo ---------------- */

    public static boolean isAmmo(ItemStack stack) {
        ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && NAMESPACE.equals(id.getNamespace()) && AMMO.contains(id.getPath());
    }

    public static boolean isFineGrainPowder(ItemStack stack) {
        ResourceLocation id = stack.isEmpty() ? null : ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && NAMESPACE.equals(id.getNamespace()) && FINE_GRAIN_POWDER.equals(id.getPath());
    }

    public static int countAmmo(SimpleContainer inventory) {
        int count = 0;
        for (ItemStack stack : inventory.items) if (isAmmo(stack)) count += stack.getCount();
        return count;
    }

    public static int countFineGrainPowder(SimpleContainer inventory) {
        int count = 0;
        for (ItemStack stack : inventory.items) if (isFineGrainPowder(stack)) count += stack.getCount();
        return count;
    }

    @Nullable
    private Object findBall() throws Throwable {
        SimpleContainer inventory = gunnerInventory();
        return inventory == null ? null : R.FIND_PREFERRED.invoke(null, gunner, inventory.items, null);
    }

    @Nullable
    private SimpleContainer gunnerInventory() {
        return gunner instanceof AbstractRecruitEntity recruit ? recruit.getInventory() : null;
    }

    public boolean hasAmmo() {
        if (!isAvailable()) return false;
        try { return findBall() != null; }
        catch (Throwable t) { return fail("findPreferred", t, false); }
    }

    /** @return muzzle speed of the next shot in blocks per tick: base speed x ball type x fine grain. */
    public float getNextShotSpeed() {
        float speed = Rules.cannonSpeed;
        if (!isAvailable()) return speed;
        try {
            Object ball = findBall();
            if (ball != null) speed *= R.TYPE_SPEED.getFloat(R.BALL_GET_TYPE.invoke(ball));
        }
        catch (Throwable t) { fail("getType", t, null); }
        SimpleContainer inventory = gunnerInventory();
        if (inventory != null && countFineGrainPowder(inventory) > 0) speed *= Rules.fineGrainFactor;
        return speed;
    }

    /**
     * Lights the fuze, paid from the gunner's inventory - see the class comment.
     *
     * @return true if a shot was started
     */
    public boolean fire() {
        if (!isAvailable() || entity.level().isClientSide() || !isGunnerDriver()) return false;
        SimpleContainer inventory = gunnerInventory();
        if (inventory == null) return false;
        try {
            Object core = core();
            if (core == null || (boolean) R.IS_COOLDOWN.invoke(core) || (boolean) R.IS_FUZING.invoke(core)) return false;
            // something is in the barrel (an animal, a mob): never fire it, that is a players' joke
            if (R.GET_PASSENGER_IN_BARREL.invoke(entity) != null) return false;

            Object ball = findBall();
            if (ball == null) return false;
            Object type = R.BALL_GET_TYPE.invoke(ball);
            if (!(boolean) R.CONSUME_PREFERRED.invoke(null, gunner, inventory.items, null)) return false;

            float speedMultiplier = R.TYPE_SPEED.getFloat(type);
            boolean fineGrain = consumeFineGrainPowder(inventory);
            if (fineGrain) speedMultiplier *= Rules.fineGrainFactor;
            R.SET_SPEED_MULTIPLIER.invoke(core, speedMultiplier);
            R.SET_FINE_GRAIN.invoke(core, fineGrain);

            Level level = entity.level();
            Supplier<Object> projectile = () -> {
                try { return R.CREATE_PROJECTILE.invoke(null, type, level); }
                catch (Throwable t) { return fail("createProjectile", t, null); }
            };
            R.TRIGGER_FUZE.invoke(core, gunner, projectile);
            inventory.setChanged();
            return true;
        }
        catch (Throwable t) { return fail("fire", t, false); }
    }

    private static boolean consumeFineGrainPowder(SimpleContainer inventory) {
        for (ItemStack stack : inventory.items) {
            if (isFineGrainPowder(stack)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /* ---------------- ballistics ---------------- */

    /** Shared shot rules, read once from ShipBridge#getRules (the same balls as the ship cannons). */
    public static final class Rules {
        public static float cannonSpeed = 2.6F;
        public static float fineGrainFactor = 1.5F;
        public static float drag = 0.99F;
        public static float gravity = 0.06F;
        public static int fuzeMin = 10;
        public static int fuzeMax = 19;
        private static boolean loaded;

        private Rules() {}

        public static void load() {
            if (loaded || !SmallShipsBridge.isAvailable()) return;
            loaded = true;
            CompoundTag rules = SmallShipsBridge.getRules();
            if (rules.contains("CannonSpeed")) cannonSpeed = rules.getFloat("CannonSpeed");
            if (rules.contains("FineGrainFactor")) fineGrainFactor = rules.getFloat("FineGrainFactor");
            if (rules.contains("ShotDrag")) drag = rules.getFloat("ShotDrag");
            if (rules.contains("ShotGravity")) gravity = rules.getFloat("ShotGravity");
            if (rules.contains("FuzeMin")) fuzeMin = rules.getInt("FuzeMin");
            if (rules.contains("FuzeMax")) fuzeMax = rules.getInt("FuzeMax");
        }
    }

    /* ---------------- binding ---------------- */

    private static <T> T fail(String what, Throwable t, T fallback) {
        if (!errorLogged) {
            errorLogged = true;
            Main.LOGGER.error("[Recruits] Small Ships ground cannon: '{}' failed, the siege engineer cannot work the gun.", what, t);
        }
        return fallback;
    }

    /** Bound on first use - only ever touched when Small Ships is loaded. */
    private static final class R {
        static boolean AVAILABLE;
        static Class<?> CANNON_ENTITY;
        static Method UPDATE_CONTROLS, UPDATE_BARREL_CONTROLS, GET_CANNON, GET_DRIVER, GET_PASSENGER_IN_BARREL,
                GET_BARREL_UUID, SET_BARREL_UUID, GET_HEALTH, GET_MAX_HEALTH;
        static Method IS_COOLDOWN, IS_FUZING, SET_SPEED_MULTIPLIER, SET_FINE_GRAIN, TRIGGER_FUZE, GET_BARREL_END;
        static Method FIND_PREFERRED, CONSUME_PREFERRED, BALL_GET_TYPE, CREATE_PROJECTILE;
        static Field TYPE_SPEED;

        static {
            try {
                Class<?> cannonEntity = Class.forName("com.talhanation.smallships.world.entity.cannon.GroundCannonEntity");
                Class<?> cannon = Class.forName("com.talhanation.smallships.world.entity.cannon.Cannon");
                Class<?> shipCannon = Class.forName("com.talhanation.smallships.world.entity.cannon.ShipCannon");
                Class<?> ammoSelection = Class.forName("com.talhanation.smallships.world.item.CannonAmmoSelection");
                Class<?> ballItem = Class.forName("com.talhanation.smallships.world.item.CannonBallItem");
                Class<?> ballType = Class.forName("com.talhanation.smallships.world.item.CannonBallItem$Type");

                UPDATE_CONTROLS = cannonEntity.getMethod("updateControls", boolean.class, boolean.class, boolean.class, boolean.class, LivingEntity.class);
                UPDATE_BARREL_CONTROLS = cannonEntity.getMethod("updateBarrelControls", boolean.class, boolean.class, LivingEntity.class);
                GET_CANNON = cannonEntity.getMethod("getCannon");
                GET_DRIVER = cannonEntity.getMethod("getDriver");
                GET_PASSENGER_IN_BARREL = cannonEntity.getMethod("getPassengerInBarrel");
                GET_BARREL_UUID = cannonEntity.getMethod("getEntityInBarrelUUID");
                GET_HEALTH = cannonEntity.getMethod("getHealth");
                GET_MAX_HEALTH = cannonEntity.getMethod("getMaxHealth");

                IS_COOLDOWN = cannon.getMethod("isCooldown");
                IS_FUZING = cannon.getMethod("isFuzing");
                SET_SPEED_MULTIPLIER = cannon.getMethod("setSpeedMultiplier", float.class);
                SET_FINE_GRAIN = cannon.getMethod("setFineGrain", boolean.class);
                TRIGGER_FUZE = cannon.getMethod("triggerFuze", Entity.class, Supplier.class);
                GET_BARREL_END = cannon.getMethod("getBarrelEndPoint");

                FIND_PREFERRED = ammoSelection.getMethod("findPreferred", Entity.class, Iterable.class, Iterable.class);
                CONSUME_PREFERRED = ammoSelection.getMethod("consumePreferred", Entity.class, Iterable.class, Iterable.class);
                BALL_GET_TYPE = ballItem.getMethod("getType");
                CREATE_PROJECTILE = shipCannon.getMethod("createProjectile", ballType, Level.class);
                TYPE_SPEED = ballType.getField("speedMultiplier");

                CANNON_ENTITY = cannonEntity;
                AVAILABLE = true;

                // optional: protected, only needed when an engineer walked into the gun before he mounted it
                try {
                    Method setBarrelUUID = cannonEntity.getDeclaredMethod("setEntityInBarrelUUID", UUID.class);
                    setBarrelUUID.setAccessible(true);
                    SET_BARREL_UUID = setBarrelUUID;
                }
                catch (Throwable t) {
                    Main.LOGGER.warn("[Recruits] Small Ships ground cannon: setEntityInBarrelUUID not accessible, engineers that walk into a gun are set down beside it instead.");
                }
            }
            catch (Throwable t) {
                AVAILABLE = false;
                Main.LOGGER.warn("[Recruits] Small Ships ground cannon not found or changed, siege engineers cannot use it: {}", t.toString());
            }
        }
    }
}
