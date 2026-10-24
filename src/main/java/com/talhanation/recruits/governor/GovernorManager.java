package com.talhanation.recruits.governor;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.network.MessageToClientGovernorData;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Holds the Governor data of all claims and the shared server-side helpers
 * (rule check, access check, garrison lookup, notifications, client snapshot).
 */
public class GovernorManager {

    public enum RuleStatus {
        RULING("ruling"),
        NO_CLAIM("no_claim"),
        NOT_CLAIM_OWNER("not_claim_owner"),
        NOT_IN_FACTION("not_in_faction"),
        REPLACED("replaced");

        private final String key;

        RuleStatus(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    @Nullable
    private static GovernorManager instance;

    private final Map<UUID, ClaimGovernorData> data = new HashMap<>();

    @Nullable
    public static GovernorManager get() {
        return instance;
    }

    public static void setInstance(@Nullable GovernorManager manager) {
        instance = manager;
    }

    // ------------------------------------------------------------------ persistence

    public void load(ServerLevel overworld) {
        data.clear();
        GovernorSaveData saveData = GovernorSaveData.get(overworld);
        for (ClaimGovernorData entry : saveData.getEntries()) {
            data.put(entry.getClaimId(), entry);
        }
        saveData.setLiveSource(this);
    }

    public void save(ServerLevel overworld) {
        if (ClaimEvents.recruitsClaimManager != null) {
            // drop data of claims that were deleted
            data.keySet().removeIf(claimId -> ClaimEvents.recruitsClaimManager.getClaim(claimId) == null);
        }
        GovernorSaveData.get(overworld).setEntries(data.values());
    }

    /** Called when the server stops: the save data keeps a plain copy and no longer points at this manager. */
    public void detach(ServerLevel overworld) {
        GovernorSaveData saveData = GovernorSaveData.get(overworld);
        saveData.setEntries(data.values());
        saveData.setLiveSource(null);
    }

    public Collection<ClaimGovernorData> getAll() {
        return data.values();
    }

    @Nullable
    public ClaimGovernorData getData(@Nullable UUID claimId) {
        if (claimId == null) return null;
        return data.get(claimId);
    }

    public ClaimGovernorData getOrCreate(UUID claimId) {
        return data.computeIfAbsent(claimId, ClaimGovernorData::new);
    }

    public void remove(UUID claimId) {
        data.remove(claimId);
    }

    // ------------------------------------------------------------------ claims

    @Nullable
    public static RecruitsClaim getClaim(@Nullable UUID claimId) {
        if (claimId == null || ClaimEvents.recruitsClaimManager == null) return null;
        RecruitsClaim claim = ClaimEvents.recruitsClaimManager.getClaim(claimId);
        if (claim == null || claim.isRemoved) return null;
        return claim;
    }

    /** Claims only exist in the overworld. */
    @Nullable
    public static RecruitsClaim getClaimAt(Level level, BlockPos pos) {
        if (ClaimEvents.recruitsClaimManager == null || level.dimension() != Level.OVERWORLD) return null;
        RecruitsClaim claim = ClaimEvents.recruitsClaimManager.getClaim(new ChunkPos(pos));
        if (claim == null || claim.isRemoved) return null;
        return claim;
    }

    public static boolean isInClaim(RecruitsClaim claim, BlockPos pos) {
        return claim.containsChunk(new ChunkPos(pos));
    }

    @Nullable
    public static UUID getClaimOwner(RecruitsClaim claim) {
        RecruitsPlayerInfo info = claim.getPlayerInfo();
        return info == null ? null : info.getUUID();
    }

    public static boolean isClaimOwner(RecruitsClaim claim, @Nullable UUID player) {
        UUID owner = getClaimOwner(claim);
        return owner != null && owner.equals(player);
    }

    /** Works for offline players: uses the member list of the faction. */
    public static boolean isFactionMember(@Nullable String factionId, @Nullable UUID player) {
        if (factionId == null || player == null || FactionEvents.recruitsFactionManager == null) return false;
        RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(factionId);
        if (faction == null) return false;
        if (player.equals(faction.getTeamLeaderUUID())) return true;
        for (RecruitsPlayerInfo member : faction.getMembers()) {
            if (player.equals(member.getUUID())) return true;
        }
        return false;
    }

    public static boolean isFactionLeader(@Nullable String factionId, @Nullable UUID player) {
        if (factionId == null || player == null || FactionEvents.recruitsFactionManager == null) return false;
        RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(factionId);
        return faction != null && player.equals(faction.getTeamLeaderUUID());
    }

    public static int getGarrisonCap(RecruitsClaim claim) {
        return claim.getClaimedChunks().size() * RecruitsServerConfig.GovernorRecruitsPerChunk.get();
    }

    /** Bounding box of all claimed chunks, full build height, inflated horizontally. */
    public static AABB getClaimBox(Level level, RecruitsClaim claim, double inflate) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (ChunkPos chunk : claim.getClaimedChunks()) {
            minX = Math.min(minX, chunk.getMinBlockX());
            minZ = Math.min(minZ, chunk.getMinBlockZ());
            maxX = Math.max(maxX, chunk.getMaxBlockX() + 1);
            maxZ = Math.max(maxZ, chunk.getMaxBlockZ() + 1);
        }
        if (minX == Integer.MAX_VALUE) return new AABB(0, 0, 0, 0, 0, 0);
        return new AABB(minX - inflate, level.getMinBuildHeight(), minZ - inflate,
                maxX + inflate, level.getMaxBuildHeight(), maxZ + inflate);
    }

