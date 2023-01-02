package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.team.TeamInspectionScreen;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;

public class MessageClientUpdateTeam implements Message<MessageClientUpdateTeam> {

    public String leaderName;
    public UUID playerUUID;
    public UUID leaderUUID;
    public ItemStack banner;

    public MessageClientUpdateTeam() {
    }

    public MessageClientUpdateTeam(UUID playerUUID, String leaderName, UUID leaderUUID, ItemStack banner) {
        this.leaderName = leaderName;
        this.playerUUID = playerUUID;
        this.leaderUUID = leaderUUID;
        this.banner = banner;
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.CLIENT;
    }

    @Override
    public void executeClientSide(NetworkEvent.Context context) {
        Main.LOGGER.debug("-----------MessageClientGetTeamLeader----------");
        Main.LOGGER.debug("leaderName: " + leaderName);
        Main.LOGGER.debug("banner: " + banner);
        TeamInspectionScreen.leader = leaderName;
        TeamInspectionScreen.bannerItem = banner;
        TeamInspectionScreen.leaderUUID = leaderUUID;
        Main.LOGGER.debug("-----------------------------------------------");
    }

    @Override
    public MessageClientUpdateTeam fromBytes(FriendlyByteBuf buf) {
        this.leaderName = buf.readUtf();
        this.playerUUID = buf.readUUID();
        this.leaderUUID = buf.readUUID();
        this.banner = buf.readItem();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(leaderName);
        buf.writeUUID(playerUUID);
        buf.writeUUID(leaderUUID);
        buf.writeItem(banner);
    }

}