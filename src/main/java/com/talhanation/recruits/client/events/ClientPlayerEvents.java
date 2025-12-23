package com.talhanation.recruits.client.events;

import com.talhanation.recruits.client.gui.worldmap.ChunkTileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientPlayerEvents {
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        updateMapTiles();
    }

    private void updateMapTiles() {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
            ChunkTileManager tileManager = ChunkTileManager.getInstance();
            tileManager.updateCurrentTile();
        }
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            ChunkTileManager.getInstance().initialize((Level) event.getLevel());
        }
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ChunkTileManager.getInstance().close();
        }
    }
}