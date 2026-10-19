package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * Client → Server: the selected group should empty its inventory into a container
 * ("anti-upkeep"). Target is either a block position or an entity.
 */
public class MessageDepositInventory implements Message<MessageDepositInventory> {

    /** Same reach as the command screen's ray; anything further is rejected. */
    private static final double MAX_TARGET_DISTANCE_SQR = 128.0D * 128.0D;

    private UUID group;
    @Nullable private UUID entity;
    @Nullable private BlockPos pos;

    public MessageDepositInventory() {
    }

    public MessageDepositInventory(UUID group, @Nullable UUID entity, @Nullable BlockPos pos) {
        this.group = group;
        this.entity = entity;
        this.pos = pos;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        if (entity == null && pos == null) return;
        if (pos != null && Vec3.atCenterOf(pos).distanceToSqr(player.position()) > MAX_TARGET_DISTANCE_SQR) return;

        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100)
        ).forEach(recruit -> CommandEvents.onDepositCommand(player.getUUID(), recruit, group, entity, pos));
    }

    public MessageDepositInventory fromBytes(FriendlyByteBuf buf) {
        this.group = buf.readUUID();
        this.entity = buf.readBoolean() ? buf.readUUID() : null;
        this.pos = buf.readBoolean() ? buf.readBlockPos() : null;
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.group);
        buf.writeBoolean(this.entity != null);
        if (this.entity != null) buf.writeUUID(this.entity);
        buf.writeBoolean(this.pos != null);
        if (this.pos != null) buf.writeBlockPos(this.pos);
    }
}
