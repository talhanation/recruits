package com.talhanation.recruits.client.events;

import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapCacheManager;
import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapStorageId;
import com.talhanation.recruits.config.RecruitsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class ClientPlayerEvents {
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        if (!RecruitsClientConfig.UpdateMapTiles.get()) return;

        updateMapTiles();
    }

    private void updateMapTiles() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.level.dimension() != Level.OVERWORLD) return;

        WorldMapCacheManager.getInstance().updateCurrentTile();
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            WorldMapCacheManager.getInstance().initialize((Level) event.getLevel());
        }
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            WorldMapCacheManager.getInstance().close();
            WorldMapStorageId.clearServerWorldId();
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level && level.isClientSide) {
            WorldMapCacheManager.getInstance().onChunkLoaded(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level && level.isClientSide) {
            WorldMapCacheManager.getInstance().onChunkUnloaded(level, event.getChunk().getPos());
        }
    }
}
