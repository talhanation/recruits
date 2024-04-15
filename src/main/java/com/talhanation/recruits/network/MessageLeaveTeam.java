package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageLeaveTeam implements Message<MessageLeaveTeam> {

    public MessageLeaveTeam(){
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = context.getSender();
        ServerLevel level = player.serverLevel();
        TeamEvents.leaveTeam(false, player, null, level, false);
    }

    public MessageLeaveTeam fromBytes(FriendlyByteBuf buf) {
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {

    }
}
