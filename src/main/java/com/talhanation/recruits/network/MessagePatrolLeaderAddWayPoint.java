package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class MessagePatrolLeaderAddWayPoint implements Message<MessagePatrolLeaderAddWayPoint> {
    private UUID worker;

    public MessagePatrolLeaderAddWayPoint() {
    }

    public MessagePatrolLeaderAddWayPoint(UUID recruit) {
        this.worker = recruit;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {

        ServerPlayer player = context.getSender();
        player.level.getEntitiesOfClass(AbstractLeaderEntity.class, player.getBoundingBox()
                        .inflate(100.0D), v -> v
                        .getUUID()
                        .equals(this.worker))
                .stream()
                .filter(AbstractLeaderEntity::isAlive)
                .findAny()
                .ifPresent(merchant -> this.addWayPoint(player, merchant));
    }

    private void addWayPoint(ServerPlayer player, AbstractLeaderEntity leaderEntity){
        BlockPos pos;
        if(leaderEntity.isInWater()) pos = leaderEntity.getOnPos().above();
        else pos = leaderEntity.getOnPos();
        //leaderEntity.tellPlayer(player, Component.literal("Pos: " + pos + " was added."));
        BlockState state = leaderEntity.getCommandSenderWorld().getBlockState(pos);
        if(state.isAir()) pos = pos.below();

        leaderEntity.addWaypoint(pos);

        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateLeaderScreen(leaderEntity.WAYPOINTS, leaderEntity.WAYPOINT_ITEMS));
    }

    public MessagePatrolLeaderAddWayPoint fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
    }

}
