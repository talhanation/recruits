package com.talhanation.recruits.governor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Everything a Governor manages, stored per claim (not on the Governor entity).
 * When a Governor dies or stops ruling, this stays and his successor takes it over.
 * Treasury and posts also stay when the claim changes owner (the treasury is loot).
 */
public class ClaimGovernorData {

    /** Loadout slot order: main hand, off hand, head, chest, legs, feet. */
    public static final int LOADOUT_SLOTS = 6;

    private final UUID claimId;
    @Nullable
    private UUID governorId;
    @Nullable
    private UUID governorOwner;
    private String governorName = "";

    private final List<GovernorPost> posts = new ArrayList<>();
    private final int[] quota = new int[GovernorUnitType.COUNT];
    private final String[][] loadout = new String[GovernorUnitType.COUNT][LOADOUT_SLOTS];

    private int budgetPerDay;
    private int spentToday;
    private long budgetDay = -1;

    private int taxLevel;
    private int taxToday;
    private long lastTaxDay = -1;

    /** When the Governor takes village quests for his owner, see {@link #wantsQuest(int)}. 0 = never. */
    private int questMode;
    private long lastQuestDay = -1;
    /** Level of the Governor in office, for the garrison limit while he is not loaded. */
    private int governorLevel;

    // one-time notifications ("einmal melden")
    public boolean treasuryMissingNotified;
    public boolean upkeepMissingNotified;
    public boolean siegeNotified;

    // runtime only
    public final transient List<Patrol> patrols = new ArrayList<>();
    @Nullable
    public transient int lastKnownTreasury = -1;
    public transient boolean treasuryMissing;
    public transient boolean upkeepMissing;
    /** Why the Governor does (not) recruit right now, shown in the Info tab. Key below gui.recruits.governor.recruit_status. */
    public transient String recruitStatus = "idle";
    public transient String recruitStatusArg = "";

    public ClaimGovernorData(UUID claimId) {
        this.claimId = claimId;
        for (String[] slots : loadout) Arrays.fill(slots, "");
    }

    // ------------------------------------------------------------------ governor

    public UUID getClaimId() {
        return claimId;
    }

    @Nullable
    public UUID getGovernorId() {
        return governorId;
    }

    @Nullable
    public UUID getGovernorOwner() {
        return governorOwner;
    }

    public String getGovernorName() {
        return governorName;
    }

    public void setGovernor(@Nullable UUID governorId, @Nullable UUID owner, String name) {
        this.governorId = governorId;
        this.governorOwner = owner;
        this.governorName = name == null ? "" : name;
        this.patrols.clear();
    }

    public void clearGovernor() {
        setGovernor(null, null, "");
        this.governorLevel = 0;
    }

    public boolean hasGovernor() {
        return governorId != null;
    }

    // ------------------------------------------------------------------ posts

    public List<GovernorPost> getPosts() {
        return posts;
    }

    public List<GovernorPost> getPosts(GovernorPost.Type type) {
        List<GovernorPost> list = new ArrayList<>();
        for (GovernorPost post : posts) if (post.getType() == type) list.add(post);
        return list;
    }

    @Nullable
    public GovernorPost getPost(UUID id) {
        for (GovernorPost post : posts) if (post.getId().equals(id)) return post;
        return null;
    }

    @Nullable
    public GovernorPost getTreasury() {
        for (GovernorPost post : posts) if (post.getType() == GovernorPost.Type.TREASURY) return post;
        return null;
    }

    @Nullable
    public BlockPos getTreasuryPos() {
        GovernorPost treasury = getTreasury();
        return treasury == null ? null : treasury.getPos();
    }

    public GovernorPost addPost(GovernorPost.Type type, BlockPos pos) {
        if (type == GovernorPost.Type.TREASURY) {
            posts.removeIf(p -> p.getType() == GovernorPost.Type.TREASURY);
            treasuryMissingNotified = false;
        }
        int number = 1;
        for (GovernorPost post : posts) {
            if (post.getType() == type) number = Math.max(number, post.getNumber() + 1);
        }
        GovernorPost post = new GovernorPost(type, pos, number);
        posts.add(post);
        return post;
    }

    @Nullable
    public GovernorPost removePost(UUID id) {
        GovernorPost post = getPost(id);
        if (post != null) posts.remove(post);
        return post;
    }

    @Nullable
    public GovernorPost getAssignedPost(UUID recruit) {
        for (GovernorPost post : posts) {
            if (recruit.equals(post.getAssigned())) return post;
        }
        return null;
    }

