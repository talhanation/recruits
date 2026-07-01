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
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public class MessageAggro implements Message<MessageAggro> {

    public static final CustomPacketPayload.Type<MessageAggro> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageaggro"));
    private UUID player;
    private UUID recruit;
    private int state;
    private UUID group;
    private boolean fromGui;


    public MessageAggro() {
    }

    public MessageAggro(UUID player, int state, UUID group) {
        this.player = player;
        this.state = state;
        this.group = group;
        this.fromGui = false;
        this.recruit = null;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));

        double boundBoxInflateModifier = 16.0D;
        if(!fromGui) {
            boundBoxInflateModifier = 100.0D;
        }


        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                player.getBoundingBox().inflate(boundBoxInflateModifier)
        ).forEach((recruit) -> {
            if (fromGui && !recruit.getUUID().equals(this.recruit)) {
                return;
            }

            CommandEvents.onAggroCommand(this.player, recruit, this.state, group, fromGui);
        });
    }

    public MessageAggro fromBytes(RegistryFriendlyByteBuf buf) {
        this.player = buf.readUUID();
        this.state = buf.readInt();
        this.group = buf.readUUID();
        if (this.recruit != null) this.recruit = buf.readUUID();
        this.fromGui = buf.readBoolean();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.player);
        buf.writeInt(this.state);
        buf.writeUUID(this.group);
        buf.writeBoolean(this.fromGui);
        if (this.recruit != null) buf.writeUUID(this.recruit);
    }

    @Override
    public CustomPacketPayload.Type<MessageAggro> type() {
        return TYPE;
    }
}