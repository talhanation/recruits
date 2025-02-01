package com.talhanation.recruits.network;

import com.talhanation.recruits.TeamEvents;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;

public class MessageAddPlayerToTeam implements Message<MessageAddPlayerToTeam> {

    private String teamName;
    private String namePlayerToAdd;

    public MessageAddPlayerToTeam(){
    }

    public MessageAddPlayerToTeam(String teamName, String namePlayerToAdd) {
        this.teamName = teamName;
        this.namePlayerToAdd = namePlayerToAdd;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        ServerLevel world = player.serverLevel();

        TeamEvents.addPlayerToTeam(player, world, this.teamName, this.namePlayerToAdd);
    }

    public MessageAddPlayerToTeam fromBytes(FriendlyByteBuf buf) {
        this.teamName = buf.readUtf();
        this.namePlayerToAdd = buf.readUtf();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.teamName);
        buf.writeUtf(this.namePlayerToAdd);
    }
}
