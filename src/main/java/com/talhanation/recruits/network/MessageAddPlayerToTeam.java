package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.world.RecruitsFaction;
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

        // Leader only, only players that asked to join and are not in another team.
        if (!NetworkSecurity.isLeaderOf(player, this.teamName)) return;
        if (!RecruitsServerConfig.ShouldFactionManagingBeAllowed.get() && !NetworkSecurity.isAdmin(player)) return;
        RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(this.teamName);
        if (faction == null || !faction.getJoinRequests().contains(this.namePlayerToAdd)) return;
        ServerPlayer toAdd = world.getServer().getPlayerList().getPlayerByName(this.namePlayerToAdd);
        if (toAdd == null || toAdd.getTeam() != null) return;

        FactionEvents.addPlayerToTeam(player, world, this.teamName, this.namePlayerToAdd);
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
