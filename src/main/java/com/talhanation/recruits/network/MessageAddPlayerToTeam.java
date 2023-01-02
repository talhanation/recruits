package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageAddPlayerToTeam implements Message<MessageAddPlayerToTeam> {

    private String teamName;
    private String playerName;

    public MessageAddPlayerToTeam(){
    }

    public MessageAddPlayerToTeam(String teamName, String playerName) {
        this.teamName = teamName;
        this.playerName = playerName;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        ServerLevel world = player.getLevel();

        TeamEvents.addPlayerToTeam(context.getSender(), world, this.teamName, this.playerName);
    }

    public MessageAddPlayerToTeam fromBytes(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.playerName = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeUtf(this.playerName);
    }
}
