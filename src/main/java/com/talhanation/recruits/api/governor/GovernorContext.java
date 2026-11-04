package com.talhanation.recruits.api.governor;

import com.talhanation.recruits.VillageEvents;
import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorController;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
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
