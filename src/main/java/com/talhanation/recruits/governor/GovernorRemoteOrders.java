package com.talhanation.recruits.governor;

import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Orders for a far Governor, carried by a Messenger (see {@code CompanionOrders}).
 * <p>
 * What a Governor manages is stored per claim ({@link ClaimGovernorData}), not on his entity. So while the owner
 * writes orders at his Messenger, the Governor screen works on a copy of that data: the normal packets change the
 * copy ({@link GovernorPackets#access}) and the screen shows it. When the Messenger is sent off he takes along
 * what differs from the real data ({@link #collect}), and hands it over when he arrives ({@link #apply}). Nothing
 * has to be loaded for this, the Governor may stand in an unloaded claim.
 * <p>
 * Carried are the settings of the Governor screen: taxes, quests, budget, units with their equipment, and removed
 * posts. What needs the place itself - new posts, handing over a group, releasing a recruit, the office - is done
 * on site as before.
 */
public final class GovernorRemoteOrders {
    private static final String CLAIM = "Claim";
    private static final String TAX = "Tax";
    private static final String QUEST_MODE = "QuestMode";
    private static final String BUDGET = "Budget";
    private static final String QUOTAS = "Quotas";
    private static final String LOADOUTS = "Loadouts";
    private static final String REMOVED_POSTS = "RemovedPosts";

    private GovernorRemoteOrders() {
    }

    /** The copy of a claim's Governor data the owner is changing at his Messenger. */
    public static final class Draft {
        public final UUID claim;
        public final UUID governor;
        /** The data as it was when the draft was opened, to tell afterwards what the owner changed. */
        private final CompoundTag base;
        public final ClaimGovernorData data;

        private Draft(UUID claim, UUID governor, CompoundTag base) {
            this.claim = claim;
            this.governor = governor;
            this.base = base;
            this.data = ClaimGovernorData.fromNBT(base.copy());
        }
    }

    /** @return a draft for the claim this Governor is in office of, null if he has none or it is not the player's */
    @Nullable
    public static Draft begin(ServerPlayer player, UUID governorId) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null) return null;
        for (ClaimGovernorData data : manager.getAll()) {
            if (!governorId.equals(data.getGovernorId())) continue;
            RecruitsClaim claim = GovernorManager.getClaim(data.getClaimId());
            if (claim == null || !GovernorManager.isClaimOwner(claim, player.getUUID())) return null;
            return new Draft(data.getClaimId(), governorId, data.toNBT());
        }
        return null;
    }

    /** The claim as the Governor screen shows it, with the changes of the draft. Null if the claim is gone. */
    @Nullable
    public static CompoundTag snapshot(ServerPlayer player, Draft draft) {
        RecruitsClaim claim = GovernorManager.getClaim(draft.claim);
        if (claim == null) return null;
        ServerLevel level = player.server.overworld();
        GovernorEntity self = level.getEntity(draft.governor) instanceof GovernorEntity governor ? governor : null;
        CompoundTag tag = GovernorManager.buildSnapshot(level, claim, draft.data, self, player.getUUID());
        if (self == null) {
            // his chunk is not loaded: the screen still needs to know whose it is
            tag.putUUID("self", draft.governor);
            tag.putString("selfName", draft.data.getGovernorName());
            tag.putBoolean("selfInClaim", true);
        }
        return tag;
    }

    /** @return what the owner changed in the draft, null if nothing */
    @Nullable
    public static CompoundTag collect(Draft draft) {
        ClaimGovernorData before = ClaimGovernorData.fromNBT(draft.base.copy());
        ClaimGovernorData after = draft.data;
        CompoundTag orders = new CompoundTag();

        if (after.getTaxLevel() != before.getTaxLevel()) orders.putInt(TAX, after.getTaxLevel());
        if (after.getQuestMode() != before.getQuestMode()) orders.putInt(QUEST_MODE, after.getQuestMode());
        if (after.getBudgetPerDay() != before.getBudgetPerDay()) orders.putInt(BUDGET, after.getBudgetPerDay());

        CompoundTag quotas = new CompoundTag();
        CompoundTag loadouts = new CompoundTag();
        for (GovernorUnitType type : GovernorUnitType.values()) {
            if (after.getQuota(type) != before.getQuota(type)) quotas.putInt(String.valueOf(type.getIndex()), after.getQuota(type));
            for (int slot = 0; slot < ClaimGovernorData.LOADOUT_SLOTS; slot++) {
                String item = after.getLoadout(type, slot);
                if (!item.equals(before.getLoadout(type, slot))) loadouts.putString(type.getIndex() + ":" + slot, item);
            }
        }
        if (!quotas.isEmpty()) orders.put(QUOTAS, quotas);
        if (!loadouts.isEmpty()) orders.put(LOADOUTS, loadouts);

        ListTag removed = new ListTag();
        for (GovernorPost post : before.getPosts()) {
            if (after.getPost(post.getId()) == null) removed.add(NbtUtils.createUUID(post.getId()));
        }
        if (!removed.isEmpty()) orders.put(REMOVED_POSTS, removed);

        if (orders.isEmpty()) return null;
        orders.putUUID(CLAIM, draft.claim);
        return orders;
    }

    /**
     * The Messenger hands the orders over: they are carried out on the real data of the claim, with the same
     * limits as the packets of the Governor screen.
     * @return false if this Governor is not in office of the claim any more, or the claim is not the owner's
     */
    public static boolean apply(UUID owner, UUID governorId, CompoundTag orders) {
        GovernorManager manager = GovernorManager.get();
        if (manager == null || !orders.hasUUID(CLAIM)) return false;
        UUID claimId = orders.getUUID(CLAIM);
        RecruitsClaim claim = GovernorManager.getClaim(claimId);
        ClaimGovernorData data = manager.getData(claimId);
        if (claim == null || data == null || !governorId.equals(data.getGovernorId())) return false;
        if (!GovernorManager.isClaimOwner(claim, owner)) return false;

        if (orders.contains(TAX)) data.setTaxLevel(orders.getInt(TAX));
        if (orders.contains(QUEST_MODE)) data.setQuestMode(orders.getInt(QUEST_MODE));
        if (orders.contains(BUDGET)) data.setBudgetPerDay(Math.max(0, Math.min(orders.getInt(BUDGET), 100000)));

        CompoundTag quotas = orders.getCompound(QUOTAS);
        CompoundTag loadouts = orders.getCompound(LOADOUTS);
        for (GovernorUnitType type : GovernorUnitType.values()) {
            String key = String.valueOf(type.getIndex());
            if (quotas.contains(key)) {
                // the garrison may have changed since the orders were written: never more than there is room for
                int others = data.getTotalQuota() - data.getQuota(type);
                int max = Math.max(0, GovernorManager.getGarrisonCap(claim) - others);
                data.setQuota(type, Math.max(0, Math.min(quotas.getInt(key), max)));
            }
            for (int slot = 0; slot < ClaimGovernorData.LOADOUT_SLOTS; slot++) {
                String slotKey = type.getIndex() + ":" + slot;
                if (!loadouts.contains(slotKey)) continue;
                String item = loadouts.getString(slotKey);
                if (item.isEmpty() || GovernorGarrison.getItem(item) != null) data.setLoadout(type, slot, item);
            }
        }

        ListTag removed = orders.getList(REMOVED_POSTS, Tag.TAG_INT_ARRAY);
        for (Tag tag : removed) data.removePost(NbtUtils.loadUUID(tag));
        if (!removed.isEmpty()) {
            // as in MessageGovernorRemovePost: patrols skip the removed points
            data.patrols.forEach(patrol -> patrol.removeMissingPoints(data));
            data.patrols.removeIf(patrol -> patrol.getCurrentPoint() == null);
        }
        return true;
    }
}
