package com.talhanation.recruits.network;

import com.talhanation.recruits.AssassinEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;

public class MessageAssassinate implements Message<MessageAssassinate> {

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

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        ServerLevel world = player.getLevel();
        MinecraftServer server = world.getServer();
        PlayerList list = server.getPlayerList();
        ServerPlayer targetPlayer = list.getPlayerByName(name);
        if (targetPlayer != null) {
            player.sendMessage(new TextComponent("Successfully found the Target"), player.getUUID());
            //this.target = targetPlayer.getUUID();
            AssassinEvents.createAssassin(name, count, world);
            AssassinEvents.doPayment(player, costs);
        }
        else {
            player.sendMessage(new TextComponent("Could not found the Target"), player.getUUID());
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
