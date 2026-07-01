package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.talhanation.recruits.FactionEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import java.util.UUID;


public class MessageDoPayment implements Message<MessageDoPayment> {

    public static final CustomPacketPayload.Type<MessageDoPayment> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messagedopayment"));
    private int amount;
    private UUID uuid;
    public MessageDoPayment(){

    }

    public MessageDoPayment(UUID uuid, int amount) {
        this.amount = amount;
        this.uuid = uuid;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context){
        ServerPlayer serverPlayer = ((ServerPlayer) context.player());
        if(serverPlayer == null) return;

        if(!serverPlayer.getUUID().equals(uuid)) return;
        if(this.amount <= 0) return;

        if(serverPlayer.isCreative() && serverPlayer.hasPermissions(2)){
            return;
        }
        if(!FactionEvents.playerHasEnoughEmeralds(serverPlayer, this.amount)) return;

        FactionEvents.doPayment(serverPlayer, this.amount);
    }
    public MessageDoPayment fromBytes(RegistryFriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.amount = buf.readInt();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(uuid);
        buf.writeInt(amount);
    }

    @Override
    public CustomPacketPayload.Type<MessageDoPayment> type() {
        return TYPE;
    }
}
