package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.CommandEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import java.util.UUID;

public class MessageCommandScreen implements Message<MessageCommandScreen> {

    public static final CustomPacketPayload.Type<MessageCommandScreen> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagecommandscreen"));
    private UUID uuid;

    public MessageCommandScreen() {
        this.uuid = new UUID(0, 0);
    }

    public MessageCommandScreen(Player player) {
        this.uuid = player.getUUID();
    }

    @Override
    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    @Override
    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = ((ServerPlayer) context.player());
        if (!player.getUUID().equals(uuid)) {
            return;
        }
        CommandEvents.openCommandScreen(player);
    }

    @Override
    public MessageCommandScreen fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        return this;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
    }


    @Override
    public CustomPacketPayload.Type<MessageCommandScreen> type() {
        return TYPE;
    }
}