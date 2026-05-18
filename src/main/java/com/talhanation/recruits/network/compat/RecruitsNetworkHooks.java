package com.talhanation.recruits.network.compat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;

import java.util.function.Consumer;

public final class RecruitsNetworkHooks {
    private RecruitsNetworkHooks() {
    }

    public static void openScreen(ServerPlayer player, MenuProvider provider) {
        player.openMenu(provider);
    }

    public static void openScreen(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
        player.openMenu(provider, extraDataWriter);
    }

    public static Packet<ClientGamePacketListener> getEntitySpawningPacket(Entity entity) {
        return entity.getAddEntityPacket(null);
    }
}
