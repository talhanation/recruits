package com.talhanation.recruits.compat.corpse;

import com.talhanation.recruits.Main;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Dependency-free entry point for lootable corpses of the Corpse and RpgZ mods.
 * Both are only seen as a {@link Container}; Corpse-specific classes are only touched
 * through {@link CorpseLootAccess} when the Corpse mod is loaded, RpgZ is accessed by reflection
 * so Recruits compiles and runs without it.
 */
public final class LootableCorpses {

    private static final String RPGZ_INVENTORY_ACCESS = "net.rpgz.access.IInventoryAccess";
    /** RpgZ turns a dead mob into a lootable corpse after 20 death ticks. */
    private static final int RPGZ_CORPSE_DEATH_TIME = 20;

    private static boolean rpgzResolved;
    @Nullable private static Class<?> rpgzAccessClass;
    @Nullable private static Method rpgzGetDropsInventory;

    private LootableCorpses() {
    }

    public static boolean isAnyCorpseModLoaded() {
        return Main.isCorpseLoaded || Main.isRPGZLoaded;
    }

    public static List<Entity> findCorpses(Level level, AABB area) {
        return level.getEntitiesOfClass(Entity.class, area, LootableCorpses::isCorpse);
    }

    public static boolean isCorpse(Entity entity) {
        if (entity == null || entity.isRemoved()) return false;
        if (Main.isCorpseLoaded && CorpseLootAccess.isCorpseEntity(entity)) return true;
        return Main.isRPGZLoaded && isRpgzCorpse(entity);
    }

    /** The corpse's items as a container, or null when the entity is no (lootable) corpse. */
    @Nullable
    public static Container getContainer(Entity entity) {
        if (entity == null || entity.isRemoved()) return null;
        if (Main.isCorpseLoaded && CorpseLootAccess.isCorpseEntity(entity)) {
            return CorpseLootAccess.getContainer(entity);
        }
        if (Main.isRPGZLoaded && isRpgzCorpse(entity)) {
            return getRpgzContainer(entity);
        }
        return null;
    }

    /** Lets the corpse update its looks after an item was taken (Corpse renders worn equipment). */
    public static void onItemTaken(@Nullable Entity entity, ItemStack taken) {
        if (entity != null && Main.isCorpseLoaded && CorpseLootAccess.isCorpseEntity(entity)) {
            CorpseLootAccess.onItemTaken(entity, taken);
        }
    }

    // ---- RpgZ ---------------------------------------------------------------

    private static boolean isRpgzCorpse(Entity entity) {
        if (!(entity instanceof Mob mob) || !mob.isDeadOrDying()) return false;
        if (((LivingEntity) mob).deathTime <= RPGZ_CORPSE_DEATH_TIME) return false;
        resolveRpgz();
        return rpgzAccessClass != null && rpgzAccessClass.isInstance(entity);
    }

    @Nullable
    private static Container getRpgzContainer(Entity entity) {
        resolveRpgz();
        if (rpgzGetDropsInventory == null) return null;
        try {
            Object inventory = rpgzGetDropsInventory.invoke(entity);
            return inventory instanceof Container container ? container : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static synchronized void resolveRpgz() {
        if (rpgzResolved) return;
        rpgzResolved = true;
        try {
            rpgzAccessClass = Class.forName(RPGZ_INVENTORY_ACCESS);
            rpgzGetDropsInventory = rpgzAccessClass.getMethod("getDropsInventory");
        } catch (ReflectiveOperationException | LinkageError e) {
            Main.LOGGER.warn("RpgZ is loaded but its corpse inventory could not be accessed, field looting of RpgZ corpses is disabled.");
            rpgzAccessClass = null;
            rpgzGetDropsInventory = null;
        }
    }
}
