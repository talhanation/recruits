package com.talhanation.recruits.governor;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.VillageEvents;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsHireTrade;
import com.talhanation.recruits.world.RecruitsVillage;
import com.talhanation.recruits.world.RecruitsVillageManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Hiring helpers of the Governor, shared by the garrison and by the modules of addons (workers).
 * Everything works with the owner's UUID, so the Governor also hires while his owner is offline.
 */
public final class GovernorHiring {

    private GovernorHiring() {
    }

    /** Where units can be hired in a claim: free (unowned) units standing in it and the nobles of its villages. */
    public record Sources(List<AbstractRecruitEntity> freeUnits, Set<VillagerNobleEntity> nobles) {
    }

    public static Sources findSources(ServerLevel level, RecruitsClaim claim) {
        AABB box = GovernorManager.getClaimBox(level, claim, 0);
        List<AbstractRecruitEntity> free = new ArrayList<>();
        Set<VillagerNobleEntity> nobles = new LinkedHashSet<>();
        for (AbstractRecruitEntity entity : level.getEntitiesOfClass(AbstractRecruitEntity.class, box,
                e -> e.isAlive() && GovernorManager.isInClaim(claim, e.blockPosition()))) {
            if (entity instanceof VillagerNobleEntity noble) nobles.add(noble);
            else if (!entity.isOwned() && entity.canBeHired()) free.add(entity);
        }
        // the nobles of the villages of the claim may stand a little outside of it
        RecruitsVillageManager villageManager = VillageEvents.villageManager;
        if (villageManager != null) {
            for (RecruitsVillage village : GovernorManager.getVillages(claim)) nobles.addAll(villageManager.getLoadedNobles(level, village));
        }
        return new Sources(free, nobles);
    }

    public static ResourceLocation getEntityId(AbstractRecruitEntity entity) {
        return EntityType.getKey(entity.getType());
    }

    /** The noble's trade for this entity type with uses left, null if he does not offer it today. */
    @Nullable
    public static RecruitsHireTrade findTrade(VillagerNobleEntity noble, ResourceLocation entityId) {
        for (RecruitsHireTrade trade : noble.getTrades()) {
            if (trade != null && trade.uses > 0 && entityId.equals(trade.resourceLocation)) return trade;
        }
        return null;
    }

    /** Price for the owner: the noble's reputation price, -1 if the owner may not buy it (embargo, reputation). */
    public static int getPrice(ServerLevel level, @Nullable UUID owner, VillagerNobleEntity noble, RecruitsHireTrade trade) {
        RecruitsVillageManager villageManager = VillageEvents.villageManager;
        if (villageManager == null || owner == null) return isEmbargoed(owner, noble) ? -1 : trade.cost;
        return villageManager.getHirePrice(level.getServer(), owner, noble, trade);
    }

    public static boolean isEmbargoed(@Nullable UUID owner, VillagerNobleEntity noble) {
        String nobleTeam = noble.getEmbargoTeamID();
        if (owner == null || nobleTeam == null || nobleTeam.isEmpty() || FactionEvents.recruitsDiplomacyManager == null) return false;
        return FactionEvents.recruitsDiplomacyManager.hasEmbargo(owner, nobleTeam);
    }

    /** A jobless villager of the noble's village who becomes the new unit, null if there is none. */
    @Nullable
    public static Villager pickVillager(ServerLevel level, VillagerNobleEntity noble) {
        RecruitsVillageManager villageManager = VillageEvents.villageManager;
        if (villageManager == null) return null;
        return villageManager.pickVillagerForHire(level, noble);
    }

    /** True if the config wants a villager for every hire and the noble has none to give. */
    public static boolean lacksVillager(ServerLevel level, VillagerNobleEntity noble) {
        return RecruitsServerConfig.NobleVillagerNeedsVillagers.get() && pickVillager(level, noble) == null;
    }

    /**
     * Creates the unit a noble hands over: from one of his villagers (look, name, items) if the config wants that,
     * otherwise next to the noble. The villager is removed. Not yet added to the level, not yet owned.
     */
    @Nullable
    public static AbstractRecruitEntity createFromNoble(ServerLevel level, VillagerNobleEntity noble, EntityType<?> type) {
        Villager villager = null;
        if (RecruitsServerConfig.NobleVillagerNeedsVillagers.get()) {
            villager = pickVillager(level, noble);
            if (villager == null) return null;
        }

        if (!(type.create(level) instanceof AbstractRecruitEntity recruit)) return null;
        recruit.initSpawn();

        if (villager != null) {
            AbstractRecruitEntity.applyVariantFromVillager(recruit, villager);
            if (villager.getCustomName() != null && !villager.getCustomName().getString().isEmpty()) {
                recruit.setCustomName(villager.getCustomName());
            }
            if (villager.isSleeping()) villager.stopSleeping();
            recruit.copyPosition(villager);
            for (ItemStack stack : villager.getInventory().items) {
                if (!stack.isEmpty()) recruit.getInventory().addItem(stack.copy());
            }
            if (RecruitsServerConfig.RecruitTablesPOIReleasing.get()) villager.releasePoi(MemoryModuleType.JOB_SITE);
            villager.releasePoi(MemoryModuleType.HOME);
            villager.releasePoi(MemoryModuleType.MEETING_POINT);
            villager.discard();
        }
        else {
            recruit.copyPosition(noble);
        }
        return recruit;
    }

    /** True if the owner may get one more unit: player limit (faction/player) and faction NPC limit. */
    public static boolean canOwnerTakeUnit(UUID owner, String factionId) {
        if (!GovernorGarrison.canFactionAddNPC(factionId)) return false;
        return RecruitEvents.recruitsPlayerUnitManager == null || RecruitEvents.recruitsPlayerUnitManager.canPlayerRecruit(factionId, owner);
    }

    /**
     * Makes the unit a normal unit of the owner (not a garrison unit): owner, team, persistence, player count.
     * Used for the units the modules ask for, e.g. workers. Works while the owner is offline.
     */
    public static void hireForOwner(ServerLevel level, AbstractRecruitEntity recruit, UUID owner, String ownerName, String factionId) {
        recruit.setOwnerUUID(Optional.of(owner));
        recruit.setIsOwned(true);
        recruit.despawnTimer = -1;
        recruit.setPersistenceRequired();
        recruit.resetPaymentTimer();
        recruit.setTarget(null);
        recruit.setGroupUUID(null);
        recruit.setFollowState(0);
        recruit.setAggroState(0);

        if (recruit instanceof ICompanion companion) {
            companion.setOwnerName(ownerName);
        }

        PlayerTeam team = factionId == null || factionId.isEmpty() ? null : level.getScoreboard().getPlayerTeam(factionId);
        if (team != null) {
            FactionEvents.addRecruitToTeam(recruit, team, level);
            FactionEvents.addNPCToData(level, factionId, 1);
        }
        recruit.needsTeamUpdate = true;

        if (RecruitEvents.recruitsPlayerUnitManager != null) RecruitEvents.recruitsPlayerUnitManager.addRecruits(owner, 1);
    }
}
