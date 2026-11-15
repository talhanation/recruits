package com.talhanation.recruits.api.governor;

import com.talhanation.recruits.VillageEvents;
import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorController;
import com.talhanation.recruits.governor.GovernorGarrison;
import com.talhanation.recruits.governor.GovernorHiring;
import com.talhanation.recruits.governor.GovernorManager;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsHireTrade;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsVillage;
import com.talhanation.recruits.world.RecruitsVillageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * What a module may see and do for one claim. A new context is made for every hook call, do not keep it.
 */
public class GovernorContext {

    private final ServerLevel level;
    private final RecruitsClaim claim;
    private final ClaimGovernorData data;
    private final ResourceLocation moduleId;
    @Nullable
    private final GovernorEntity governor;
    @Nullable
    private final GovernorController controller;

    public GovernorContext(ServerLevel level, RecruitsClaim claim, ClaimGovernorData data, ResourceLocation moduleId,
                           @Nullable GovernorEntity governor, @Nullable GovernorController controller) {
        this.level = level;
        this.claim = claim;
        this.data = data;
        this.moduleId = moduleId;
        this.governor = governor;
        this.controller = controller;
    }

    /** Same claim, for another module. */
    public GovernorContext forModule(ResourceLocation id) {
        return new GovernorContext(level, claim, data, id, governor, controller);
    }

    // ------------------------------------------------------------------ claim and owner

    public ServerLevel getLevel() {
        return level;
    }

    public RecruitsClaim getClaim() {
        return claim;
    }

    public UUID getClaimId() {
        return claim.getUUID();
    }

    public long getDay() {
        return level.getDayTime() / 24000L;
    }

    /** The owner the Governor works for, null if the claim has none. */
    @Nullable
    public UUID getOwner() {
        if (data.getGovernorOwner() != null) return data.getGovernorOwner();
        return GovernorManager.getClaimOwner(claim);
    }

    public String getOwnerName() {
        if (governor != null) return governor.getOwnerName();
        RecruitsPlayerInfo info = claim.getPlayerInfo();
        return info != null ? info.getName() : "";
    }

    public String getFactionId() {
        String id = claim.getOwnerFactionStringID();
        return id == null ? "" : id;
    }

    /** The Governor if he is loaded and rules, null in the simulation of an unloaded claim. */
    @Nullable
    public GovernorEntity getGovernor() {
        return governor;
    }

    /**
     * Sends the Governor to a place instead of his post, e.g. to a build site he marks out: he holds his position
     * there until it is cleared with null. His own tasks (withdraw, hire, taxes) come first. Only while he is loaded.
     */
    public void setVisitTarget(@Nullable BlockPos pos) {
        if (governor == null) return;
        governor.controller.setVisitTarget(pos == null ? null : net.minecraft.world.phys.Vec3.atBottomCenterOf(pos));
    }

    public boolean isLoaded() {
        return governor != null;
    }

    /** Sends a message to the owner right away (if online). For the daily summary use the report instead. */
    public void notifyOwner(Component message) {
        GovernorManager.notifyPlayer(level, getOwner(), message);
    }

    // ------------------------------------------------------------------ module data

    /** Data of this module stored with the claim's Governor data. Changes are saved with the world. */
    public CompoundTag getModuleData() {
        return data.getModuleData(moduleId);
    }

    /**
     * Unit types this module needs but can not hire in the claim: no free unit stands in it and no noble offers them.
     * When a village of the claim elects its next noble, it prefers a trader type that offers them.
     * Replaces the module's last list, an empty list clears it.
     */
    public void setWantedUnits(Collection<ResourceLocation> entityIds) {
        data.setWantedUnits(moduleId, entityIds);
    }

    // ------------------------------------------------------------------ treasury and budget

    @Nullable
    public BlockPos getTreasuryPos() {
        return data.getTreasuryPos();
    }

    /** Coins in the treasury at the last check (the Governor counts them every few seconds). */
    public int getTreasuryCoins() {
        return Math.max(0, data.lastKnownTreasury);
    }

    /** What the Governor may still spend today (daily budget minus what he spent). */
    public int getRemainingBudget() {
        return data.getRemainingBudget(getDay());
    }

    /** The currency of the treasury (the Recruits currency). */
    public net.minecraft.world.item.Item getCurrency() {
        return GovernorGarrison.getCurrency();
    }

    /**
     * Takes currency out of the treasury to pass it on, e.g. into the market for the merchant. Counts as spending
     * of the day: at most the remaining budget, only while the treasury is loaded.
     * @return how much was taken, the caller puts it somewhere (or back with {@link #deposit})
     */
    public int withdraw(int amount) {
        amount = Math.min(amount, getRemainingBudget());
        if (amount <= 0) return 0;
        BlockPos pos = data.getTreasuryPos();
        if (pos == null || !level.isLoaded(pos)) return 0;
        Container container = GovernorGarrison.getContainer(level, pos);
        if (container == null) return 0;
        int removed = GovernorGarrison.removeCurrency(container, amount);
        data.lastKnownTreasury = GovernorGarrison.countCurrency(container);
        if (removed > 0) data.addSpent(getDay(), removed);
        return removed;
    }

