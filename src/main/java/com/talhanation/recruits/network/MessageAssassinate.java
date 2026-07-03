package com.talhanation.recruits.network;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.RegistryFriendlyByteBuf;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.api.distmarker.Dist;
import java.util.Objects;

public class MessageAssassinate implements Message<MessageAssassinate> {

    public static final CustomPacketPayload.Type<MessageAssassinate> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("recruits", "messageassassinate"));
    //private UUID target;
    private int count;
    private int costs;
    private String name;

    public MessageAssassinate(){
    }

    public MessageAssassinate(String name, int count, int costs) {
        //this.target = null;
        this.count = count;
        this.costs = costs;
        this.name = name;
    }

    public PacketFlow getExecutingSide() {
        return PacketFlow.SERVERBOUND;
    }

    public void executeServerSide(IPayloadContext context) {
        ServerPlayer player = Objects.requireNonNull(((ServerPlayer) context.player()));
        ServerLevel world = player.serverLevel();
        MinecraftServer server = world.getServer();
        PlayerList list = server.getPlayerList();
        ServerPlayer targetPlayer = list.getPlayerByName(name);
        if (targetPlayer != null) {
            player.sendSystemMessage(Component.literal("Successfully found the Target"));
            //this.target = targetPlayer.getUUID();
            //AssassinEvents.createAssassin(name, count, world);
            //AssassinEvents.doPayment(player, costs);
        }
        else {
            player.sendSystemMessage(Component.literal("Could not found the Target"));
            //player.sendMessage(new StringTextComponent(": " + this.name), player.getUUID());
        }
    }

    public MessageAssassinate fromBytes(RegistryFriendlyByteBuf buf) {
        //this.target = buf.readUUID();
        this.count = buf.readInt();
        this.costs = buf.readInt();
        this.name = buf.readUtf();
        return this;
    }

    public void toBytes(RegistryFriendlyByteBuf buf) {
        //buf.writeUUID(this.target);
        buf.writeInt(this.count);
        buf.writeInt(this.costs);
        buf.writeUtf(this.name);
    }


    @Override
    public CustomPacketPayload.Type<MessageAssassinate> type() {
        return TYPE;
    }
}
