package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

/**
 * NeoForge replacement for ForgeChunkManager.forceChunk(level, MOD_ID, entity, ...):
 * entity owned, non ticking chunk tickets of chunk loading companions.
 */
public class ModChunkTickets {
    public static final TicketController CHUNK_LOADER = new TicketController(ResourceLocation.fromNamespaceAndPath(Main.MOD_ID, "chunk_loader"));

    public static void register(RegisterTicketControllersEvent event) {
        event.register(CHUNK_LOADER);
    }
}
