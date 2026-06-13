package com.talhanation.recruits.network;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.network.codec.ClaimNetworkCodec;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


public class MessageUpdateClaim implements Message<MessageUpdateClaim> {
    private static final int CLAIM_AREA_RANGE = 2;
    private static final int MAX_PLAYER_CLAIM_DISTANCE_CHUNKS = 4;
    private static final int BUFFER_ZONE_RADIUS = 3;
    private static final int MAX_CLAIM_NAME_LENGTH = 32;

    private RecruitsClaim claim;

    public MessageUpdateClaim(){

    }

    public MessageUpdateClaim(RecruitsClaim claim) {
        this.claim = claim;
    }

    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    public void executeServerSide(NetworkEvent.Context context){
        ServerPlayer sender = context.getSender();
        RecruitsClaim updatedClaim = this.claim;
        if (updatedClaim == null || sender == null) return;
        if(!RecruitsServerConfig.AllowClaiming.get()) return;
        if(sender.level().dimension() != Level.OVERWORLD) return;

        ServerLevel level = (ServerLevel) sender.getCommandSenderWorld();
        RecruitsClaim acceptedClaim = validateClaimUpdate(level, sender, updatedClaim);
        if (acceptedClaim == null) {
            resyncKnownClaim(sender, updatedClaim);
            return;
        }

        ClaimEvents.recruitsClaimManager.addOrUpdateClaim(level, acceptedClaim);
    }

    @Nullable
    private RecruitsClaim validateClaimUpdate(
            ServerLevel level, ServerPlayer sender, RecruitsClaim requestedClaim) {
        Set<ChunkPos> requestedChunks = chunkSet(requestedClaim);
        if (!requestedClaim.isRemoved
                && requestedChunks.size() > RecruitsServerConfig.MaxClaimChunks.get()) {
            return null;
        }

        RecruitsClaim currentClaim =
                ClaimEvents.recruitsClaimManager.getClaim(requestedClaim.getUUID());
        if (currentClaim == null) {
            return validateCreateClaim(level, sender, requestedClaim, requestedChunks);
        }

        if (requestedClaim.isRemoved) {
            return validateDeleteClaim(sender, currentClaim);
        }

        Set<ChunkPos> currentChunks = chunkSet(currentClaim);
        if (requestedChunks.equals(currentChunks)) {
            return validateEditClaim(level, sender, currentClaim, requestedClaim);
        }

        if (requestedChunks.size() == currentChunks.size() + 1
                && requestedChunks.containsAll(currentChunks)) {
            return validateAddChunk(sender, currentClaim, currentChunks, requestedChunks);
        }

        if (requestedChunks.size() + 1 == currentChunks.size()
                && currentChunks.containsAll(requestedChunks)) {
            return validateRemoveChunk(sender, currentClaim, currentChunks, requestedChunks);
        }

        return null;
    }

    @Nullable
    private RecruitsClaim validateCreateClaim(
            ServerLevel level,
            ServerPlayer sender,
            RecruitsClaim requestedClaim,
            Set<ChunkPos> requestedChunks) {
        if (requestedClaim.isRemoved || requestedChunks.isEmpty()) return null;

        RecruitsFaction senderFaction = getSenderFaction(sender);
        if (senderFaction == null || !isFactionLeader(sender, senderFaction)) return null;
        if (!sameFaction(senderFaction, requestedClaim.getOwnerFaction())) return null;

        ChunkPos center = requestedClaim.getCenter();
        if (center == null || !isNearPlayer(sender, center, MAX_PLAYER_CLAIM_DISTANCE_CHUNKS)) {
            return null;
        }
        if (!isExactClaimArea(center, requestedChunks)) return null;

        for (ChunkPos chunk : requestedChunks) {
            if (ClaimEvents.recruitsClaimManager.getClaim(chunk) != null) return null;
            if (isInBufferZone(chunk, senderFaction)) return null;
        }

        if (!takePayment(sender, getClaimCost(senderFaction))) return null;

        RecruitsClaim acceptedClaim =
                RecruitsClaim.fromNetwork(
                        requestedClaim.getUUID(),
                        safeClaimName(requestedClaim.getName(), senderFaction.getTeamDisplayName()),
                        senderFaction);
        acceptedClaim.setPlayer(
                new RecruitsPlayerInfo(sender.getUUID(), sender.getScoreboardName(), senderFaction));
        acceptedClaim.setCenter(center);
        for (ChunkPos chunk : requestedChunks) {
            acceptedClaim.addChunk(chunk);
        }
        acceptedClaim.resetHealth();
        return acceptedClaim;
    }

    @Nullable
    private RecruitsClaim validateDeleteClaim(ServerPlayer sender, RecruitsClaim currentClaim) {
        if (!isAdminCreative(sender)) return null;

        RecruitsClaim acceptedClaim = copyClaim(currentClaim);
        acceptedClaim.isRemoved = true;
        return acceptedClaim;
    }

