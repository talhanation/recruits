package com.talhanation.recruits.network;

import com.talhanation.recruits.AssassinEvents;
import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageCreateTeam implements Message<MessageCreateTeam> {

    private String teamName;

    public MessageCreateTeam(){
    }

    public MessageCreateTeam(String name) {
        this.teamName = name;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        Main.LOGGER.debug("hello from message");
        ServerPlayer player = context.getSender();
        ServerLevel world = player.getLevel();
        CommandEvents.createTeam(world, teamName, player.getName().getString());
    }

    public MessageCreateTeam fromBytes(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
    }

}
