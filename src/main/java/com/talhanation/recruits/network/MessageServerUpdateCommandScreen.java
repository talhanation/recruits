package com.talhanation.recruits.network;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

public class MessageServerUpdateCommandScreen implements Message<MessageServerUpdateCommandScreen> {

    public String teamName;
    public int group;

    public MessageServerUpdateCommandScreen() {
    }

    public MessageServerUpdateCommandScreen(int group) {
        this.group = group;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        CommandEvents.updateCommandScreen(context.getSender(), this.group);
    }

    @Override
    public MessageServerUpdateCommandScreen fromBytes(FriendlyByteBuf buf) {
        this.group = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(group);
    }

}