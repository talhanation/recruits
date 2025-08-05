package com.talhanation.recruits.world;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public class RecruitsClaim {
    private final List<ChunkPos> claimedChunks = new ArrayList<>();
    private String name;
    private RecruitsTeam ownerFaction;
    private boolean allowBlockInteraction = true;
    private boolean allowBlockPlacement   = true;
    private boolean allowBlockBreaking    = true;
    public List<RecruitsTeam> defendingParties = new ArrayList<>();
    public List<RecruitsTeam> attackingParties = new ArrayList<>();
    public ChunkPos center;
    public boolean isUnderSiege;
    public int health;
    public RecruitsPlayerInfo playerInfo;
    public boolean isAdmin;
    public RecruitsClaim(String name, RecruitsTeam ownerFaction) {
        this.name = name;
        this.ownerFaction = ownerFaction;
        this.isAdmin = false;
        resetHealth();
    }
    public void setCenter(ChunkPos center){
        this.center = center;
    }

    public ChunkPos getCenter(){
        return this.center;
    }
    public void addChunk(ChunkPos chunkPos) {
        if (!claimedChunks.contains(chunkPos)) {
            claimedChunks.add(chunkPos);
        }
    }

    public void removeChunk(ChunkPos chunkPos) {
        claimedChunks.remove(chunkPos);
    }

    public boolean containsChunk(ChunkPos chunkPos) {
        return claimedChunks.contains(chunkPos);
    }

    public List<ChunkPos> getClaimedChunks() {
        return claimedChunks;
    }

    public String getName() {
        return name;
    }

    public String getOwnerFactionStringID() {
        return ownerFaction.getStringID();
    }
    public RecruitsTeam getOwnerFaction(){
        return ownerFaction;
    }
    public RecruitsPlayerInfo getPlayerInfo(){
        return playerInfo;
    }
    public boolean isBlockInteractionAllowed() {
        return allowBlockInteraction;
    }

    public boolean isBlockPlacementAllowed() {
        return allowBlockPlacement;
    }

    public boolean isBlockBreakingAllowed() {
        return allowBlockBreaking;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setAdminClaim(boolean admin){
        this.isAdmin = admin;
    }
    public void setOwnerFaction(RecruitsTeam faction) {
        this.ownerFaction = faction;
    }
    public void setPlayer(RecruitsPlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }
    public void setBlockInteractionAllowed(boolean allow) {
        this.allowBlockInteraction = allow;
    }

    public void setBlockPlacementAllowed(boolean allow) {
        this.allowBlockPlacement = allow;
    }

    public void setBlockBreakingAllowed(boolean allow) {
        this.allowBlockBreaking = allow;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", name);
        if(ownerFaction != null) nbt.put("ownerFaction", ownerFaction.toNBT());
        if(playerInfo != null) nbt.put("playerInfo", playerInfo.toNBT());
        nbt.putBoolean("allowInteraction", allowBlockInteraction);
        nbt.putBoolean("allowPlacement",   allowBlockPlacement);
        nbt.putBoolean("allowBreaking",    allowBlockBreaking);
        nbt.putBoolean("isAdmin",    isAdmin);
        nbt.putBoolean("isUnderSiege", isUnderSiege);

        ListTag chunkList = new ListTag();
        for (ChunkPos pos : claimedChunks) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putInt("x", pos.x);
            chunkTag.putInt("z", pos.z);
            chunkList.add(chunkTag);
        }
        nbt.put("chunks", chunkList);

        nbt.putInt("centerX", this.getCenter().x);
        nbt.putInt("centerZ", this.getCenter().z);

        nbt.putInt("health", this.getHealth());
        return nbt;
    }

    public static RecruitsClaim fromNBT(CompoundTag nbt) {
        String name = nbt.getString("name");
        RecruitsTeam recruitsTeam = RecruitsTeam.fromNBT(nbt.getCompound("ownerFaction"));
        RecruitsClaim claim = new RecruitsClaim(name, recruitsTeam);
        RecruitsPlayerInfo playerInfo = RecruitsPlayerInfo.getFromNBT(nbt.getCompound("playerInfo"));
        if(playerInfo != null) claim.setPlayer(playerInfo);

        claim.setBlockInteractionAllowed(nbt.getBoolean("allowInteraction"));
        claim.setBlockPlacementAllowed(nbt.getBoolean("allowPlacement"));
        claim.setBlockBreakingAllowed(nbt.getBoolean("allowBreaking"));
        claim.setAdminClaim(nbt.getBoolean("isAdmin"));
        claim.isUnderSiege = nbt.getBoolean("isUnderSiege");

        if (nbt.contains("chunks", Tag.TAG_LIST)) {
            ListTag chunkList = nbt.getList("chunks", Tag.TAG_COMPOUND);
            for (Tag tag : chunkList) {
                CompoundTag chunkTag = (CompoundTag) tag;
                int x = chunkTag.getInt("x");
                int z = chunkTag.getInt("z");
                claim.addChunk(new ChunkPos(x, z));
            }
        }
        int x = nbt.getInt("centerX");
        int z = nbt.getInt("centerZ");

        claim.setCenter(new ChunkPos(x, z));

        claim.setHealth(nbt.getInt("health"));

        return claim;
    }

    public static CompoundTag toNBT(List<RecruitsClaim> list) {
        CompoundTag nbt = new CompoundTag();
        ListTag claimList = new ListTag();

        for (RecruitsClaim claim : list) {
            claimList.add(claim.toNBT());
        }

        nbt.put("Claims", claimList);
        return nbt;
    }

    public static List<RecruitsClaim> getListFromNBT(CompoundTag nbt) {
        List<RecruitsClaim> list = new ArrayList<>();
        ListTag claimList = nbt.getList("Claims", Tag.TAG_COMPOUND);

        for (int i = 0; i < claimList.size(); i++) {
            CompoundTag claimTag = claimList.getCompound(i);
            RecruitsClaim claim = RecruitsClaim.fromNBT(claimTag);
            list.add(claim);
        }

        return list;
    }

    public void setUnderSiege(boolean underSiege, ServerLevel level) {
        RecruitsTeam ownerFaction = TeamEvents.recruitsTeamManager.getTeamByStringID(this.getOwnerFactionStringID());
        if(ownerFaction == null) return;

        if(!this.isUnderSiege && underSiege){
            this.isUnderSiege = true;

            for(Player player : TeamEvents.recruitsTeamManager.getPlayersInTeam(this.getOwnerFactionStringID(), level)){
                player.sendSystemMessage(SIEGE_START_DEFENDER(this.getName(), attackingParties.toString()));
            }
            for(RecruitsTeam attacker: attackingParties){
                for(Player player : TeamEvents.recruitsTeamManager.getPlayersInTeam(attacker.getStringID(), level)){
                    player.sendSystemMessage(SIEGE_START_ATTACKER(this.getName()));
                }
            }

        }
        else if(this.isUnderSiege && !underSiege){
            this.isUnderSiege = false;

            for(Player player : TeamEvents.recruitsTeamManager.getPlayersInTeam(this.getOwnerFactionStringID(), level)){
                player.sendSystemMessage(SIEGE_FAILED_DEFENDER(this.getName()));
            }
            for(RecruitsTeam attacker: attackingParties){
                for(Player player : TeamEvents.recruitsTeamManager.getPlayersInTeam(attacker.getStringID(), level)){
                    player.sendSystemMessage(SIEGE_FAILED_ATTACKER(this.getName()));
                }
            }
        }
    }

    public void setSiegeSuccess(ServerLevel level){
        for(Player player : TeamEvents.recruitsTeamManager.getPlayersInTeam(this.getOwnerFactionStringID(), level)){
            player.sendSystemMessage(SIEGE_SUCCESS_DEFENDER(this.getName()));
        }

        this.setOwnerFaction(attackingParties.get(0));
        this.isUnderSiege = false;
        this.resetHealth();

        for(Player player : TeamEvents.recruitsTeamManager.getPlayersInTeam(this.getOwnerFactionStringID(), level)){
            player.sendSystemMessage(SIEGE_SUCCESS_ATTACKER(this.getName()));
        }

        this.attackingParties.clear();
        this.defendingParties.clear();
    }

    public void resetHealth() {
        this.health = getMaxHealth();
    }

    public int getMaxHealth(){
        return 60 * RecruitsServerConfig.SiegeClaimsConquerTime.get();
    }

    public Component SIEGE_START_ATTACKER(String claim){
        return Component.translatable("chat.recruits.text.siegeStartAttacker", claim);
    }
    public Component SIEGE_START_DEFENDER(String claim, String attackers){
        return Component.translatable("chat.recruits.text.siegeStartDefender", claim, attackers);
    }

    public Component SIEGE_FAILED_ATTACKER(String claim){
        return Component.translatable("chat.recruits.text.siegeFailedAttacker", claim);
    }

    public Component SIEGE_FAILED_DEFENDER(String claim){
        return Component.translatable("chat.recruits.text.siegeFailedDefender", claim);
    }
    public Component SIEGE_SUCCESS_ATTACKER(String claim){
        return Component.translatable("chat.recruits.text.siegeSuccessAttacker", claim);
    }

    public Component SIEGE_SUCCESS_DEFENDER(String claim){
        return Component.translatable("chat.recruits.text.siegeSuccessDefender", claim);
    }
}
