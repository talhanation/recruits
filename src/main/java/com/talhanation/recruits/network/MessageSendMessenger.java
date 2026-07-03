package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MessageSendMessenger implements Message<MessageSendMessenger> {

    public static final CustomPacketPayload.Type<MessageSendMessenger> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagesendmessenger"));
    private UUID recruit;
    private boolean start;
    private CompoundTag nbt;
    private String message;

    public MessageSendMessenger() {
    }

    public MessageSendMessenger(UUID recruit, RecruitsPlayerInfo targetPlayer, String message, boolean start) {
        this.recruit = recruit;
        this.message = message;
        this.start = start;

        if(targetPlayer != null){
            this.nbt = targetPlayer.toNBT();
        }
        else {
            this.nbt = new CompoundTag();
        }

    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                MessengerEntity.class,
                player.getBoundingBox().inflate(16D),
                (messenger) -> messenger.getUUID().equals(this.recruit)
        ).forEach((messenger) -> {
            if (messenger.getUUID().equals(this.recruit)){

                messenger.setMessage(this.message);

                if(!this.nbt.isEmpty()){
                    messenger.setTargetPlayerInfo(RecruitsPlayerInfo.getFromNBT(this.nbt));
                }

                if(start){
                    messenger.setIsTreatyMessenger(false);
                    messenger.start();
                }
            }
        });
    }

    public MessageSendMessenger fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.start = buf.readBoolean();
        this.message = buf.readUtf();
        this.nbt = buf.readNbt();

        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeBoolean(start);
        buf.writeUtf(message);
        buf.writeNbt(nbt);
    }

    @Override
    public CustomPacketPayload.Type<MessageSendMessenger> type() {
        return TYPE;
    }
}
