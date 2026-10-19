package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsFactionManager;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;


public class MessageSaveTeamSettings implements Message<MessageSaveTeamSettings> {
    private CompoundTag nbt;
    private String stringID;
    private int cost;

    public MessageSaveTeamSettings() {
    }

    public MessageSaveTeamSettings(RecruitsFaction team, int cost) {
        this.nbt = team.toNBT();
        this.stringID = team.getStringID();
        this.cost = cost;
    }

    @Override
    public Dist getExecutingSide()  {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer sender = context.getSender();
        if (sender == null || nbt == null || stringID == null) return;

        // Only the leader of THIS faction (before: anyone could e.g. make himself leader of any faction).
        if (!NetworkSecurity.isLeaderOf(sender, stringID)) return;
        if (!RecruitsServerConfig.ShouldFactionEditingBeAllowed.get() && !NetworkSecurity.isAdmin(sender)) return;

        RecruitsFaction current = FactionEvents.recruitsFactionManager.getFactionByStringID(stringID);
        RecruitsFaction edited = RecruitsFaction.fromNBT(nbt);
        if (current == null || edited == null) return;
        if (!stringID.equals(edited.getStringID())) return;

        int changes = 0;

        // Leader: only an existing member of this faction, name from the server's member list.
        if (!current.getTeamLeaderUUID().equals(edited.getTeamLeaderUUID())) {
            RecruitsPlayerInfo newLeader = findMember(sender, current, edited.getTeamLeaderUUID());
            if (newLeader == null) return;
            edited.setTeamLeaderID(newLeader.getUUID());
            edited.setTeamLeaderName(newLeader.getName());
            changes++;
        }

        // Display name
        String displayName = NetworkSecurity.cleanText(edited.getTeamDisplayName(), 32);
        if (displayName.isBlank()) return;
        if (!displayName.equals(current.getTeamDisplayName())) {
            if (usedByOtherFaction(stringID, displayName, null)) return;
            changes++;
        }
        edited.setTeamDisplayName(displayName);

        // Team colour: must be a real colour, null/format codes break the scoreboard team packet for everyone.
        ChatFormatting color = ChatFormatting.getById(edited.getTeamColor());
        if (color == null || !color.isColor()) return;
        if (edited.getTeamColor() != current.getTeamColor()) changes++;

        // Unit colour
        if (edited.getUnitColor() < 0) return;
        if (edited.getUnitColor() != current.getUnitColor()) changes++;

        // Banner: has to be a non-blank banner that no other faction uses.
        if (!current.getBanner().equals(edited.getBanner())) {
            ItemStack banner = ItemStack.of(edited.getBanner());
            if (!(banner.getItem() instanceof BannerItem)) return;
            if (RecruitsFactionManager.isBannerBlank(banner)) return;
            if (usedByOtherFaction(stringID, null, edited.getBanner())) return;
            changes++;
        }

        // Max recruits per member: the GUI clamps to the server config, now the server does too.
        int maxPerPlayer = Math.max(0, Math.min(edited.getMaxNPCsPerPlayer(), RecruitsServerConfig.MaxRecruitsForPlayer.get()));
        edited.setMaxNPCsPerPlayer(maxPerPlayer);
        if (maxPerPlayer != current.getMaxNPCsPerPlayer()) changes++;

        if (changes == 0) return;

        // Price is calculated here like in FactionEditScreen#calculateCost - the client value is ignored.
        int serverCost = changes * RecruitsServerConfig.FactionCreationCost.get();
        FactionEvents.modifyTeam(sender.server.overworld(), stringID, edited, sender, serverCost);
    }

    private static boolean usedByOtherFaction(String ownId, String displayName, CompoundTag banner) {
        for (RecruitsFaction other : FactionEvents.recruitsFactionManager.getFactions()) {
            if (other == null || ownId.equals(other.getStringID())) continue;
            if (displayName != null && displayName.equalsIgnoreCase(other.getTeamDisplayName().strip())) return true;
            if (banner != null && banner.equals(other.getBanner())) return true;
        }
        return false;
    }

    private static RecruitsPlayerInfo findMember(ServerPlayer sender, RecruitsFaction faction, java.util.UUID uuid) {
        if (uuid == null) return null;
        for (RecruitsPlayerInfo member : faction.getMembers()) {
            if (member != null && uuid.equals(member.getUUID())) return member;
        }
        // Fallback for older saves without member list: an online player in this scoreboard team.
        ServerPlayer online = sender.server.getPlayerList().getPlayer(uuid);
        if (online != null && online.getTeam() != null && online.getTeam().getName().equals(faction.getStringID())) {
            return new RecruitsPlayerInfo(online.getUUID(), online.getScoreboardName());
        }
        return null;
    }

    @Override
    public MessageSaveTeamSettings fromBytes(FriendlyByteBuf buf) {
        this.nbt = buf.readNbt();
        this.stringID = buf.readUtf();
        this.cost = buf.readInt();
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.nbt);
        buf.writeUtf(this.stringID);
        buf.writeInt(this.cost);
    }
}
