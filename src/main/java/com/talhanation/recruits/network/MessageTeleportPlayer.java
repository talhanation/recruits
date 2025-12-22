package com.talhanation.recruits.network;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
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