    @Nullable
    private RecruitsClaim validateEditClaim(
            ServerLevel level,
            ServerPlayer sender,
            RecruitsClaim currentClaim,
            RecruitsClaim requestedClaim) {
        if (!canManageClaim(sender, currentClaim)) return null;

        RecruitsPlayerInfo requestedPlayerInfo = requestedClaim.getPlayerInfo();
        RecruitsPlayerInfo acceptedPlayerInfo = resolvePlayerInfo(level, requestedPlayerInfo);
        if (acceptedPlayerInfo == null || acceptedPlayerInfo.getFaction() == null) return null;

        boolean ownerChanges = !sameFaction(currentClaim.getOwnerFaction(), acceptedPlayerInfo.getFaction());
        if (ownerChanges && currentClaim.isUnderSiege) return null;

        RecruitsClaim acceptedClaim = copyClaim(currentClaim);
        acceptedClaim.setName(safeClaimName(requestedClaim.getName(), currentClaim.getName()));
        acceptedClaim.setPlayer(acceptedPlayerInfo);
        acceptedClaim.setOwnerFaction(acceptedPlayerInfo.getFaction());
        acceptedClaim.setBlockInteractionAllowed(requestedClaim.isBlockInteractionAllowed());
        acceptedClaim.setBlockPlacementAllowed(requestedClaim.isBlockPlacementAllowed());
        acceptedClaim.setBlockBreakingAllowed(requestedClaim.isBlockBreakingAllowed());
        return acceptedClaim;
    }

    @Nullable
    private RecruitsClaim validateAddChunk(
            ServerPlayer sender,
            RecruitsClaim currentClaim,
            Set<ChunkPos> currentChunks,
            Set<ChunkPos> requestedChunks) {
        if (!canManageClaim(sender, currentClaim)) return null;
        if (currentChunks.size() >= RecruitsServerConfig.MaxClaimChunks.get()) return null;

        ChunkPos addedChunk = onlyExtraChunk(requestedChunks, currentChunks);
        if (addedChunk == null) return null;
        if (!isNearPlayer(sender, addedChunk, MAX_PLAYER_CLAIM_DISTANCE_CHUNKS)) return null;
        if (!hasCardinalNeighbor(addedChunk, currentChunks)) return null;
        if (ClaimEvents.recruitsClaimManager.getClaim(addedChunk) != null) return null;
        if (isInBufferZone(addedChunk, currentClaim.getOwnerFaction())) return null;

        if (!takePayment(sender, RecruitsServerConfig.ChunkCost.get())) return null;

        RecruitsClaim acceptedClaim = copyClaim(currentClaim);
        acceptedClaim.addChunk(addedChunk);
        acceptedClaim.setCenter(calculateCenter(chunkSet(acceptedClaim)));
        return acceptedClaim;
    }

    @Nullable
    private RecruitsClaim validateRemoveChunk(
            ServerPlayer sender,
            RecruitsClaim currentClaim,
            Set<ChunkPos> currentChunks,
            Set<ChunkPos> requestedChunks) {
        boolean adminCreative = isAdminCreative(sender);
        if (!adminCreative && !canManageClaim(sender, currentClaim)) return null;

        ChunkPos removedChunk = onlyExtraChunk(currentChunks, requestedChunks);
        if (removedChunk == null) return null;
        if (!adminCreative && !isNearPlayer(sender, removedChunk, MAX_PLAYER_CLAIM_DISTANCE_CHUNKS)) {
            return null;
        }
        if (!canRemoveChunk(removedChunk, currentChunks)) return null;

        RecruitsClaim acceptedClaim = copyClaim(currentClaim);
        acceptedClaim.removeChunk(removedChunk);
        if (acceptedClaim.getClaimedChunks().isEmpty()) {
            acceptedClaim.isRemoved = true;
        } else {
            acceptedClaim.setCenter(calculateCenter(chunkSet(acceptedClaim)));
        }
        return acceptedClaim;
    }

    private static boolean takePayment(ServerPlayer sender, int cost) {
        if (cost <= 0 || sender.isCreative()) return true;
        if (!FactionEvents.playerHasEnoughEmeralds(sender, cost)) return false;
        FactionEvents.doPayment(sender, cost);
        return true;
    }

    private static int getClaimCost(RecruitsFaction ownerFaction) {
        if (!RecruitsServerConfig.CascadeThePriceOfClaims.get()) {
            return RecruitsServerConfig.ClaimingCost.get();
        }

        int amount = 1;
        if (ownerFaction != null) {
            for (RecruitsClaim claim : ClaimEvents.recruitsClaimManager.getAllClaims()) {
                if (claim == null || claim.getOwnerFaction() == null) continue;
                if (sameFaction(claim.getOwnerFaction(), ownerFaction)) amount++;
            }
        }
        return amount * RecruitsServerConfig.ClaimingCost.get();
    }

