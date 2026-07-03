package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageAssignRecruitToPlayer implements Message<MessageAssignRecruitToPlayer> {

    public static final CustomPacketPayload.Type<MessageAssignRecruitToPlayer> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageassignrecruittoplayer"));
    private UUID recruit;
    private UUID newOwner;
    public MessageAssignRecruitToPlayer() {
    }

    public MessageAssignRecruitToPlayer(UUID recruit, UUID newOwner) {
        this.recruit = recruit;
        this.newOwner = newOwner;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer serverPlayer = ((ServerPlayer) context.player());
        List<AbstractRecruitEntity> list = Objects.requireNonNull(((ServerPlayer) context.player())).getCommandSenderWorld().getEntitiesOfClass(AbstractRecruitEntity.class, ((ServerPlayer) context.player()).getBoundingBox().inflate(64.0D));
        ServerLevel serverLevel = (ServerLevel) serverPlayer.getCommandSenderWorld();

        for (AbstractRecruitEntity recruit : list) {
            if(recruit.getUUID().equals(this.recruit)){
                recruit.assignToPlayer(newOwner, null);
                FactionEvents.notifyPlayer(serverLevel, new RecruitsPlayerInfo(newOwner, ""), 0, serverPlayer.getName().getString());
                break;
            }
        }
    }

    public MessageAssignRecruitToPlayer fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.newOwner = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeUUID(this.newOwner);
    }

    @Override
    public CustomPacketPayload.Type<MessageAssignRecruitToPlayer> type() {
        return TYPE;
    }
}