package com.talhanation.recruits.governor;

import com.talhanation.recruits.ClaimEvent;
import com.talhanation.recruits.SiegeEvent;
import com.talhanation.recruits.api.governor.GovernorModules;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Lifecycle of the Governor data and reactions to claim events.
 */
public class GovernorEvents {

    private MinecraftServer server;

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        GovernorManager manager = new GovernorManager();
        manager.load(server.overworld());
        GovernorManager.setInstance(manager);
    }

    /** Tickets of the daily term that were left from a crash. Here all levels are loaded. */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        GovernorManager manager = GovernorManager.get();
        if (manager != null) GovernorDayLoader.clearStaleTickets(event.getServer().overworld(), manager);
    }

    /** Daily term of office of Governors whose claim is not loaded. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && server != null) GovernorDayLoader.tick(server);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (server != null) GovernorDayLoader.releaseAll(server.overworld());
        GovernorManager manager = GovernorManager.get();
        if (manager != null && server != null) {
            manager.save(server.overworld());
            manager.detach(server.overworld());
        }
        GovernorManager.setInstance(null);
    }

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        GovernorManager manager = GovernorManager.get();
        if (manager != null) manager.save(level);
    }

    /** The claim was conquered: its Governor stops ruling right away, posts and treasury stay as loot. */
    @SubscribeEvent
    public void onSiegeSuccess(SiegeEvent.Success event) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null) return;
        ClaimGovernorData data = manager.getData(event.getClaim().getUUID());
        if (data == null) return;

        // addon modules first, while the old owner is still known (e.g. remove his Governor trades and orders)
        UUID oldOwner = data.getGovernorOwner();
        UUID newOwner = GovernorManager.getClaimOwner(event.getClaim());
        GovernorModules.forEach("onOwnerChanged", module -> module.onOwnerChanged(event.getLevel(), event.getClaim(), oldOwner, newOwner));

        if (data.hasGovernor()) {
            GovernorManager.notifyPlayer(event.getLevel(), data.getGovernorOwner(), Component.translatable("chat.recruits.governor.stopped_ruling",
                    data.getGovernorName(), event.getClaim().getName(),
                    Component.translatable("gui.recruits.governor.status.conquered")));
            data.clearGovernor();
        }
        for (GovernorPost post : data.getPosts()) post.setAssigned(null);
        data.patrols.clear();
        data.siegeNotified = false;
    }

    /** Daily reports the owner missed while he was offline. */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        GovernorManager manager = GovernorManager.get();
        if (manager != null && event.getEntity() instanceof ServerPlayer player) manager.deliverPendingReports(player);
    }

    @SubscribeEvent
    public void onClaimRemoved(ClaimEvent.Removed event) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null) return;
        ClaimGovernorData data = manager.getData(event.getClaim().getUUID());
        if (data != null && data.hasGovernor()) {
            GovernorManager.notifyPlayer(event.getLevel(), data.getGovernorOwner(), Component.translatable("chat.recruits.governor.stopped_ruling",
                    data.getGovernorName(), event.getClaim().getName(),
                    Component.translatable("gui.recruits.governor.status.no_claim")));
        }
        manager.remove(event.getClaim().getUUID());
    }
}