    @Nullable
    private static RecruitsFaction getSenderFaction(ServerPlayer sender) {
        Team team = sender.getTeam();
        if (team == null) return null;
        return FactionEvents.recruitsFactionManager.getFactionByStringID(team.getName());
    }

    private static boolean canManageClaim(ServerPlayer sender, RecruitsClaim claim) {
        return isAdminCreative(sender)
                || isFactionLeader(sender, claim.getOwnerFaction())
                || isClaimLeader(sender, claim);
    }

    private static boolean isFactionLeader(ServerPlayer sender, @Nullable RecruitsFaction faction) {
        return faction != null && Objects.equals(faction.getTeamLeaderUUID(), sender.getUUID());
    }

    private static boolean isClaimLeader(ServerPlayer sender, RecruitsClaim claim) {
        return claim != null
                && claim.getPlayerInfo() != null
                && Objects.equals(claim.getPlayerInfo().getUUID(), sender.getUUID());
    }

    private static boolean isAdminCreative(ServerPlayer sender) {
        return sender.isCreative() && sender.hasPermissions(2);
    }

    @Nullable
    private static RecruitsPlayerInfo resolvePlayerInfo(
            ServerLevel level, @Nullable RecruitsPlayerInfo requestedPlayerInfo) {
        if (requestedPlayerInfo == null
                || requestedPlayerInfo.getUUID() == null
                || requestedPlayerInfo.getFaction() == null) {
            return null;
        }

        RecruitsFaction faction =
                FactionEvents.recruitsFactionManager.getFactionByStringID(
                        requestedPlayerInfo.getFaction().getStringID());
        if (faction == null) return null;

        ServerPlayer onlinePlayer =
                level.getServer().getPlayerList().getPlayer(requestedPlayerInfo.getUUID());
        if (onlinePlayer != null) {
            Team team = onlinePlayer.getTeam();
            if (team == null || !Objects.equals(team.getName(), faction.getStringID())) return null;
            RecruitsPlayerInfo playerInfo =
                    new RecruitsPlayerInfo(onlinePlayer.getUUID(), onlinePlayer.getScoreboardName(), faction);
            playerInfo.setOnline(true);
            return playerInfo;
        }

        for (RecruitsPlayerInfo member : faction.getMembers()) {
            if (member != null && Objects.equals(member.getUUID(), requestedPlayerInfo.getUUID())) {
                return new RecruitsPlayerInfo(member.getUUID(), member.getName(), faction);
            }
        }

        PlayerTeam playerTeam = level.getScoreboard().getPlayerTeam(faction.getStringID());
        if (playerTeam != null && playerTeam.getPlayers().contains(requestedPlayerInfo.getName())) {
            return new RecruitsPlayerInfo(
                    requestedPlayerInfo.getUUID(),
                    requestedPlayerInfo.getName(),
                    faction);
        }

        return null;
    }

