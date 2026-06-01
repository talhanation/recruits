package com.talhanation.recruits.network;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageTeleportPlayer implements Message<MessageTeleportPlayer> {

    public BlockPos pos;
    public MessageTeleportPlayer() {
    }

    public MessageTeleportPlayer(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();

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
    public MessageTeleportPlayer fromBytes(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }
}

