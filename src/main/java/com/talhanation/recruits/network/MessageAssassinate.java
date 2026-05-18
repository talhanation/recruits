package com.talhanation.recruits.network;

import com.talhanation.recruits.network.compat.RecruitsMessage;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.network.protocol.PacketFlow;
import com.talhanation.recruits.network.compat.RecruitsNetworkContext;

import java.util.Objects;

public class MessageAssassinate implements RecruitsMessage<MessageAssassinate> {

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

    public void executeServerSide(RecruitsNetworkContext context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
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

    public MessageAssassinate fromBytes(FriendlyByteBuf buf) {
        //this.target = buf.readUUID();
        this.count = buf.readInt();
        this.costs = buf.readInt();
        this.name = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        //buf.writeUUID(this.target);
        buf.writeInt(this.count);
        buf.writeInt(this.costs);
        buf.writeUtf(this.name);
    }

}
