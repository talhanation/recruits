package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageProtectEntity implements Message<MessageProtectEntity> {

    public static final CustomPacketPayload.Type<MessageProtectEntity> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageprotectentity"));
    private UUID uuid;
    private UUID target;
    private UUID group;

    public MessageProtectEntity(){

    }

    public MessageProtectEntity(UUID uuid, UUID target, UUID group) {
        this.uuid = uuid;
        this.target = target;
        this.group = group;

    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(100)
        ).forEach((recruit) -> CommandEvents.onProtectButton(uuid, recruit, target, group));
    }
    public MessageProtectEntity fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.target = buf.readUUID();
        this.group = buf.readUUID();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(target);
        buf.writeUUID(group);
    }


    @Override
    public CustomPacketPayload.Type<MessageProtectEntity> type() {
        return TYPE;
    }
}