package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.client.gui.MessengerTreatyAnswerScreen;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import java.util.UUID;

public class MessageToClientOpenTreatyAnswerScreen implements Message<MessageToClientOpenTreatyAnswerScreen> {

    public static final CustomPacketPayload.Type<MessageToClientOpenTreatyAnswerScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagetoclientopentreatyanswerscreen"));
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
    public PacketFlow getExecutingSide() {
        return PacketFlow.CLIENTBOUND;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void executeClientSide(IPayloadContext context) {
        Player player = Minecraft.getInstance().player;
        player.getCommandSenderWorld().getEntitiesOfClass(MessengerEntity.class, player.getBoundingBox()
                        .inflate(16.0D), v -> v.getUUID().equals(this.recruitUUID))
                .stream()
                .filter(Entity::isAlive)
                .findAny()
                .ifPresent(messenger -> Minecraft.getInstance().setScreen(new MessengerTreatyAnswerScreen(messenger, player, durationHours, RecruitsPlayerInfo.getFromNBT(nbt))));
    }

    @Override
    public MessageToClientOpenTreatyAnswerScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.durationHours = buf.readInt();
        this.nbt = buf.readNbt();
        this.recruitUUID = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeInt(durationHours);
        buf.writeNbt(nbt);
        buf.writeUUID(recruitUUID);
    }

    @Override
    public CustomPacketPayload.Type<MessageToClientOpenTreatyAnswerScreen> type() {
        return TYPE;
    }
}
