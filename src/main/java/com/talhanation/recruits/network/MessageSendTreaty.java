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
import java.util.Objects;
import java.util.UUID;

public class MessageSendTreaty implements Message<MessageSendTreaty> {

    public static final CustomPacketPayload.Type<MessageSendTreaty> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagesendtreaty"));
    private UUID recruit;
    private boolean start;
    private CompoundTag targetPlayerNbt;
    private int durationHours;

    public MessageSendTreaty() {
    }

    public MessageSendTreaty(UUID recruit, RecruitsPlayerInfo targetPlayer, int durationHours, boolean start) {
        this.recruit = recruit;
        this.durationHours = durationHours;
        this.start = start;

        if (targetPlayer != null) {
            this.targetPlayerNbt = targetPlayer.toNBT();
        } else {
            this.targetPlayerNbt = new CompoundTag();
        }
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        player.getCommandSenderWorld().getEntitiesOfClass(
                MessengerEntity.class,
                player.getBoundingBox().inflate(16D),
                (messenger) -> messenger.getUUID().equals(this.recruit)
        ).forEach((messenger) -> {
            if (messenger.getUUID().equals(this.recruit)) {

                if (!this.targetPlayerNbt.isEmpty()) {
                    messenger.setTargetPlayerInfo(RecruitsPlayerInfo.getFromNBT(this.targetPlayerNbt));
                }

                if (start) {
                    messenger.setTreatyDurationHours(this.durationHours);
                    messenger.setIsTreatyMessenger(true);
                    messenger.start();
                }
            }
        });
    }

    @Override
    public MessageSendTreaty fromBytes(RegistryFriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.start = buf.readBoolean();
        this.durationHours = buf.readInt();
        this.targetPlayerNbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeBoolean(start);
        buf.writeInt(durationHours);
        buf.writeNbt(targetPlayerNbt);
    }

    @Override
    public CustomPacketPayload.Type<MessageSendTreaty> type() {
        return TYPE;
    }
}
