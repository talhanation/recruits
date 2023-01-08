package com.talhanation.recruits.network;

import com.talhanation.recruits.client.gui.team.TeamInspectionScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;

public class MessageToClientUpdateTeam implements Message<MessageToClientUpdateTeam> {

    public String leaderName;
    public UUID playerUUID;
    public UUID leaderUUID;
    public ItemStack banner;
    public int players;
    public int npcs;
    //public List<String> joinRequests;

    public MessageToClientUpdateTeam() {
    }

    public MessageToClientUpdateTeam(UUID playerUUID, String leaderName, UUID leaderUUID, ItemStack banner, List<String> joinRequests, int players, int npcs) {
        this.leaderName = leaderName;
        this.playerUUID = playerUUID;
        this.leaderUUID = leaderUUID;
        this.banner = banner;
        this.players = players;
        this.npcs = npcs;
        //this.joinRequests = joinRequests;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        TeamInspectionScreen.leader = leaderName;
        TeamInspectionScreen.bannerItem = banner;
        TeamInspectionScreen.leaderUUID = leaderUUID;
        TeamInspectionScreen.players = players;
        TeamInspectionScreen.npcs = npcs;
        //TeamAddPlayerScreen.joinRequests = joinRequests;
    }

    @Override
    public MessageToClientUpdateTeam fromBytes(FriendlyByteBuf buf) {
        this.leaderName = buf.readUtf();
        this.playerUUID = buf.readUUID();
        this.leaderUUID = buf.readUUID();
        this.banner = buf.readItem();
        this.players = buf.readInt();
        this.npcs = buf.readInt();

        //this.joinRequests = buf.readList(FriendlyByteBuf::readUtf);
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(leaderName);
        buf.writeUUID(playerUUID);
        buf.writeUUID(leaderUUID);
        buf.writeItem(banner);
        buf.writeInt(players);
        buf.writeInt(npcs);
        //buf.writeCollection(joinRequests, FriendlyByteBuf::writeUtf);
    }

}