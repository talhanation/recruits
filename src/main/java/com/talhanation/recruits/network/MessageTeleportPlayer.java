package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.api.distmarker.Dist;
public class MessageTeleportPlayer implements Message<MessageTeleportPlayer> {

    public static final CustomPacketPayload.Type<MessageTeleportPlayer> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageteleportplayer"));
    public BlockPos pos;
    public MessageTeleportPlayer() {
    }

    public MessageTeleportPlayer(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());

        if (player == null || this.pos == null) return;
        if (!player.isCreative() || !player.hasPermissions(2)) return;

        ServerLevel level = player.serverLevel();
        BlockPos corrected = resolveTeleportPos(level, this.pos);
        player.teleportTo(corrected.getX() + 0.5D, corrected.getY(), corrected.getZ() + 0.5D);
    }

    private static BlockPos resolveTeleportPos(ServerLevel level, BlockPos pos) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        y = Math.max(y, level.getMinBuildHeight());
        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    @Override
    public MessageTeleportPlayer fromBytes(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public CustomPacketPayload.Type<MessageTeleportPlayer> type() {
        return TYPE;
    }
}