    public static RuleStatus checkRule(@Nullable ClaimGovernorData data, @Nullable RecruitsClaim claim, UUID governor, @Nullable UUID owner) {
        if (claim == null || data == null) return RuleStatus.NO_CLAIM;
        if (data.getGovernorId() != null && !data.getGovernorId().equals(governor)) return RuleStatus.REPLACED;
        if (owner == null || !isClaimOwner(claim, owner)) return RuleStatus.NOT_CLAIM_OWNER;
        if (claim.getOwnerFaction() == null || !isFactionMember(claim.getOwnerFactionStringID(), owner)) return RuleStatus.NOT_IN_FACTION;
        return RuleStatus.RULING;
    }

    /**
     * Server-side access check for every Governor packet:
     * sender is the owner of the claim, in the overworld and in or right next to the claim.
     */
    @Nullable
    public static RecruitsClaim getManageableClaim(ServerPlayer player, @Nullable UUID claimId) {
        RecruitsClaim claim = getClaim(claimId);
        if (claim == null) return null;
        if (player.level().dimension() != Level.OVERWORLD) return null;
        if (!isClaimOwner(claim, player.getUUID())) return null;

        ChunkPos playerChunk = player.chunkPosition();
        for (ChunkPos chunk : claim.getClaimedChunks()) {
            if (Math.abs(chunk.x - playerChunk.x) <= 2 && Math.abs(chunk.z - playerChunk.z) <= 2) return claim;
        }
        return null;
    }

    // ------------------------------------------------------------------ garrison

    public static List<AbstractRecruitEntity> findGarrison(ServerLevel level, RecruitsClaim claim, @Nullable UUID owner) {
        UUID claimId = claim.getUUID();
        return level.getEntitiesOfClass(AbstractRecruitEntity.class, getClaimBox(level, claim, 32),
                recruit -> GovernorGarrison.isGarrisonOf(recruit, claimId, owner));
    }

    public static void unassign(ClaimGovernorData data, UUID recruit) {
        for (GovernorPost post : data.getPosts()) {
            if (recruit.equals(post.getAssigned())) post.setAssigned(null);
        }
        for (ClaimGovernorData.Patrol patrol : data.patrols) {
            patrol.members.remove(recruit);
        }
    }

    // ------------------------------------------------------------------ village

    public static List<Villager> findVillagers(ServerLevel level, RecruitsClaim claim) {
        return level.getEntitiesOfClass(Villager.class, getClaimBox(level, claim, 0),
                villager -> villager.isAlive() && !villager.isBaby() && isInClaim(claim, villager.blockPosition()));
    }

    /** Village bells (meeting points) inside the claim. */
    public static List<BlockPos> findBells(ServerLevel level, RecruitsClaim claim) {
        List<BlockPos> bells = new ArrayList<>();
        if (claim.getClaimedChunks().isEmpty()) return bells;
        AABB box = getClaimBox(level, claim, 0);
        BlockPos center = BlockPos.containing(box.getCenter().x, 64, box.getCenter().z);
        int radius = (int) Math.ceil(Math.max(box.getXsize(), box.getZsize()) / 2D * 1.5D);
        level.getPoiManager().findAll(
                holder -> holder.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.MEETING),
                pos -> isInClaim(claim, pos),
                center, radius,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY
        ).forEach(pos -> bells.add(pos.immutable()));
        return bells;
    }

