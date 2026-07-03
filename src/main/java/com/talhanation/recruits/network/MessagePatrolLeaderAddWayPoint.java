package com.talhanation.recruits.network;
import de.maxhenkel.corelib.net.NetUtils;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractLeaderEntity;
import com.talhanation.recruits.entities.CaptainEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Objects;
import java.util.UUID;

public class MessagePatrolLeaderAddWayPoint implements Message<MessagePatrolLeaderAddWayPoint> {
    public static final CustomPacketPayload.Type<MessagePatrolLeaderAddWayPoint> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagepatrolleaderaddwaypoint"));
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

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(100.0D),
                v -> v.getUUID().equals(this.worker) && v.isAlive()
        ).forEach((merchant) -> this.addWayPoint(new BlockPos(x, y, z), player, merchant));
    }

    private void addWayPoint(BlockPos pos, Player player, AbstractLeaderEntity leaderEntity) {
        BlockState state = leaderEntity.getCommandSenderWorld().getBlockState(pos);
        while (state.isAir()) {
            pos = pos.below();
            state = leaderEntity.getCommandSenderWorld().getBlockState(pos);
        }

        if (leaderEntity instanceof CaptainEntity captain && !state.is(Blocks.WATER)) {
            player.sendSystemMessage(TEXT_NOT_WATER_WAYPOINT(captain.getName().getString()));
        } else {
            leaderEntity.addWaypoint(pos);
            NetUtils.sendTo((ServerPlayer) player, new MessageToClientUpdateLeaderScreen(leaderEntity.WAYPOINTS, leaderEntity.WAYPOINT_ITEMS, leaderEntity.getArmySize()));
        }
    }

    public MessagePatrolLeaderAddWayPoint fromBytes(RegistryFriendlyByteBuf buf) {
        this.worker = buf.readUUID();
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.worker);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
    }

    private MutableComponent TEXT_NOT_WATER_WAYPOINT(String name) {
        return Component.translatable("chat.recruits.text.notWaterWaypoint", name);
    }

    @Override
    public CustomPacketPayload.Type<MessagePatrolLeaderAddWayPoint> type() {
        return TYPE;
    }
}
