package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.CaptainEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.Tags;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderAddWayPoint implements Message<MessagePatrolLeaderAddWayPoint> {
    private static final double MAX_WAYPOINT_DISTANCE = 128.0D;
    private UUID worker;
    private int x;
    private int y;
    private int z;

    public MessagePatrolLeaderAddWayPoint() {
    }

    public MessagePatrolLeaderAddWayPoint(UUID recruit, int x, int y, int z) {
        this.worker = recruit;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(100.0D),
                v -> v.getUUID().equals(this.worker) && NetworkSecurity.canControl(player, v)
        ).forEach((merchant) -> this.addWayPoint(new BlockPos(x, y, z), player, merchant));
    }

    private void addWayPoint(BlockPos pos, Player player, AbstractLeaderEntity leaderEntity) {
        Level level = leaderEntity.getCommandSenderWorld();
        // Waypoints are picked by looking at a block: must be near the player and in a loaded chunk.
        // Before: no limit + "while(isAir) below()" never ended under the world (VOID_AIR) -> server hang.
        if (player.distanceToSqr(pos.getX() + 0.5D, player.getY(), pos.getZ() + 0.5D) > MAX_WAYPOINT_DISTANCE * MAX_WAYPOINT_DISTANCE) return;
        if (!level.isLoaded(pos)) return;
        pos = new BlockPos(pos.getX(), Math.min(pos.getY(), level.getMaxBuildHeight()), pos.getZ());

        BlockState state = level.getBlockState(pos);
        while (state.isAir() && pos.getY() > level.getMinBuildHeight()) {
            pos = pos.below();
            state = level.getBlockState(pos);
        }
        if (state.isAir()) return;

        if (leaderEntity instanceof CaptainEntity captain && !state.is(Blocks.WATER)) {
            player.sendSystemMessage(TEXT_NOT_WATER_WAYPOINT(captain.getName().getString()));
        } else {
            leaderEntity.addWaypoint(pos);
            Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new MessageToClientUpdateLeaderScreen(leaderEntity.WAYPOINTS, leaderEntity.WAYPOINT_ITEMS, leaderEntity.getArmySize()));
        }
    }

    public MessagePatrolLeaderAddWayPoint fromBytes(FriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
    }

    private MutableComponent TEXT_NOT_WATER_WAYPOINT(String name) {
        return Component.translatable("chat.recruits.text.notWaterWaypoint", name);
    }
}