    // ------------------------------------------------------------------ notifications

    public static void notifyPlayer(ServerLevel level, @Nullable UUID player, Component message) {
        if (player == null) return;
        Player online = level.getServer().getPlayerList().getPlayer(player);
        if (online != null) online.sendSystemMessage(message);
    }

    // ------------------------------------------------------------------ client snapshot

    /** Snapshot of a claim (command screen tab, answers to edits). Includes its Governor if he is loaded. */
    public static void sendSnapshot(ServerPlayer player, @Nullable UUID claimId, boolean openScreen) {
        GovernorManager manager = get();
        RecruitsClaim claim = getClaim(claimId);
        if (manager == null || claim == null) return;
        ServerLevel level = player.server.overworld();
        ClaimGovernorData data = manager.getOrCreate(claim.getUUID());
        GovernorEntity self = null;
        if (data.getGovernorId() != null && level.getEntity(data.getGovernorId()) instanceof GovernorEntity governor) self = governor;
        CompoundTag snapshot = buildSnapshot(level, claim, data, self, player.getUUID());
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MessageToClientGovernorData(snapshot, openScreen));
    }

    /** Snapshot of one Governor (Governor screen), also when he has no office. */
    public static void sendGovernorSnapshot(ServerPlayer player, GovernorEntity governor, boolean openScreen) {
        GovernorManager manager = get();
        if (manager == null) return;
        ServerLevel level = player.server.overworld();
        RecruitsClaim claim = getClaim(governor.getGovernedClaim());
        ClaimGovernorData data = claim == null ? null : manager.getOrCreate(claim.getUUID());
        CompoundTag snapshot = buildSnapshot(level, claim, data, governor, player.getUUID());
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MessageToClientGovernorData(snapshot, openScreen));
    }

    public static CompoundTag buildSnapshot(ServerLevel level, @Nullable RecruitsClaim claim, @Nullable ClaimGovernorData data,
                                            @Nullable GovernorEntity self, @Nullable UUID viewer) {
        CompoundTag tag = new CompoundTag();
        long day = level.getDayTime() / 24000L;

        if (self != null) {
            tag.putUUID("self", self.getUUID());
            tag.putString("selfName", self.getName().getString());
            tag.putBoolean("selfInClaim", claim != null && isInClaim(claim, self.blockPosition()));
            tag.putInt("purse", self.getPurse());
        }

        // claims of the viewer with their Governor, for the claim selection list
        if (viewer != null && ClaimEvents.recruitsClaimManager != null) {
            GovernorManager manager = get();
            ListTag own = new ListTag();
            for (RecruitsClaim ownClaim : ClaimEvents.recruitsClaimManager.getAllClaims()) {
                if (ownClaim == null || ownClaim.isRemoved || !isClaimOwner(ownClaim, viewer)) continue;
                CompoundTag entry = new CompoundTag();
                entry.putUUID("claim", ownClaim.getUUID());
                ClaimGovernorData ownData = manager == null ? null : manager.getData(ownClaim.getUUID());
                if (ownData != null && ownData.hasGovernor()) {
                    entry.putUUID("governor", ownData.getGovernorId());
                    entry.putString("governorName", ownData.getGovernorName());
                }
                own.add(entry);
            }
            tag.put("ownClaims", own);
        }

        if (claim == null || data == null) return tag;

        tag.putUUID("claim", claim.getUUID());
        tag.putString("claimName", claim.getName());
        RecruitsPlayerInfo ownerInfo = claim.getPlayerInfo();
        tag.putString("ownerName", ownerInfo != null ? ownerInfo.getName() : "");
        if (ownerInfo != null) tag.putUUID("owner", ownerInfo.getUUID());
        RecruitsFaction faction = claim.getOwnerFaction();
        tag.putString("factionName", faction != null ? faction.getTeamDisplayName() : "");
        tag.putInt("chunkCount", claim.getClaimedChunks().size());
        tag.putInt("cap", getGarrisonCap(claim));
        tag.putBoolean("underSiege", claim.isUnderSiege);

        ListTag chunks = new ListTag();
        for (ChunkPos chunk : claim.getClaimedChunks()) chunks.add(LongTag.valueOf(chunk.toLong()));
        tag.put("chunks", chunks);

        // governor
        boolean ruling = false;
        String status = "none";
        if (data.getGovernorId() != null) {
            tag.putUUID("governor", data.getGovernorId());
            tag.putString("governorName", data.getGovernorName());
            Entity entity = level.getEntity(data.getGovernorId());
            if (entity instanceof GovernorEntity governor) {
                RuleStatus ruleStatus = checkRule(data, claim, governor.getUUID(), governor.getOwnerUUID());
                ruling = ruleStatus == RuleStatus.RULING;
                status = ruleStatus.getKey();
                tag.putInt("purse", governor.getPurse());
            } else {
                // unloaded but still registered: treat as ruling if the owner still matches
                RuleStatus ruleStatus = checkRule(data, claim, data.getGovernorId(), data.getGovernorOwner());
                ruling = ruleStatus == RuleStatus.RULING;
                status = ruleStatus.getKey();
            }
        }
        tag.putBoolean("ruling", ruling);
        tag.putString("status", status);

        // posts
        ListTag posts = new ListTag();
        for (GovernorPost post : data.getPosts()) {
            CompoundTag postTag = post.toNBT();
            if (post.getAssigned() != null) {
                Entity assigned = level.getEntity(post.getAssigned());
                if (assigned != null) postTag.putString("assignedName", assigned.getName().getString());
            }
            posts.add(postTag);
        }
        tag.put("posts", posts);

        // treasury
        BlockPos treasuryPos = data.getTreasuryPos();
        int coins = -1;
        boolean treasuryMissing = false;
        if (treasuryPos != null && level.isLoaded(treasuryPos)) {
            Container container = GovernorGarrison.getContainer(level, treasuryPos);
            if (container != null) coins = GovernorGarrison.countCurrency(container);
            else treasuryMissing = true;
        }
        tag.putBoolean("hasTreasury", treasuryPos != null);
        tag.putInt("coins", coins);
        tag.putBoolean("treasuryMissing", treasuryMissing);
        boolean upkeepMissing = data.getPosts(GovernorPost.Type.UPKEEP).isEmpty();
        for (GovernorPost upkeep : data.getPosts(GovernorPost.Type.UPKEEP)) {
            if (level.isLoaded(upkeep.getPos()) && !GovernorGarrison.isContainer(level, upkeep.getPos())) upkeepMissing = true;
        }
        tag.putBoolean("upkeepMissing", upkeepMissing);

        // garrison
        int[] counts = new int[GovernorUnitType.COUNT];
        UUID garrisonOwner = data.getGovernorOwner() != null ? data.getGovernorOwner() : getClaimOwner(claim);
        int total = 0;
        if (garrisonOwner != null) {
            for (AbstractRecruitEntity recruit : findGarrison(level, claim, garrisonOwner)) {
                GovernorUnitType type = GovernorUnitType.of(recruit);
                if (type != null) {
                    counts[type.getIndex()]++;
                    total++;
                }
            }
        }
        int[] quota = new int[GovernorUnitType.COUNT];
        CompoundTag loadout = new CompoundTag();
        for (GovernorUnitType type : GovernorUnitType.values()) {
            quota[type.getIndex()] = data.getQuota(type);
            ListTag slots = new ListTag();
            for (int i = 0; i < ClaimGovernorData.LOADOUT_SLOTS; i++) {
                slots.add(net.minecraft.nbt.StringTag.valueOf(data.getLoadout(type, i)));
            }
            loadout.put(String.valueOf(type.getIndex()), slots);
        }
        tag.putIntArray("counts", counts);
        tag.putIntArray("quota", quota);
        tag.putInt("garrisonTotal", total);
        tag.put("loadout", loadout);
        tag.putInt("budget", data.getBudgetPerDay());
        tag.putInt("spentToday", data.getSpentToday(day));

        // village
        List<BlockPos> bells = findBells(level, claim);
        tag.putBoolean("village", !bells.isEmpty());
        tag.putInt("inhabitants", bells.isEmpty() ? 0 : findVillagers(level, claim).size());
        tag.putInt("taxLevel", data.getTaxLevel());
        tag.putInt("taxToday", data.getTaxToday(day));
        tag.putIntArray("taxPenalty", new int[]{0,
                RecruitsServerConfig.GovernorTaxReputationPenaltyLow.get(),
                RecruitsServerConfig.GovernorTaxReputationPenaltyMedium.get(),
                RecruitsServerConfig.GovernorTaxReputationPenaltyHigh.get()});
        tag.putInt("taxBase", RecruitsServerConfig.GovernorTaxPer10Inhabitants.get());
        return tag;
    }
}
