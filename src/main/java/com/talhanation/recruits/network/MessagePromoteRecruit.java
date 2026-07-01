package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.server.level.ServerPlayer;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessagePromoteRecruit implements Message<MessagePromoteRecruit> {

    public static final CustomPacketPayload.Type<MessagePromoteRecruit> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagepromoterecruit"));
    private UUID recruit;
    private int profession;
    private String name;
    public MessagePromoteRecruit(){
    }

    public MessagePromoteRecruit(UUID recruit, int profession, String name) {
        this.recruit = recruit;
        this.profession = profession;
        this.name = name;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        Objects.requireNonNull(((ServerPlayer) context.player())).getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class,
                ((ServerPlayer) context.player()).getBoundingBox().inflate(16D),
                livingEntity -> livingEntity.getUUID().equals(this.recruit)
        ).forEach((recruit) -> RecruitEvents.promoteRecruit(recruit, profession, name, ((ServerPlayer) context.player())));

    }
    public MessagePromoteRecruit fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.profession = buf.readInt();
        this.name = buf.readUtf();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeInt(profession);
        buf.writeUtf(name);
    }


    @Override
    public CustomPacketPayload.Type<MessagePromoteRecruit> type() {
        return TYPE;
    }
}