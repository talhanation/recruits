package com.talhanation.recruits;

import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsVillageManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

/**
 * Dorf-Register, Noble-Wahl, Ruf und Aufträge (claude/noble-villager-konzept.md).
 */
public class VillageEvents {

    public static RecruitsVillageManager villageManager;
    private static MinecraftServer server;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        villageManager = new RecruitsVillageManager();
        villageManager.load(server.overworld());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (villageManager != null && server != null) villageManager.save(server.overworld());
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (villageManager != null && server != null) villageManager.save(server.overworld());
    }

    /** Wahl, Aufträge und Auflösen – alle 100 Ticks pro Dimension. */
    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) return;
        if (villageManager == null || !(event.level instanceof ServerLevel level)) return;
        if (level.getGameTime() % RecruitsVillageManager.TICK_INTERVAL != 0) return;

        villageManager.tick(level);
    }

    /** Einwohner erfassen: alle 200 Ticks pro Villager, gestaffelt über die Entity-ID. */
    @SubscribeEvent
    public void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (villageManager == null) return;
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (!(villager.level() instanceof ServerLevel level)) return;
        if (Math.floorMod(level.getGameTime() + villager.getId(), RecruitsVillageManager.VILLAGER_CHECK_INTERVAL) != 0) return;

        villageManager.updateVillager(level, villager);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDamage(LivingDamageEvent event) {
        if (villageManager == null || event.isCanceled() || event.getAmount() <= 0) return;
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (!(victim instanceof Villager) && !(victim instanceof VillagerNobleEntity)) return;

        villageManager.onVictimHurt(victim, event.getSource());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDeath(LivingDeathEvent event) {
        if (villageManager == null || event.isCanceled()) return;
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;

        villageManager.onLivingDeath(level, victim, event.getSource());
    }

    /** Noble getötet oder entfernt: aus dem Dorf austragen und Bett freigeben. Entladen zählt nicht. */
    @SubscribeEvent
    public void onEntityLeave(EntityLeaveLevelEvent event) {
        if (villageManager == null) return;
        Entity entity = event.getEntity();
        if (!(entity instanceof VillagerNobleEntity noble)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Entity.RemovalReason reason = noble.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) return;

        villageManager.onNobleRemoved(level, noble, reason == Entity.RemovalReason.KILLED);
    }
}
