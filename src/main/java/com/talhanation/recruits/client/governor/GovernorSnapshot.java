package com.talhanation.recruits.client.governor;

import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorPost;
import com.talhanation.recruits.governor.GovernorUnitType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;

/** Client view of the Governor data of one claim, built from the server snapshot. */
@OnlyIn(Dist.CLIENT)
public class GovernorSnapshot {

    /** Claim the snapshot is about; null for a Governor without office. */
    @Nullable
    public UUID claim;

    /** The Governor the screen is about (null if he is not loaded). */
    @Nullable
    public UUID self;
    public String selfName = "";
    public boolean selfInClaim;

    /** Claims of the viewing player: claim -> Governor in office (null = free). */
    public final Map<UUID, OwnClaim> ownClaims = new HashMap<>();

    public record OwnClaim(@Nullable UUID governor, String governorName) {
    }
    public String claimName = "";
    @Nullable
    public UUID owner;
    public String ownerName = "";
    public String factionName = "";
    public int chunkCount;
    public int cap;
    public boolean underSiege;
    public final List<ChunkPos> chunks = new ArrayList<>();

    @Nullable
    public UUID governor;
    public String governorName = "";
    public boolean ruling;
    public String status = "none";
    public int purse;

    public final List<GovernorPost> posts = new ArrayList<>();
    public final Map<UUID, String> assignedNames = new HashMap<>();

    public boolean hasTreasury;
    public int coins = -1;
    public boolean treasuryMissing;
    public boolean upkeepMissing;

    public int[] counts = new int[GovernorUnitType.COUNT];
    public int[] quota = new int[GovernorUnitType.COUNT];
    public int garrisonTotal;
    public final String[][] loadout = new String[GovernorUnitType.COUNT][ClaimGovernorData.LOADOUT_SLOTS];
    public int budget;
    public int spentToday;

    public boolean village;
    public int inhabitants;
    public int taxLevel;
    public int taxToday;
    public int[] taxPenalty = new int[4];
    public int taxBase;

    public static GovernorSnapshot fromNBT(CompoundTag tag) {
        GovernorSnapshot s = new GovernorSnapshot();
        s.self = tag.hasUUID("self") ? tag.getUUID("self") : null;
        s.selfName = tag.getString("selfName");
        s.selfInClaim = tag.getBoolean("selfInClaim");
        s.purse = tag.getInt("purse");
        ListTag own = tag.getList("ownClaims", Tag.TAG_COMPOUND);
        for (int i = 0; i < own.size(); i++) {
            CompoundTag entry = own.getCompound(i);
            if (!entry.hasUUID("claim")) continue;
            s.ownClaims.put(entry.getUUID("claim"), new OwnClaim(
                    entry.hasUUID("governor") ? entry.getUUID("governor") : null, entry.getString("governorName")));
        }

        s.claim = tag.hasUUID("claim") ? tag.getUUID("claim") : null;
        if (s.claim == null) return s;
        s.claimName = tag.getString("claimName");
        s.owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        s.ownerName = tag.getString("ownerName");
        s.factionName = tag.getString("factionName");
        s.chunkCount = tag.getInt("chunkCount");
        s.cap = tag.getInt("cap");
        s.underSiege = tag.getBoolean("underSiege");
        ListTag chunkList = tag.getList("chunks", Tag.TAG_LONG);
        for (Tag chunk : chunkList) {
            if (chunk instanceof LongTag longTag) s.chunks.add(new ChunkPos(longTag.getAsLong()));
        }

        s.governor = tag.hasUUID("governor") ? tag.getUUID("governor") : null;
        s.governorName = tag.getString("governorName");
        s.ruling = tag.getBoolean("ruling");
        s.status = tag.getString("status");

        ListTag postList = tag.getList("posts", Tag.TAG_COMPOUND);
        for (int i = 0; i < postList.size(); i++) {
            CompoundTag postTag = postList.getCompound(i);
            GovernorPost post = GovernorPost.fromNBT(postTag);
            s.posts.add(post);
            if (postTag.contains("assignedName")) s.assignedNames.put(post.getId(), postTag.getString("assignedName"));
        }

        s.hasTreasury = tag.getBoolean("hasTreasury");
        s.coins = tag.getInt("coins");
        s.treasuryMissing = tag.getBoolean("treasuryMissing");
        s.upkeepMissing = tag.getBoolean("upkeepMissing");

        copy(tag.getIntArray("counts"), s.counts);
        copy(tag.getIntArray("quota"), s.quota);
        s.garrisonTotal = tag.getInt("garrisonTotal");
        CompoundTag loadoutTag = tag.getCompound("loadout");
        for (GovernorUnitType type : GovernorUnitType.values()) {
            Arrays.fill(s.loadout[type.getIndex()], "");
            ListTag slots = loadoutTag.getList(String.valueOf(type.getIndex()), Tag.TAG_STRING);
            for (int i = 0; i < Math.min(slots.size(), ClaimGovernorData.LOADOUT_SLOTS); i++) {
                s.loadout[type.getIndex()][i] = slots.getString(i);
            }
        }
        s.budget = tag.getInt("budget");
        s.spentToday = tag.getInt("spentToday");

        s.village = tag.getBoolean("village");
        s.inhabitants = tag.getInt("inhabitants");
        s.taxLevel = tag.getInt("taxLevel");
        s.taxToday = tag.getInt("taxToday");
        copy(tag.getIntArray("taxPenalty"), s.taxPenalty);
        s.taxBase = tag.getInt("taxBase");
        return s;
    }

    private static void copy(int[] from, int[] to) {
        for (int i = 0; i < Math.min(from.length, to.length); i++) to[i] = from[i];
    }

    public List<GovernorPost> getPosts(GovernorPost.Type type) {
        List<GovernorPost> list = new ArrayList<>();
        for (GovernorPost post : posts) if (post.getType() == type) list.add(post);
        return list;
    }

    public boolean hasClaim() {
        return claim != null;
    }

    public int getTotalQuota() {
        int sum = 0;
        for (int q : quota) sum += q;
        return sum;
    }
}
