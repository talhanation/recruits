package com.talhanation.recruits.client.events;

import com.talhanation.recruits.client.gui.worldmap.ChunkTileManager;
import com.talhanation.recruits.client.gui.worldmap.WorldMapScreen;
import com.talhanation.recruits.config.RecruitsClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class ClientPlayerEvents {
    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {

        if (!(Minecraft.getInstance().screen instanceof WorldMapScreen)) return;
        if (!RecruitsClientConfig.UpdateMapTiles.get()) return;

        updateMapTiles();
    }

    private void updateMapTiles() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.level.dimension() != Level.OVERWORLD) return;

        ChunkTileManager.getInstance().updateCurrentTile();
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