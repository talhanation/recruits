package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;
import java.util.UUID;

public class MessageRecruitGui implements Message<MessageRecruitGui> {

    public static final CustomPacketPayload.Type<MessageRecruitGui> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagerecruitgui"));
    private UUID uuid;
    private UUID recruit;


    public MessageRecruitGui() {
        this.uuid = new UUID(0, 0);
    }

    public MessageRecruitGui(Player player, UUID recruit) {
        this.uuid = player.getUUID();
        this.recruit = recruit;
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        if (!player.getUUID().equals(uuid)) {
            return;
        }

        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(16.0D),
                v -> v.getUUID().equals(this.recruit) && v.isAlive()
        ).forEach(recruit -> recruit.openGUI(player));
    }

    @Override
    public MessageRecruitGui fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.recruit = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeUUID(recruit);
    }

    @Override
    public CustomPacketPayload.Type<MessageRecruitGui> type() {
        return TYPE;
    }
}