    private static boolean isExactClaimArea(ChunkPos center, Set<ChunkPos> chunks) {
        int expectedSize = (CLAIM_AREA_RANGE * 2 + 1) * (CLAIM_AREA_RANGE * 2 + 1);
        if (chunks.size() != expectedSize) return false;

        for (int dx = -CLAIM_AREA_RANGE; dx <= CLAIM_AREA_RANGE; dx++) {
            for (int dz = -CLAIM_AREA_RANGE; dz <= CLAIM_AREA_RANGE; dz++) {
                if (!chunks.contains(new ChunkPos(center.x + dx, center.z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isNearPlayer(ServerPlayer sender, ChunkPos chunk, int maxDistance) {
        ChunkPos playerChunk = sender.chunkPosition();
        return Math.abs(playerChunk.x - chunk.x) <= maxDistance
                && Math.abs(playerChunk.z - chunk.z) <= maxDistance;
    }

    private static boolean isInBufferZone(ChunkPos chunk, @Nullable RecruitsFaction ownFaction) {
        if (chunk == null || ownFaction == null) return false;

        for (RecruitsClaim claim : ClaimEvents.recruitsClaimManager.getAllClaims()) {
            if (claim == null || claim.getOwnerFaction() == null || claim.getClaimedChunks() == null) continue;
            if (sameFaction(claim.getOwnerFaction(), ownFaction)) continue;

            for (ChunkPos claimChunk : claim.getClaimedChunks()) {
                if (claimChunk == null) continue;
                int dx = Math.abs(chunk.x - claimChunk.x);
                int dz = Math.abs(chunk.z - claimChunk.z);
                if (dx <= BUFFER_ZONE_RADIUS && dz <= BUFFER_ZONE_RADIUS && !(dx == 0 && dz == 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCardinalNeighbor(ChunkPos chunk, Set<ChunkPos> chunks) {
        return chunks.contains(new ChunkPos(chunk.x + 1, chunk.z))
                || chunks.contains(new ChunkPos(chunk.x - 1, chunk.z))
                || chunks.contains(new ChunkPos(chunk.x, chunk.z + 1))
                || chunks.contains(new ChunkPos(chunk.x, chunk.z - 1));
    }

    private static boolean canRemoveChunk(ChunkPos chunk, Set<ChunkPos> chunks) {
        int openSides = 0;
        if (!chunks.contains(new ChunkPos(chunk.x + 1, chunk.z))) openSides++;
        if (!chunks.contains(new ChunkPos(chunk.x - 1, chunk.z))) openSides++;
        if (!chunks.contains(new ChunkPos(chunk.x, chunk.z + 1))) openSides++;
        if (!chunks.contains(new ChunkPos(chunk.x, chunk.z - 1))) openSides++;
        return openSides >= 2;
    }

    @Nullable
    private static ChunkPos onlyExtraChunk(Set<ChunkPos> largerSet, Set<ChunkPos> smallerSet) {
        ChunkPos extra = null;
        for (ChunkPos chunk : largerSet) {
            if (smallerSet.contains(chunk)) continue;
            if (extra != null) return null;
            extra = chunk;
        }
        return extra;
    }

    private static ChunkPos calculateCenter(Set<ChunkPos> chunks) {
        if (chunks.isEmpty()) return new ChunkPos(0, 0);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (ChunkPos chunk : chunks) {
            minX = Math.min(minX, chunk.x);
            maxX = Math.max(maxX, chunk.x);
            minZ = Math.min(minZ, chunk.z);
            maxZ = Math.max(maxZ, chunk.z);
        }
        return new ChunkPos((minX + maxX) / 2, (minZ + maxZ) / 2);
    }

    private static Set<ChunkPos> chunkSet(RecruitsClaim claim) {
        Set<ChunkPos> chunks = new HashSet<>();
        if (claim == null || claim.getClaimedChunks() == null) return chunks;

        for (ChunkPos chunk : claim.getClaimedChunks()) {
            if (chunk != null) chunks.add(chunk);
        }
        return chunks;
    }

    private static RecruitsClaim copyClaim(RecruitsClaim source) {
        RecruitsClaim copy =
                RecruitsClaim.fromNetwork(source.getUUID(), source.getName(), source.getOwnerFaction());
        copy.setPlayer(source.getPlayerInfo());
        copy.setBlockInteractionAllowed(source.isBlockInteractionAllowed());
        copy.setBlockPlacementAllowed(source.isBlockPlacementAllowed());
        copy.setBlockBreakingAllowed(source.isBlockBreakingAllowed());
        copy.setAdminClaim(source.isAdmin);
        copy.isUnderSiege = source.isUnderSiege;
        copy.isRemoved = source.isRemoved;
        copy.setCenter(source.getCenter());
        copy.setHealth(source.getHealth());
        copy.setSiegeSpeedPercent(source.getSiegeSpeedPercent());
        if (source.defendingParties != null) copy.defendingParties.addAll(source.defendingParties);
        if (source.attackingParties != null) copy.attackingParties.addAll(source.attackingParties);
        for (ChunkPos chunk : chunkSet(source)) {
            copy.addChunk(chunk);
        }
        return copy;
    }

    private static void resyncKnownClaim(ServerPlayer sender, RecruitsClaim requestedClaim) {
        RecruitsClaim currentClaim =
                ClaimEvents.recruitsClaimManager.getClaim(requestedClaim.getUUID());
        RecruitsClaim reply = currentClaim != null ? currentClaim : copyClaim(requestedClaim);
        if (currentClaim == null) {
            reply.isRemoved = true;
        }

        Main.SIMPLE_CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> sender),
                new MessageToClientUpdateClaim(reply));
    }

    private static String safeClaimName(String name, String fallback) {
        String safeName = name == null || name.isBlank() ? fallback : name;
        if (safeName == null || safeName.isBlank()) safeName = "claim";
        return safeName.length() <= MAX_CLAIM_NAME_LENGTH
                ? safeName
                : safeName.substring(0, MAX_CLAIM_NAME_LENGTH);
    }

    private static boolean sameFaction(@Nullable RecruitsFaction left, @Nullable RecruitsFaction right) {
        if (left == null || right == null) return false;
        return Objects.equals(left.getStringID(), right.getStringID());
    }

    public MessageUpdateClaim fromBytes(FriendlyByteBuf buf) {
        this.claim = ClaimNetworkCodec.readNullableClaim(buf);
        return this;
    }

    public void toBytes(FriendlyByteBuf buf) {
        ClaimNetworkCodec.writeNullableClaim(buf, this.claim);
    }
}
