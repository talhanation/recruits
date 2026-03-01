package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.UUID;

public class MessageSendTreaty implements Message<MessageSendTreaty> {

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
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
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
    public MessageSendTreaty fromBytes(FriendlyByteBuf buf) {
        this.recruit = buf.readUUID();
        this.start = buf.readBoolean();
        this.durationHours = buf.readInt();
        this.targetPlayerNbt = buf.readNbt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(recruit);
        buf.writeBoolean(start);
        buf.writeInt(durationHours);
        buf.writeNbt(targetPlayerNbt);
    }
}