    @Nullable
    public GovernorPost getNearest(GovernorPost.Type type, Vec3 from) {
        GovernorPost best = null;
        double bestDist = Double.MAX_VALUE;
        for (GovernorPost post : posts) {
            if (post.getType() != type) continue;
            double dist = post.getStandPos().distanceToSqr(from);
            if (dist < bestDist) {
                bestDist = dist;
                best = post;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ garrison settings

    public int getQuota(GovernorUnitType type) {
        return quota[type.getIndex()];
    }

    public void setQuota(GovernorUnitType type, int amount) {
        quota[type.getIndex()] = Math.max(0, amount);
    }

    public int getTotalQuota() {
        int sum = 0;
        for (int q : quota) sum += q;
        return sum;
    }

    /** "" = this slot is not managed ("egal"). */
    public String getLoadout(GovernorUnitType type, int slot) {
        if (slot < 0 || slot >= LOADOUT_SLOTS) return "";
        String value = loadout[type.getIndex()][slot];
        return value == null ? "" : value;
    }

    public void setLoadout(GovernorUnitType type, int slot, String itemId) {
        if (slot < 0 || slot >= LOADOUT_SLOTS) return;
        loadout[type.getIndex()][slot] = itemId == null ? "" : itemId;
    }

    public int getBudgetPerDay() {
        return budgetPerDay;
    }

    public void setBudgetPerDay(int budget) {
        this.budgetPerDay = Math.max(0, budget);
    }

    public int getSpentToday(long day) {
        rollDay(day);
        return spentToday;
    }

    public int getRemainingBudget(long day) {
        rollDay(day);
        return Math.max(0, budgetPerDay - spentToday);
    }

    public void addSpent(long day, int amount) {
        rollDay(day);
        spentToday = Math.max(0, spentToday + amount);
    }

    private void rollDay(long day) {
        if (day != budgetDay) {
            budgetDay = day;
            spentToday = 0;
        }
    }

    // ------------------------------------------------------------------ taxes

    public int getTaxLevel() {
        return taxLevel;
    }

    public void setTaxLevel(int level) {
        this.taxLevel = Math.max(0, Math.min(3, level));
    }

    public long getLastTaxDay() {
        return lastTaxDay;
    }

    public void setLastTaxDay(long day) {
        this.lastTaxDay = day;
    }

    public int getTaxToday(long day) {
        return lastTaxDay == day ? taxToday : 0;
    }

    public void setTaxToday(int amount) {
        this.taxToday = amount;
    }

    // ------------------------------------------------------------------ village quests

    public static final int QUEST_MODES = 4;
    /** Reputation thresholds of the quest modes: below this effective reputation the Governor takes quests. */
    private static final int[] QUEST_THRESHOLDS = {Integer.MIN_VALUE, 0, 25, 75};

    public int getQuestMode() {
        return questMode;
    }

    public void setQuestMode(int mode) {
        this.questMode = Math.max(0, Math.min(QUEST_MODES - 1, mode));
    }

    /** True if the Governor should take a quest at this effective reputation of his owner. */
    public boolean wantsQuest(int reputation) {
        return questMode > 0 && reputation < QUEST_THRESHOLDS[questMode];
    }

    public long getLastQuestDay() {
        return lastQuestDay;
    }

    public void setLastQuestDay(long day) {
        this.lastQuestDay = day;
    }

    public int getGovernorLevel() {
        return governorLevel;
    }

    public void setGovernorLevel(int level) {
        this.governorLevel = Math.max(0, level);
    }

    public void setRecruitStatus(String status, Object arg) {
        this.recruitStatus = status;
        this.recruitStatusArg = arg == null ? "" : String.valueOf(arg);
    }

    // ------------------------------------------------------------------ NBT

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("claim", claimId);
        if (governorId != null) tag.putUUID("governor", governorId);
        if (governorOwner != null) tag.putUUID("governorOwner", governorOwner);
        tag.putString("governorName", governorName);

        ListTag postList = new ListTag();
        for (GovernorPost post : posts) postList.add(post.toNBT());
        tag.put("posts", postList);

        tag.putIntArray("quota", quota);

        CompoundTag loadoutTag = new CompoundTag();
        for (GovernorUnitType type : GovernorUnitType.values()) {
            ListTag slots = new ListTag();
            for (int i = 0; i < LOADOUT_SLOTS; i++) {
                slots.add(net.minecraft.nbt.StringTag.valueOf(getLoadout(type, i)));
            }
            loadoutTag.put(String.valueOf(type.getIndex()), slots);
        }
        tag.put("loadout", loadoutTag);

        tag.putInt("budget", budgetPerDay);
        tag.putInt("spent", spentToday);
        tag.putLong("budgetDay", budgetDay);
        tag.putInt("taxLevel", taxLevel);
        tag.putInt("taxToday", taxToday);
        tag.putLong("lastTaxDay", lastTaxDay);
        tag.putInt("questMode", questMode);
        tag.putLong("lastQuestDay", lastQuestDay);
        tag.putInt("governorLevel", governorLevel);
        tag.putBoolean("treasuryNotified", treasuryMissingNotified);
        tag.putBoolean("upkeepNotified", upkeepMissingNotified);
        tag.putBoolean("siegeNotified", siegeNotified);
        return tag;
    }

    public static ClaimGovernorData fromNBT(CompoundTag tag) {
        ClaimGovernorData data = new ClaimGovernorData(tag.getUUID("claim"));
        data.governorId = tag.hasUUID("governor") ? tag.getUUID("governor") : null;
        data.governorOwner = tag.hasUUID("governorOwner") ? tag.getUUID("governorOwner") : null;
        data.governorName = tag.getString("governorName");

        ListTag postList = tag.getList("posts", Tag.TAG_COMPOUND);
        for (int i = 0; i < postList.size(); i++) {
            data.posts.add(GovernorPost.fromNBT(postList.getCompound(i)));
        }

        int[] savedQuota = tag.getIntArray("quota");
        for (int i = 0; i < Math.min(savedQuota.length, data.quota.length); i++) data.quota[i] = Math.max(0, savedQuota[i]);

        CompoundTag loadoutTag = tag.getCompound("loadout");
        for (GovernorUnitType type : GovernorUnitType.values()) {
            String key = String.valueOf(type.getIndex());
            if (!loadoutTag.contains(key, Tag.TAG_LIST)) continue;
            ListTag slots = loadoutTag.getList(key, Tag.TAG_STRING);
            for (int i = 0; i < Math.min(slots.size(), LOADOUT_SLOTS); i++) {
                data.setLoadout(type, i, slots.getString(i));
            }
        }

        data.budgetPerDay = tag.getInt("budget");
        data.spentToday = tag.getInt("spent");
        data.budgetDay = tag.contains("budgetDay") ? tag.getLong("budgetDay") : -1;
        data.taxLevel = tag.getInt("taxLevel");
        data.taxToday = tag.getInt("taxToday");
        data.lastTaxDay = tag.contains("lastTaxDay") ? tag.getLong("lastTaxDay") : -1;
        data.setQuestMode(tag.getInt("questMode"));
        data.lastQuestDay = tag.contains("lastQuestDay") ? tag.getLong("lastQuestDay") : -1;
        data.governorLevel = tag.getInt("governorLevel");
        data.treasuryMissingNotified = tag.getBoolean("treasuryNotified");
        data.upkeepMissingNotified = tag.getBoolean("upkeepNotified");
        data.siegeNotified = tag.getBoolean("siegeNotified");
        return data;
    }

    /**
     * A few reserve recruits that walk through all patrol points of the claim (route in any order,
     * nearest point next), stay a moment at each and then return to the reserve. Runtime only.
     */
    public static class Patrol {
        public final List<UUID> members = new ArrayList<>();
        /** Patrol points still to visit, the first one is the current target. */
        public final List<UUID> route = new ArrayList<>();
        public final List<BlockPos> routePos = new ArrayList<>();
        public int dwellLeft;
        public boolean arrived;
        /** Ticks on the way to the current point (to skip a point nobody can reach). */
        public int legTicks;

        public Patrol(List<UUID> route, List<BlockPos> routePos) {
            this.route.addAll(route);
            this.routePos.addAll(routePos);
        }

        @Nullable
        public BlockPos getCurrentPoint() {
            return routePos.isEmpty() ? null : routePos.get(0);
        }

        /** Next point of the route; false if the route is done. */
        public boolean advance() {
            if (!route.isEmpty()) {
                route.remove(0);
                routePos.remove(0);
            }
            arrived = false;
            dwellLeft = 0;
            legTicks = 0;
            return !route.isEmpty();
        }

        /** Removes points that were deleted meanwhile. */
        public void removeMissingPoints(ClaimGovernorData data) {
            for (int i = route.size() - 1; i >= 0; i--) {
                if (data.getPost(route.get(i)) == null) {
                    if (i == 0) {
                        arrived = false;
                        legTicks = 0;
                    }
                    route.remove(i);
                    routePos.remove(i);
                }
            }
        }
    }
}
