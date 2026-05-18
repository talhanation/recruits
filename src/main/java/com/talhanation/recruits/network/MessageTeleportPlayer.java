package com.talhanation.recruits.network;

import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

public class MessageTeleportPlayer implements RecruitsMessage<MessageTeleportPlayer> {

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
    public void executeServerSide(RecruitsNetworkContext context) {
        Player player = context.getSender();

        if(player == null)return;

        BlockPos corrected = player.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
        if(corrected.getY() < -65){
            corrected.offset(0, 164, 0);
        }
        player.teleportTo(corrected.getX(), corrected.getY() + 2, corrected.getZ());
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

