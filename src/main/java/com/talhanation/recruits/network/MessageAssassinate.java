package com.talhanation.recruits.network;

import com.talhanation.recruits.AssassinEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;

public class MessageAssassinate implements Message<MessageAssassinate> {

    //private UUID target;
    private int count;
    private String name;

    public MessageAssassinate(){
    }

    public MessageAssassinate(String name, int count) {
        //this.target = null;
        this.count = count;
        this.name = name;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayerEntity player = context.getSender();
        ServerWorld world = player.getLevel();
        MinecraftServer server = world.getServer();
        PlayerList list = server.getPlayerList();
        ServerPlayerEntity targetPlayer = list.getPlayerByName(name);
        if (targetPlayer != null) {
            player.sendMessage(new StringTextComponent("Successfully found the Target"), player.getUUID());
            //this.target = targetPlayer.getUUID();
            AssassinEvents.createAssassin(name, count, world);
        }
        else {
            player.sendMessage(new StringTextComponent("Could not found the Target"), player.getUUID());
            player.sendMessage(new StringTextComponent("Text: " + this.name), player.getUUID());
        }
    }

    public MessageAssassinate fromBytes(PacketBuffer buf) {
        //this.target = buf.readUUID();
        this.count = buf.readInt();
        this.name = buf.readUtf();
        return this;
    }

    public void toBytes(PacketBuffer buf) {
        //buf.writeUUID(this.target);
        buf.writeInt(this.count);
        buf.writeUtf(this.name);
    }

}
