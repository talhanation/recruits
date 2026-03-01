package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.MessengerTreatyAnswerScreen;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageToClientOpenTreatyAnswerScreen implements Message<MessageToClientOpenTreatyAnswerScreen> {

    public int durationHours;
    public CompoundTag nbt;
    public UUID recruitUUID;

    public MessageToClientOpenTreatyAnswerScreen() {
    }

    public MessageToClientOpenTreatyAnswerScreen(MessengerEntity messenger, int durationHours, RecruitsPlayerInfo playerInfo) {
        this.durationHours = durationHours;
        this.nbt = playerInfo.toNBT();
        this.recruitUUID = messenger.getUUID();
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(NetworkEvent.Context context) {
        Player player = Minecraft.getInstance().player;
        player.getCommandSenderWorld().getEntitiesOfClass(MessengerEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v.getUUID().equals(this.recruitUUID))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(messenger -> Minecraft.getInstance().setScreen(new MessengerTreatyAnswerScreen(messenger, player, durationHours, RecruitsPlayerInfo.getFromNBT(nbt))));
    }

    @Override
    public MessageToClientOpenTreatyAnswerScreen fromBytes(FriendlyByteBuf buf) {
        this.durationHours = buf.readInt();
        this.nbt = buf.readNbt();
        this.recruitUUID = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(durationHours);
        buf.writeNbt(nbt);
        buf.writeUUID(recruitUUID);
    }
}
