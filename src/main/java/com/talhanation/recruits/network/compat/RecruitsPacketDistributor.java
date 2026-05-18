package com.talhanation.recruits.network.compat;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public final class RecruitsPacketDistributor {
    public static final PlayerTarget PLAYER = new PlayerTarget();
    public static final AllTarget ALL = new AllTarget();

    private RecruitsPacketDistributor() {
    }

    public sealed interface Target permits PlayerPacketTarget, AllPacketTarget {
        void send(CustomPacketPayload payload);
    }

    public record PlayerPacketTarget(ServerPlayer player) implements Target {
        @Override
        public void send(CustomPacketPayload payload) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public record AllPacketTarget() implements Target {
        @Override
        public void send(CustomPacketPayload payload) {
            net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(payload);
        }
    }

    public static final class PlayerTarget {
        public Target with(Supplier<ServerPlayer> player) {
            return new PlayerPacketTarget(player.get());
        }
    }

    public static final class AllTarget {
        public Target noArg() {
            return new AllPacketTarget();
        }
    }
}
