package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.team.TeamInspectionScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageToClientGetWorldTeams implements Message<MessageToClientGetWorldTeams> {

    public String leaderName;
    public UUID playerUUID;
    public ItemStack banner;

    public MessageToClientGetWorldTeams() {
    }

    public MessageToClientGetWorldTeams(UUID playerUUID, String leadername, ItemStack banner) {
        this.leaderName = leadername;
        this.playerUUID = playerUUID;
        this.banner = banner;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        //context.getSender().sendMessage(new TextComponent(leaderName), playerUUID);
        Main.LOGGER.debug("-----------MessageClientGetTeamLeader----------");
        Main.LOGGER.debug("leaderName: " + leaderName);
        Main.LOGGER.debug("banner: " + banner);
        TeamInspectionScreen.leader = leaderName;
        TeamInspectionScreen.bannerItem = banner;
        //ClientAccess.setTeamLeaderName(leaderName);
        Main.LOGGER.debug("-----------------------------------------------");
    }

    @Override
    public MessageToClientGetWorldTeams fromBytes(FriendlyByteBuf buf) {
        this.leaderName = buf.readUtf();
        this.playerUUID = buf.readUUID();
        this.banner = buf.readItem();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(leaderName);
        buf.writeUUID(playerUUID);
        buf.writeItem(banner);
    }

}