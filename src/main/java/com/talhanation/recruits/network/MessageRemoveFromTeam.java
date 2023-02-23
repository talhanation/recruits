package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageRemoveFromTeam implements Message<MessageRemoveFromTeam> {

    private String player;

    public MessageRemoveFromTeam(){
    }

    public MessageRemoveFromTeam(String player){
        this.player = player;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerLevel level = context.getSender().getLevel();

        level.players().stream().toList().forEach(serverPlayer -> TeamEvents.tryToRemoveFromTeam(serverPlayer, level, player));
    }

    public MessageRemoveFromTeam fromBytes(FriendlyByteBuf buf) {
        this.player = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(player);
    }
}