    /** Puts currency back into the treasury (e.g. what did not fit into the market), it no longer counts as spent. */
    public int deposit(int amount) {
        if (amount <= 0) return 0;
        BlockPos pos = data.getTreasuryPos();
        if (pos == null || !level.isLoaded(pos)) return amount;
        Container container = GovernorGarrison.getContainer(level, pos);
        if (container == null) return amount;
        int left = GovernorGarrison.addCurrency(container, amount);
        data.lastKnownTreasury = GovernorGarrison.countCurrency(container);
        data.addSpent(getDay(), -(amount - left));
        return left;
    }

    /**
     * Puts part of today's budget aside without paying it, e.g. to save up for a building that costs more than one
     * day's budget. The coins stay in the treasury; pay them later with {@link #spend(int, int)}.
     * @return how much was put aside (at most the remaining budget)
     */
    public int reserveBudget(int amount) {
        amount = Math.min(amount, getRemainingBudget());
        if (amount <= 0) return 0;
        data.addSpent(getDay(), amount);
        return amount;
    }

    /**
     * Pays from the treasury, e.g. the price of a building. Counts as spending of the day, so only within the
     * remaining budget, and only while the treasury chest is loaded.
     * @return true if the whole amount was paid, false if nothing was paid
     */
    public boolean spend(int amount) {
        return spend(amount, 0);
    }

    /**
     * Like {@link #spend(int)}, but {@code saved} of the amount was put aside on earlier days ({@link #reserveBudget})
     * and does not count against today's budget again.
     */
    public boolean spend(int amount, int saved) {
        if (amount <= 0) return true;
        saved = Math.max(0, Math.min(saved, amount));
        if (amount - saved > getRemainingBudget()) return false;
        BlockPos pos = data.getTreasuryPos();
        if (pos == null || !level.isLoaded(pos)) return false;
        Container container = GovernorGarrison.getContainer(level, pos);
        if (container == null || GovernorGarrison.countCurrency(container) < amount) return false;

        int removed = GovernorGarrison.removeCurrency(container, amount);
        if (removed < amount) {
            GovernorGarrison.addCurrency(container, removed);
            data.lastKnownTreasury = GovernorGarrison.countCurrency(container);
            return false;
        }
        data.lastKnownTreasury = GovernorGarrison.countCurrency(container);
        data.addSpent(getDay(), amount - saved);
        return true;
    }

    // ------------------------------------------------------------------ village

    public List<RecruitsVillage> getVillages() {
        return GovernorManager.getVillages(claim);
    }

    public int getInhabitants() {
        return GovernorManager.getInhabitants(level, claim);
    }

    /** Effective reputation of the owner (and his faction) in the village, 0 without the noble system. */
    public int getReputation(RecruitsVillage village) {
        RecruitsVillageManager villageManager = VillageEvents.villageManager;
        UUID owner = getOwner();
        if (villageManager == null || owner == null) return 0;
        return villageManager.getEffectiveReputation(village, owner, RecruitsVillageManager.getFactionOf(level.getServer(), owner));
    }

    // ------------------------------------------------------------------ hiring

    /**
     * Offers of the loaded nobles of the claim's villages for this entity type, with the owner's price.
     * Searches the claim for entities: call it in the daily check, not every interval.
     */
    public List<HireOffer> getNobleOffers(ResourceLocation entityId) {
        List<HireOffer> offers = new ArrayList<>();
        UUID owner = getOwner();
        for (VillagerNobleEntity noble : GovernorHiring.findSources(level, claim).nobles()) {
            RecruitsHireTrade trade = GovernorHiring.findTrade(noble, entityId);
            if (trade == null) continue;
            int price = GovernorHiring.getPrice(level, owner, noble, trade);
            offers.add(new HireOffer(noble.getUUID(), noble.getName().getString(), entityId, price, trade.uses));
        }
        return offers;
    }

    /**
     * Asks the Governor to hire a unit. Only while he is loaded (false otherwise). A request with the same key
     * replaces the old one, so a module can ask again at every daily check without duplicates.
     */
    public boolean requestHire(HireRequest request) {
        if (controller == null) return false;
        controller.queueHire(request, getDay());
        return true;
    }

    public void cancelHire(String key) {
        if (controller != null) controller.cancelHire(key);
    }

    /** The open request with this key, null if there is none (served, expired, or never asked). */
    @Nullable
    public HireRequest getHireRequest(String key) {
        return controller == null ? null : controller.getHireRequest(key);
    }
}
