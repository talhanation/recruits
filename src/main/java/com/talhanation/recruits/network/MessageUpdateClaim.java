package com.talhanation.recruits.network;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MessageUpdateClaim implements Message<MessageUpdateClaim> {

    private CompoundTag claimNBT;

    public MessageUpdateClaim(){

    }

    public MessageUpdateClaim(RecruitsClaim claim) {
        this.claimNBT = claim.toNBT();
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer player = context.getSender();
        if (player == null || this.claimNBT == null || this.claimNBT.isEmpty()) return;
        if(!RecruitsServerConfig.AllowClaiming.get()) return;
        if(player.level().dimension() != Level.OVERWORLD) return;

        RecruitsClaim requestedClaim = ClaimNetworkAuthority.readClaim(this.claimNBT);
        if (requestedClaim == null || requestedClaim.getCenter() == null || requestedClaim.getClaimedChunks().isEmpty()) return;

        ServerLevel level = (ServerLevel) player.getCommandSenderWorld();
        RecruitsClaim currentClaim = ClaimNetworkAuthority.claimByUuid(requestedClaim.getUUID());
        if (currentClaim == null) {
            handleNewClaim(player, level, requestedClaim);
        }
        else {
            handleExistingClaim(player, level, currentClaim, requestedClaim);
        }
    }

    private static void handleNewClaim(ServerPlayer player, ServerLevel level, RecruitsClaim requestedClaim) {
        RecruitsFaction ownerFaction = FactionNetworkAuthority.leaderFaction(player);
        List<ChunkPos> requestedChunks = requestedClaim.getClaimedChunks();
        if (ownerFaction == null) return;
        if (!ClaimNetworkAuthority.isFiveByFiveArea(requestedClaim.getCenter(), requestedChunks)) return;
        if (!ClaimNetworkAuthority.isNearPlayer(player, requestedClaim.getCenter())) return;
        if (ClaimEvents.recruitsClaimManager.claimExists(requestedClaim, requestedChunks)) return;
        if (ClaimNetworkAuthority.hasForeignBufferConflict(ownerFaction, requestedChunks)) return;

        RecruitsClaim newClaim = new RecruitsClaim(ownerFaction);
        newClaim.setCenter(requestedClaim.getCenter());
        newClaim.setPlayer(new RecruitsPlayerInfo(player.getUUID(), player.getName().getString(), ownerFaction));
        for (ChunkPos chunk : requestedChunks) {
            newClaim.addChunk(chunk);
        }
        int claimCost = ClaimNetworkAuthority.newClaimCost(ownerFaction);
        ClaimEvents.recruitsClaimManager.tryAddOrUpdateClaim(level, newClaim, () -> ClaimNetworkAuthority.pay(player, claimCost));
    }

    private static void handleExistingClaim(ServerPlayer player, ServerLevel level, RecruitsClaim currentClaim, RecruitsClaim requestedClaim) {
        if (requestedClaim.isRemoved) {
            if (!ClaimNetworkAuthority.isCreativeAdmin(player)) return;
            ClaimEvents.recruitsClaimManager.removeClaim(currentClaim);
            ClaimEvents.recruitsClaimManager.broadcastClaimsToAll(level);
            return;
        }

        if (currentClaim.getOwnerFaction() == null) return;
        if (!ClaimNetworkAuthority.canManageClaim(player, currentClaim) && !ClaimNetworkAuthority.isCreativeAdmin(player)) return;
        if (requestedClaim.getOwnerFaction() != null
                && !requestedClaim.getOwnerFactionStringID().equals(currentClaim.getOwnerFactionStringID())) {
            return;
        }

        Set<ChunkPos> currentChunks = new HashSet<>(currentClaim.getClaimedChunks());
        Set<ChunkPos> requestedChunks = new HashSet<>(requestedClaim.getClaimedChunks());
        Set<ChunkPos> addedChunks = new HashSet<>(requestedChunks);
        addedChunks.removeAll(currentChunks);
        Set<ChunkPos> removedChunks = new HashSet<>(currentChunks);
        removedChunks.removeAll(requestedChunks);
        RecruitsClaim updatedClaim = RecruitsClaim.fromNBT(currentClaim.toNBT());
        int paymentCost = 0;

        boolean updated;
        if (addedChunks.isEmpty() && removedChunks.isEmpty()) {
            if (!applyEditableMetadata(updatedClaim, requestedClaim)) return;
            updated = true;
        }
        else if (addedChunks.size() == 1 && removedChunks.isEmpty()) {
            updated = addChunk(player, updatedClaim, requestedClaim, addedChunks.iterator().next());
            paymentCost = RecruitsServerConfig.ChunkCost.get();
        }
        else if (addedChunks.isEmpty() && removedChunks.size() == 1) {
            updated = removeChunk(player, updatedClaim, requestedClaim, removedChunks.iterator().next());
        }
        else {
            return;
        }

        if (!updated) return;
        int cost = paymentCost;
        ClaimEvents.recruitsClaimManager.tryAddOrUpdateClaim(level, updatedClaim, () -> ClaimNetworkAuthority.pay(player, cost));
    }

    private static boolean addChunk(ServerPlayer player, RecruitsClaim currentClaim, RecruitsClaim requestedClaim, ChunkPos chunk) {
        if (currentClaim.getClaimedChunks().size() >= RecruitsClaim.MAX_SIZE) return false;
        if (!ClaimNetworkAuthority.isNearPlayer(player, chunk)) return false;
        if (ClaimEvents.recruitsClaimManager.getClaim(chunk) != null) return false;
        if (!hasNeighbor(currentClaim, chunk)) return false;
        if (ClaimNetworkAuthority.hasForeignBufferConflict(currentClaim.getOwnerFaction(), chunk)) return false;
        if (!applyEditableMetadata(currentClaim, requestedClaim)) return false;

        currentClaim.addChunk(chunk);
        ClaimNetworkAuthority.recalculateCenter(currentClaim);
        return true;
    }

    private static boolean removeChunk(ServerPlayer player, RecruitsClaim currentClaim, RecruitsClaim requestedClaim, ChunkPos chunk) {
        if (!ClaimNetworkAuthority.isNearPlayer(player, chunk)) return false;
        if (!ClaimNetworkAuthority.canRemoveChunk(currentClaim, chunk)) return false;
        if (!applyEditableMetadata(currentClaim, requestedClaim)) return false;
        currentClaim.removeChunk(chunk);
        ClaimNetworkAuthority.recalculateCenter(currentClaim);
        return true;
    }

    private static boolean hasNeighbor(RecruitsClaim claim, ChunkPos chunk) {
        return claim.containsChunk(new ChunkPos(chunk.x + 1, chunk.z))
                || claim.containsChunk(new ChunkPos(chunk.x - 1, chunk.z))
                || claim.containsChunk(new ChunkPos(chunk.x, chunk.z + 1))
                || claim.containsChunk(new ChunkPos(chunk.x, chunk.z - 1));
    }

    private static boolean applyEditableMetadata(RecruitsClaim currentClaim, RecruitsClaim requestedClaim) {
        if (requestedClaim.getName() == null || requestedClaim.getName().isBlank() || requestedClaim.getName().length() > 32) {
            return false;
        }
        RecruitsPlayerInfo requestedPlayer = requestedClaim.getPlayerInfo();
        RecruitsPlayerInfo member = null;
        if (requestedPlayer != null) {
            member = FactionNetworkAuthority.memberByUuid(currentClaim.getOwnerFaction(), requestedPlayer);
            if (member == null) return false;
        }

        currentClaim.setName(requestedClaim.getName());
        if (member != null) {
            currentClaim.setPlayer(new RecruitsPlayerInfo(member.getUUID(), member.getName(), currentClaim.getOwnerFaction()));
        }
        currentClaim.setBlockInteractionAllowed(requestedClaim.isBlockInteractionAllowed());
        currentClaim.setBlockPlacementAllowed(requestedClaim.isBlockPlacementAllowed());
        currentClaim.setBlockBreakingAllowed(requestedClaim.isBlockBreakingAllowed());
        return true;
    }

    public MessageUpdateClaim fromBytes(FriendlyByteBuf buf) {
        this.claimNBT = buf.readNbt();
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(claimNBT);
    }
}
