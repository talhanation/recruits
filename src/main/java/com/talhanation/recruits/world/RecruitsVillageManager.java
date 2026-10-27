package com.talhanation.recruits.world;

import com.mojang.authlib.GameProfile;
import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.network.MessageToClientNobleInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Dörfer, Noble-Wahl, Ruf und Tagesaufträge (siehe claude/noble-villager-konzept.md).
 */
public class RecruitsVillageManager {

    public static final long DAY_TICKS = 24000L;
    /** Mitte der Vanilla-Versammlung (MEET 9000–11000). Fenster bis zur Dämmerung. */
    public static final long MEETING_TIME = 10000L;
    public static final long MEETING_END = 12000L;
    public static final int TICK_INTERVAL = 100;
    public static final int VILLAGER_CHECK_INTERVAL = 200;
    public static final long NOBLE_DEATH_COOLDOWN_DAYS = 2;
    public static final long DISSOLVE_EMPTY_TICKS = 5 * DAY_TICKS;

    public static final int REP_HIT = -3;
    public static final int REP_KILL_VILLAGER = -15;
    public static final int REP_KILL_NOBLE = -40;
    /** Hiring from a noble needs more than 2 jobless villagers of the village, so the village never runs empty. */
    public static final int MIN_JOBLESS_FOR_HIRE = 3;
    private static final String HIT_THROTTLE_KEY = "RecruitsNobleRepHit";
    private static final int HIT_THROTTLE_TICKS = 20;

    private final Map<UUID, RecruitsVillage> villages = new LinkedHashMap<>();
    private final Map<UUID, NobleQuestProgress> activeQuests = new HashMap<>();
    private final RandomSource random = RandomSource.create();

    /* ===================== Laden / Speichern ===================== */

    public void load(ServerLevel overworld) {
        RecruitsVillageSaveData data = RecruitsVillageSaveData.get(overworld);
        villages.clear();
        activeQuests.clear();
        for (RecruitsVillage village : data.getVillages()) {
            villages.put(village.getId(), village);
        }
        activeQuests.putAll(data.getActiveQuests());
    }

    public void save(ServerLevel overworld) {
        RecruitsVillageSaveData data = RecruitsVillageSaveData.get(overworld);
        data.setVillages(new ArrayList<>(villages.values()));
        data.setActiveQuests(new HashMap<>(activeQuests));
        data.setDirty();
    }

    /* ===================== Config ===================== */

    public static boolean isReputationEnabled() {
        return RecruitsServerConfig.NobleReputationEnabled.get();
    }

    public static boolean isQuestsEnabled() {
        return RecruitsServerConfig.NobleQuestsEnabled.get();
    }

    public static long getDay(Level level) {
        return level.getDayTime() / DAY_TICKS;
    }

    /* ===================== Suche ===================== */

    public Collection<RecruitsVillage> getVillages() {
        return Collections.unmodifiableCollection(villages.values());
    }

    @Nullable
    public RecruitsVillage getVillage(@Nullable UUID id) {
        return id == null ? null : villages.get(id);
    }

    /** Das nächste Dorf, dessen Mitte höchstens 48 Blöcke von {@code pos} entfernt ist. */
    @Nullable
    public RecruitsVillage findVillage(ResourceKey<Level> dimension, BlockPos pos) {
        RecruitsVillage best = null;
        double bestDist = Double.MAX_VALUE;
        for (RecruitsVillage village : villages.values()) {
            if (!village.isInRange(dimension, pos)) continue;
            double dist = village.distanceSqr(pos);
            if (dist < bestDist) {
                bestDist = dist;
                best = village;
            }
        }
        return best;
    }

    /** Ein Villager gehört zu dem Dorf, in dessen Radius sein Bett steht. */
    @Nullable
    public RecruitsVillage getVillageOfVillager(Villager villager) {
        Optional<GlobalPos> home = villager.getBrain().getMemory(MemoryModuleType.HOME);
        if (home.isEmpty()) return null;
        GlobalPos pos = home.get();
        if (!pos.dimension().equals(villager.level().dimension())) return null;
        return findVillage(pos.dimension(), pos.pos());
    }

    @Nullable
    public RecruitsVillage getVillageOfNoble(VillagerNobleEntity noble) {
        return getVillage(noble.getVillageId());
    }

    /* ===================== Claims (Governor) ===================== */

    /** Villages whose centre (the bell) lies in the claim. Claims only exist in the overworld. */
    public List<RecruitsVillage> getVillagesInClaim(@Nullable RecruitsClaim claim) {
        List<RecruitsVillage> list = new ArrayList<>();
        if (claim == null) return list;
        for (RecruitsVillage village : villages.values()) {
            if (!village.getDimension().equals(Level.OVERWORLD)) continue;
            if (claim.containsChunk(new ChunkPos(village.getCenter()))) list.add(village);
        }
        return list;
    }

    /** Inhabitants of all villages in the claim, counted exactly like the nobles count them. */
    public int getInhabitants(@Nullable RecruitsClaim claim, long day) {
        int count = 0;
        for (RecruitsVillage village : getVillagesInClaim(claim)) count += village.getInhabitants(day);
        return count;
    }

    /** Loaded nobles of the village. */
    public List<VillagerNobleEntity> getLoadedNobles(ServerLevel level, RecruitsVillage village) {
        List<VillagerNobleEntity> list = new ArrayList<>();
        if (!village.getDimension().equals(level.dimension())) return list;
        for (UUID uuid : village.nobles.keySet()) {
            if (level.getEntity(uuid) instanceof VillagerNobleEntity noble && noble.isAlive()) list.add(noble);
        }
        return list;
    }

    /**
     * Jobless adult villagers that belong to the village (their bed is in it) and are loaded right now.
     * These are the villagers a noble can turn into recruits.
     */
    public List<Villager> findJoblessResidents(ServerLevel level, RecruitsVillage village) {
        if (!village.getDimension().equals(level.dimension())) return new ArrayList<>();
        return level.getEntitiesOfClass(Villager.class,
                new AABB(village.getCenter()).inflate(RecruitsVillage.RADIUS + 16),
                villager -> villager.isAlive() && !villager.isBaby() && !villager.isPassenger()
                        && villager.getVillagerData().getProfession().equals(VillagerProfession.NONE)
                        && getVillageOfVillager(villager) == village);
    }

    /** Jobless villagers a noble can hire from: the residents of his village, without village the ones around him. */
    public List<Villager> findHireableVillagers(ServerLevel level, VillagerNobleEntity noble) {
        RecruitsVillage village = getVillageOfNoble(noble);
        if (village != null) return findJoblessResidents(level, village);
        return level.getEntitiesOfClass(Villager.class, noble.getBoundingBox().inflate(32D),
                villager -> villager.isAlive() && !villager.isBaby() && !villager.isPassenger()
                        && villager.getVillagerData().getProfession().equals(VillagerProfession.NONE));
    }

    /** The villager that becomes the recruit: the nearest hireable one, if the village has enough of them. */
    @Nullable
    public Villager pickVillagerForHire(ServerLevel level, VillagerNobleEntity noble) {
        List<Villager> list = findHireableVillagers(level, noble);
        if (list.size() < MIN_JOBLESS_FOR_HIRE) return null;
        list.sort(Comparator.comparingDouble(villager -> villager.distanceToSqr(noble)));
        return list.get(0);
    }

    /* ===================== Einwohner ===================== */

    /** Alle 200 Ticks pro Villager (Konzept 1, „Erfassen“). */
    public void updateVillager(ServerLevel level, Villager villager) {
        Optional<GlobalPos> home = villager.getBrain().getMemory(MemoryModuleType.HOME);
        if (home.isEmpty()) return; // 1. kein Bett → kein Dorf

        GlobalPos bed = home.get();
        if (!bed.dimension().equals(level.dimension())) return;

        long day = getDay(level);
        RecruitsVillage village = findVillage(level.dimension(), bed.pos());

        if (village == null) {
            // 3. Glocke, die zu keinem Dorf gehört → neues Dorf mit dieser Glocke als Mitte
            Optional<GlobalPos> meeting = villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT);
            if (meeting.isEmpty() || !meeting.get().dimension().equals(level.dimension())) return;

            BlockPos bell = meeting.get().pos();
            if (findVillage(level.dimension(), bell) != null) return;

            village = new RecruitsVillage(UUID.randomUUID(), level.dimension(), bell);
            village.lastResidentSeenTime = level.getGameTime();
            villages.put(village.getId(), village);

            if (!village.isInRange(level.dimension(), bed.pos())) return;
        }

        // 2. Bett im Radius → Einwohner
        village.residents.put(villager.getUUID(), day);
        village.lastResidentSeenTime = level.getGameTime();
    }

    /* ===================== Nobles ===================== */

    /**
     * Stellt sicher, dass der Noble in seinem Dorf eingetragen ist. Bestehende Nobles ohne Dorf
     * tragen sich in das Dorf ein, in dessen Radius sie stehen.
     */
    @Nullable
    public RecruitsVillage ensureNoble(ServerLevel level, VillagerNobleEntity noble) {
        RecruitsVillage village = getVillage(noble.getVillageId());

        if (village != null && !village.nobles.containsKey(noble.getUUID())) {
            int slot = noble.getVillageSlot() >= 0 && isSlotFree(village, noble.getVillageSlot())
                    ? noble.getVillageSlot() : village.getNextFreeSlot();
            village.addNoble(noble.getUUID(), slot, noble.getTraderType());
            noble.setVillage(village.getId(), slot);
        }

        if (village == null) {
            village = findVillage(level.dimension(), noble.blockPosition());
            if (village == null) {
                if (noble.getVillageId() != null) noble.setVillage(null, -1);
                return null;
            }
            RecruitsVillage.NobleEntry existing = village.nobles.get(noble.getUUID());
            int slot = existing != null ? existing.slot : village.getNextFreeSlot();
            village.addNoble(noble.getUUID(), slot, noble.getTraderType());
            noble.setVillage(village.getId(), slot);
        }

        RecruitsVillage.NobleEntry entry = village.nobles.get(noble.getUUID());
        if (entry != null) entry.traderType = noble.getTraderType();
        village.lastResidentSeenTime = level.getGameTime();
        return village;
    }

    private boolean isSlotFree(RecruitsVillage village, int slot) {
        for (RecruitsVillage.NobleEntry entry : village.nobles.values()) {
            if (entry.slot == slot) return false;
        }
        return true;
    }

    /** Noble endgültig entfernt (getötet oder discard): austragen, Bett freigeben, ggf. Cooldown. */
    public void onNobleRemoved(ServerLevel level, VillagerNobleEntity noble, boolean killed) {
        RecruitsVillage village = getVillage(noble.getVillageId());
        if (village != null) {
            village.nobles.remove(noble.getUUID());
            if (killed) village.lastNobleDeathDay = getDay(level);
        }
        releaseBed(level, noble.getHomeBed());
    }

    public static void releaseBed(ServerLevel level, @Nullable BlockPos bed) {
        if (bed == null) return;
        PoiManager poiManager = level.getPoiManager();
        if (!poiManager.existsAtPosition(PoiTypes.HOME, bed)) return;
        try {
            poiManager.release(bed);
        } catch (IllegalStateException e) {
            Main.LOGGER.debug("Could not release noble bed at {}", bed);
        }
    }

    /* ===================== Tick / Versammlung ===================== */

    /** Alle 100 Ticks pro Dimension. */
    public void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        long day = getDay(level);
        long time = level.getDayTime() % DAY_TICKS;

        Iterator<RecruitsVillage> iterator = villages.values().iterator();
        while (iterator.hasNext()) {
            RecruitsVillage village = iterator.next();
            if (!village.getDimension().equals(level.dimension())) continue;
            if (!level.isPositionEntityTicking(village.getCenter())) continue;

            // Auflösen: 5 Tage (geladene Zeit) kein Einwohner gesehen
            if (gameTime - village.lastResidentSeenTime > VILLAGER_CHECK_INTERVAL * 2L) {
                village.emptyLoadedTicks += TICK_INTERVAL;
                if (village.emptyLoadedTicks >= DISSOLVE_EMPTY_TICKS) {
                    iterator.remove();
                    continue;
                }
            } else {
                village.emptyLoadedTicks = 0;
            }

             if (time >= MEETING_TIME && time < MEETING_END && village.lastElectionDay != day) {
                village.lastElectionDay = day;
                holdMeeting(level, village, day);
            }
        }

        if (level.dimension().equals(Level.OVERWORLD)) {
            expireQuests(level.getServer(), gameTime);
        }
    }

    /** Einmal pro Tag: Einwohner aufräumen, Aufträge beschließen, ggf. Noble wählen. */
    private void holdMeeting(ServerLevel level, RecruitsVillage village, long day) {
        village.removeStaleResidents(day);

        if (isQuestsEnabled()) {
            village.offers.clear();
            village.offers.addAll(NobleQuestRegistry.drawOffers(random, getBountyTargets(level.getServer(), village)));
            village.offersDay = day;
        }

        if (!RecruitsServerConfig.NobleVillagerSpawn.get()) return;

        int inhabitants = village.getInhabitants(day);
        int desired = RecruitsVillage.getDesiredNobles(inhabitants,
                RecruitsServerConfig.NobleMinInhabitants.get(),
                RecruitsServerConfig.NobleInhabitantsPerAdditional.get(),
                RecruitsServerConfig.NobleMaxPerVillage.get());

        if (village.nobles.size() >= desired) return;
        if (day - village.lastNobleDeathDay < NOBLE_DEATH_COOLDOWN_DAYS) return;

        List<Villager> candidates = level.getEntitiesOfClass(Villager.class,
                new AABB(village.getCenter()).inflate(RecruitsVillage.RADIUS),
                villager -> isCandidate(village, villager));
        if (candidates.isEmpty()) return;

        Villager chosen = candidates.get(random.nextInt(candidates.size()));
        convertToNoble(level, village, chosen);
    }

    /** Einwohner, erwachsen, Beruf NONE, wach. */
    private boolean isCandidate(RecruitsVillage village, Villager villager) {
        if (!villager.isAlive() || villager.isBaby() || villager.isSleeping()) return false;
        if (villager.isPassenger() || villager.isLeashed()) return false;
        if (!villager.getVillagerData().getProfession().equals(VillagerProfession.NONE)) return false;
        if (!village.residents.containsKey(villager.getUUID())) return false;
        RecruitsVillage bedVillage = getVillageOfVillager(villager);
        return bedVillage == village;
    }

    @Nullable
    public VillagerNobleEntity convertToNoble(ServerLevel level, RecruitsVillage village, Villager villager) {
        VillagerNobleEntity noble = ModEntityTypes.VILLAGER_NOBLE.get().create(level);
        if (noble == null) return null;

        if (villager.isSleeping()) villager.stopSleeping();

        noble.copyPosition(villager);
        noble.initSpawn();
        AbstractRecruitEntity.applyVariantFromVillager(noble, villager);
        noble.setTraderType(pickTraderType(village));

        for (ItemStack stack : villager.getInventory().items) {
            if (!stack.isEmpty()) noble.getInventory().addItem(stack.copy());
        }

        if (villager.hasCustomName()) noble.setCustomName(villager.getCustomName());

        // Bett NICHT freigeben – es gehört jetzt dem Noble.
        villager.getBrain().getMemory(MemoryModuleType.HOME)
                .filter(home -> home.dimension().equals(level.dimension()))
                .ifPresent(home -> noble.setHomeBed(home.pos()));

        int slot = village.getNextFreeSlot();
        noble.setVillage(village.getId(), slot);
        noble.goToAnchor(village.getAnchor(slot));

        Team team = villager.getTeam();
        if (team != null) {
            PlayerTeam playerTeam = level.getScoreboard().getPlayerTeam(team.getName());
            if (playerTeam != null) level.getScoreboard().addPlayerToTeam(noble.getStringUUID(), playerTeam);
        }

        ForgeEventFactory.onLivingConvert(villager, noble);
        level.addFreshEntity(noble);

        if (RecruitsServerConfig.RecruitTablesPOIReleasing.get()) villager.releasePoi(MemoryModuleType.JOB_SITE);
        villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
        villager.releasePoi(MemoryModuleType.MEETING_POINT);
        village.residents.remove(villager.getUUID());
        villager.discard();

        village.addNoble(noble.getUUID(), slot, noble.getTraderType());
        village.lastResidentSeenTime = level.getGameTime();

        playElectionCeremony(level, village, noble);
        return noble;
    }

    private String pickTraderType(RecruitsVillage village) {
        List<String> all = RecruitsHireTradesRegistry.getAllTraderTypes();
        if (all.isEmpty()) return "";
        Set<String> used = village.getUsedTraderTypes();
        List<String> unused = new ArrayList<>(all);
        unused.removeAll(used);
        List<String> pool = unused.isEmpty() ? all : unused;
        return pool.get(random.nextInt(pool.size()));
    }

    private void playElectionCeremony(ServerLevel level, RecruitsVillage village, VillagerNobleEntity noble) {
        BlockPos center = village.getCenter();
        level.playSound(null, center, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, noble.getX(), noble.getY() + 1.0D, noble.getZ(), 20, 0.5D, 1.0D, 0.5D, 0.1D);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, noble.getX(), noble.getY() + 1.0D, noble.getZ(), 30, 0.5D, 1.0D, 0.5D, 0.2D);

        Component message = Component.translatable("chat.recruits.noble.elected");
        for (ServerPlayer player : level.players()) {
            if (village.isInRange(level.dimension(), player.blockPosition())) {
                player.sendSystemMessage(message);
            }
        }
    }

    /* ===================== Ruf ===================== */

    public record Actor(UUID player, @Nullable String faction) {
    }

    /** Spieler direkt, oder der Owner eines Recruits. */
    @Nullable
    public Actor resolveActor(@Nullable Entity entity) {
        if (entity instanceof ServerPlayer player) {
            return new Actor(player.getUUID(), getFactionId(player));
        }
        if (entity instanceof AbstractRecruitEntity recruit && recruit.isOwned() && recruit.getOwnerUUID() != null) {
            Player owner = recruit.getOwner();
            String faction = owner instanceof ServerPlayer serverOwner ? getFactionId(serverOwner) : getFactionId(recruit.getTeam());
            return new Actor(recruit.getOwnerUUID(), faction);
        }
        return null;
    }

    @Nullable
    public static String getFactionId(@Nullable Player player) {
        return player == null ? null : getFactionId(player.getTeam());
    }

    @Nullable
    public static String getFactionId(@Nullable Team team) {
        if (team == null || FactionEvents.recruitsFactionManager == null) return null;
        return FactionEvents.recruitsFactionManager.getFactionByStringID(team.getName()) != null ? team.getName() : null;
    }

    /** Fraktion eines (evtl. offline) Spielers über die Mitgliederlisten. */
    @Nullable
    public static String getFactionOf(MinecraftServer server, UUID player) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) return getFactionId(online);
        if (FactionEvents.recruitsFactionManager == null) return null;
        for (RecruitsFaction faction : FactionEvents.recruitsFactionManager.getFactions()) {
            if (player.equals(faction.getTeamLeaderUUID())) return faction.getStringID();
            for (RecruitsPlayerInfo member : faction.members) {
                if (member != null && player.equals(member.getUUID())) return faction.getStringID();
            }
        }
        return null;
    }

    public static boolean isLeader(UUID player, @Nullable String factionId) {
        if (factionId == null || FactionEvents.recruitsFactionManager == null) return false;
        RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(factionId);
        return faction != null && player.equals(faction.getTeamLeaderUUID());
    }

    /** Spieler-Ruf um Δ, Fraktions-Ruf um Δ / 2. */
    public void addReputation(RecruitsVillage village, Actor actor, int delta) {
        if (!isReputationEnabled() || village == null || actor == null) return;
        village.addPlayerReputation(actor.player(), delta);
        village.addFactionReputation(actor.faction(), NobleReputation.getFactionDelta(delta));
    }

    public int getEffectiveReputation(RecruitsVillage village, UUID player, @Nullable String faction) {
        int playerRep = village.getPlayerReputation(player);
        if (faction == null) return NobleReputation.getEffective(playerRep, 0, false, false);
        return NobleReputation.getEffective(playerRep, village.getFactionReputation(faction), true, isLeader(player, faction));
    }

    public int getEffectiveReputation(RecruitsVillage village, ServerPlayer player) {
        return getEffectiveReputation(village, player.getUUID(), getFactionId(player));
    }

    /** Ruf ist für diesen Noble aktiv (Config an und Noble hat ein Dorf). */
    @Nullable
    private RecruitsVillage getReputationVillage(VillagerNobleEntity noble) {
        if (!isReputationEnabled()) return null;
        return getVillageOfNoble(noble);
    }

    /** Embargo der Fraktion des Nobles gegen den Spieler (wie beim Rechtsklick in VillagerEvents). */
    public static boolean hasEmbargo(ServerPlayer player, VillagerNobleEntity noble) {
        String teamId = noble.getEmbargoTeamID();
        if (teamId == null || teamId.isEmpty() || FactionEvents.recruitsDiplomacyManager == null) return false;
        return FactionEvents.recruitsDiplomacyManager.hasEmbargo(player.getUUID(), teamId);
    }

    public boolean isHostile(ServerPlayer player, VillagerNobleEntity noble) {
        RecruitsVillage village = getReputationVillage(noble);
        return village != null && NobleReputation.getTier(getEffectiveReputation(village, player)) == NobleReputation.Tier.HOSTILE;
    }

    /**
     * Price for a (maybe offline) player, e.g. when his Governor hires. −1 if he may not buy this trade.
     * Same rules as {@link #getHirePrice(ServerPlayer, VillagerNobleEntity, RecruitsHireTrade)}.
     */
    public int getHirePrice(MinecraftServer server, UUID player, VillagerNobleEntity noble, RecruitsHireTrade trade) {
        String teamId = noble.getEmbargoTeamID();
        if (teamId != null && !teamId.isEmpty() && FactionEvents.recruitsDiplomacyManager != null
                && FactionEvents.recruitsDiplomacyManager.hasEmbargo(player, teamId)) return -1;
        RecruitsVillage village = getReputationVillage(noble);
        if (village == null) return trade.cost;

        int reputation = getEffectiveReputation(village, player, getFactionOf(server, player));
        if (NobleReputation.getTier(reputation) == NobleReputation.Tier.HOSTILE) return -1;

        int tradeLevel = RecruitsHireTradesRegistry.getTradeLevel(noble.getTraderType(), trade.resourceLocation);
        if (!NobleReputation.isTradeLevelUnlocked(reputation, tradeLevel)) return -1;

        return NobleReputation.getPrice(trade.cost, reputation);
    }

    /** Preis auf dem Server. −1, wenn der Spieler diesen Trade nicht kaufen darf. */
    public int getHirePrice(ServerPlayer player, VillagerNobleEntity noble, RecruitsHireTrade trade) {
        if (hasEmbargo(player, noble)) return -1;
        RecruitsVillage village = getReputationVillage(noble);
        if (village == null) return trade.cost;

        int reputation = getEffectiveReputation(village, player);
        if (NobleReputation.getTier(reputation) == NobleReputation.Tier.HOSTILE) return -1;

        int tradeLevel = RecruitsHireTradesRegistry.getTradeLevel(noble.getTraderType(), trade.resourceLocation);
        if (!NobleReputation.isTradeLevelUnlocked(reputation, tradeLevel)) return -1;

        return NobleReputation.getPrice(trade.cost, reputation);
    }

    /* ===================== Ereignisse ===================== */

    @Nullable
    private RecruitsVillage getVillageOfVictim(LivingEntity victim) {
        if (victim instanceof VillagerNobleEntity noble) return getVillageOfNoble(noble);
        if (victim instanceof Villager villager) return getVillageOfVillager(villager);
        return null;
    }

    /** Dorfbewohner oder Noble geschlagen: −3, höchstens einmal pro Sekunde je Opfer. */
    public void onVictimHurt(LivingEntity victim, DamageSource source) {
        if (!isReputationEnabled()) return;
        RecruitsVillage village = getVillageOfVictim(victim);
        if (village == null) return;

        Actor actor = resolveActor(source.getEntity());
        if (actor == null) return;

        long now = victim.level().getGameTime();
        CompoundTag data = victim.getPersistentData();
        if (data.contains(HIT_THROTTLE_KEY) && now - data.getLong(HIT_THROTTLE_KEY) < HIT_THROTTLE_TICKS) return;
        data.putLong(HIT_THROTTLE_KEY, now);

        addReputation(village, actor, REP_HIT);
    }

    /**
     * Who gets the credit for a kill: the attacker, the owner of a projectile, the recruit that operates a
     * siege weapon or cannon, or - for deaths by fire, falling and the like - whoever hurt the victim last.
     */
    @Nullable
    public Entity resolveKiller(LivingEntity victim, DamageSource source) {
        Entity entity = source.getEntity();
        if (entity == null && source.getDirectEntity() instanceof Projectile projectile) entity = projectile.getOwner();
        if (entity != null && !(entity instanceof Player) && !(entity instanceof AbstractRecruitEntity)) {
            Entity operator = entity.getControllingPassenger();
            if (!(operator instanceof AbstractRecruitEntity)) {
                operator = null;
                for (Entity passenger : entity.getPassengers()) {
                    if (passenger instanceof AbstractRecruitEntity) {
                        operator = passenger;
                        break;
                    }
                }
            }
            if (operator != null) entity = operator;
        }
        if (resolveActor(entity) == null) {
            LivingEntity credit = victim.getKillCredit();
            if (credit != null && credit != victim) entity = credit;
        }
        return entity;
    }

    public void onLivingDeath(ServerLevel level, LivingEntity victim, DamageSource source) {
        Entity killer = resolveKiller(victim, source);
        Actor actor = resolveActor(killer);

        // Ruf: Dorfbewohner −15, Noble −40
        if (actor != null && isReputationEnabled()) {
            RecruitsVillage village = getVillageOfVictim(victim);
            if (village != null) {
                addReputation(village, actor, victim instanceof VillagerNobleEntity ? REP_KILL_NOBLE : REP_KILL_VILLAGER);
            }
        }

        if (actor == null || activeQuests.isEmpty()) return;

        // Auftragsfortschritt
        NobleQuestProgress progress = activeQuests.get(actor.player());
        if (progress == null || progress.completed || progress.isExpired(level.getGameTime())) return;

        NobleQuest quest = progress.quest;
        ServerPlayer holder = level.getServer().getPlayerList().getPlayer(actor.player());

        if (quest.type == NobleQuest.Type.MONSTER_HUNT) {
            if (!NobleQuestRegistry.matchesMonster(quest.target, victim.getType())) return;
            RecruitsVillage village = getVillage(progress.villageId);
            if (village == null) return;
            // the victim or the one who killed it (archers shoot from the edge of the village) has to be in the village
            boolean inVillage = village.isInRange(level.dimension(), victim.blockPosition())
                    || (killer != null && village.isInRange(level.dimension(), killer.blockPosition()));
            if (!inVillage) return;

            progress.addProgress(1);
            if (holder != null) {
                holder.sendSystemMessage(progress.completed
                        ? Component.translatable("chat.recruits.noble.quest.completed")
                        : Component.translatable("chat.recruits.noble.quest.progress", progress.progress, quest.count));
            }
        }
        else if (quest.type == NobleQuest.Type.BOUNTY && victim instanceof ServerPlayer target) {
            if (!target.getUUID().equals(quest.getTargetUUID())) return;

            progress.addProgress(1);

            // Schuld beglichen: Ziel bekommt +15
            RecruitsVillage village = getVillage(progress.villageId);
            if (village != null && isReputationEnabled()) {
                if (quest.isFactionBounty()) village.addFactionReputation(quest.targetFaction, NobleQuestRegistry.BOUNTY_ATONEMENT);
                else village.addPlayerReputation(target.getUUID(), NobleQuestRegistry.BOUNTY_ATONEMENT);
            }
            if (holder != null) holder.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.completed"));
        }
    }

    /* ===================== Aufträge ===================== */

    private List<NobleQuestRegistry.BountyTarget> getBountyTargets(MinecraftServer server, RecruitsVillage village) {
        List<NobleQuestRegistry.BountyTarget> targets = new ArrayList<>();
        if (!isReputationEnabled()) return targets;

        village.playerReputation.forEach((uuid, value) -> {
            if (value > NobleQuestRegistry.BOUNTY_THRESHOLD) return;
            String name = getPlayerName(server, uuid);
            if (name != null) targets.add(new NobleQuestRegistry.BountyTarget(uuid, name, null));
        });

        if (FactionEvents.recruitsFactionManager != null) {
            village.factionReputation.forEach((factionId, value) -> {
                if (value > NobleQuestRegistry.BOUNTY_THRESHOLD) return;
                RecruitsFaction faction = FactionEvents.recruitsFactionManager.getFactionByStringID(factionId);
                if (faction == null || !isRealPlayer(faction.getTeamLeaderUUID())) return;
                targets.add(new NobleQuestRegistry.BountyTarget(faction.getTeamLeaderUUID(), faction.getTeamLeaderName(), factionId));
            });
        }
        return targets;
    }

    /** Admin-/NPC-Fraktionen haben als Leader den Platzhalter UUID(0,0) „none“ – kein Kopfgeld-Ziel. */
    private static boolean isRealPlayer(@Nullable UUID uuid) {
        return uuid != null && !(uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L);
    }

    @Nullable
    private static String getPlayerName(MinecraftServer server, UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) return online.getScoreboardName();
        GameProfileCache cache = server.getProfileCache();
        if (cache == null) return null;
        return cache.get(uuid).map(GameProfile::getName).orElse(null);
    }

    @Nullable
    public NobleQuestProgress getActiveQuest(UUID player) {
        return activeQuests.get(player);
    }

    private void expireQuests(MinecraftServer server, long gameTime) {
        Iterator<Map.Entry<UUID, NobleQuestProgress>> iterator = activeQuests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, NobleQuestProgress> entry = iterator.next();
            if (!entry.getValue().isExpired(gameTime)) continue;
            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) player.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.expired"));
        }
    }

    public void acceptQuest(ServerPlayer player, VillagerNobleEntity noble, UUID offerId) {
        if (!isQuestsEnabled()) return;
        RecruitsVillage village = getVillageOfNoble(noble);
        if (village == null || hasEmbargo(player, noble)) return;

        if (isHostile(player, noble)) {
            player.sendSystemMessage(Component.translatable("chat.recruits.noble.hostile"));
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        NobleQuestProgress current = activeQuests.get(player.getUUID());
        if (current != null && !current.isExpired(gameTime)) {
            player.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.already_active"));
            return;
        }

        NobleQuest offer = village.getOffer(offerId);
        if (offer == null || offer.takenBy.contains(player.getUUID())) return;

        if (offer.type == NobleQuest.Type.BOUNTY) {
            UUID target = offer.getTargetUUID();
            if (target == null || target.equals(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.bounty_not_allowed"));
                return;
            }
            String takerFaction = getFactionId(player);
            String targetFaction = offer.isFactionBounty() ? offer.targetFaction : getFactionOf(player.getServer(), target);
            if (takerFaction != null && takerFaction.equals(targetFaction)) {
                player.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.bounty_not_allowed"));
                return;
            }
        }

        offer.takenBy.add(player.getUUID());
        activeQuests.put(player.getUUID(), new NobleQuestProgress(offer.copy(), village.getId(), gameTime + NobleQuestProgress.DURATION_TICKS));
        player.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.accepted", offer.getDescription()));

        if (offer.type == NobleQuest.Type.BOUNTY) {
            ServerPlayer target = player.getServer().getPlayerList().getPlayer(offer.getTargetUUID());
            if (target != null) target.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.bounty_on_you"));
        }
    }

    public void turnInQuest(ServerPlayer player, VillagerNobleEntity noble) {
        NobleQuestProgress progress = activeQuests.get(player.getUUID());
        if (progress == null) return;

        if (hasEmbargo(player, noble)) return;

        RecruitsVillage village = getVillageOfNoble(noble);
        if (village == null || !village.getId().equals(progress.villageId)) {
            player.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.wrong_village"));
            return;
        }
        if (isHostile(player, noble)) {
            player.sendSystemMessage(Component.translatable("chat.recruits.noble.hostile"));
            return;
        }

        NobleQuest quest = progress.quest;
        if (quest.type == NobleQuest.Type.DELIVERY && !progress.completed) {
            if (quest.countDeliveryItems(player) < quest.count) {
                player.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.missing_items", quest.count, quest.getTargetDisplayName()));
                return;
            }
            quest.removeDeliveryItems(player);
            progress.completed = true;
            progress.progress = quest.count;
        }

        if (!progress.completed) {
            player.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.not_done"));
            return;
        }

        activeQuests.remove(player.getUUID());
        addReputation(village, new Actor(player.getUUID(), getFactionId(player)), quest.rewardReputation);
        giveCurrency(player, NobleQuestRegistry.REWARD_CURRENCY);
        player.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.rewarded", NobleReputation.format(quest.rewardReputation)));
    }

    public void abandonQuest(ServerPlayer player) {
        if (activeQuests.remove(player.getUUID()) != null) {
            player.sendSystemMessage(Component.translatable("chat.recruits.noble.quest.abandoned"));
        }
    }

    private static void giveCurrency(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        ResourceLocation id = ResourceLocation.tryParse(RecruitsServerConfig.RecruitCurrency.get());
        Item item = id != null ? ForgeRegistries.ITEMS.getValue(id) : null;
        if (item == null || item == Items.AIR) item = Items.EMERALD;

        ItemStack stack = new ItemStack(item, amount);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    /* ===================== Aufträge durch den Governor ===================== */

    /**
     * The Governor takes an offer of the village for his (maybe offline) owner. Same rules as for the player:
     * one active quest, not when hostile, every offer once per player. Bounties are left to the player.
     */
    @Nullable
    public NobleQuestProgress acceptQuestFor(ServerLevel level, UUID owner, @Nullable String faction, RecruitsVillage village, UUID offerId) {
        if (!isQuestsEnabled()) return null;
        if (isReputationEnabled() && NobleReputation.getTier(getEffectiveReputation(village, owner, faction)) == NobleReputation.Tier.HOSTILE) return null;

        long gameTime = level.getGameTime();
        NobleQuestProgress current = activeQuests.get(owner);
        if (current != null && !current.isExpired(gameTime)) return null;

        NobleQuest offer = village.getOffer(offerId);
        if (offer == null || offer.type == NobleQuest.Type.BOUNTY || offer.takenBy.contains(owner)) return null;

        offer.takenBy.add(owner);
        NobleQuestProgress progress = new NobleQuestProgress(offer.copy(), village.getId(), gameTime + NobleQuestProgress.DURATION_TICKS);
        activeQuests.put(owner, progress);
        return progress;
    }

    /**
     * The Governor turns in the owner's quest of this village. A delivery counts as done when he brought the items.
     * Gives the reputation to the owner and returns the currency reward (goes to the treasury), −1 if nothing was turned in.
     */
    public int turnInQuestFor(ServerLevel level, UUID owner, @Nullable String faction, RecruitsVillage village, boolean deliveredItems) {
        NobleQuestProgress progress = activeQuests.get(owner);
        if (progress == null || !village.getId().equals(progress.villageId) || progress.isExpired(level.getGameTime())) return -1;
        if (progress.quest.type == NobleQuest.Type.DELIVERY && !progress.completed && deliveredItems) {
            progress.completed = true;
            progress.progress = progress.quest.count;
        }
        if (!progress.completed) return -1;

        activeQuests.remove(owner);
        addReputation(village, new Actor(owner, faction), progress.quest.rewardReputation);
        return NobleQuestRegistry.REWARD_CURRENCY;
    }

    /* ===================== Client-Info ===================== */

    public NobleVillageInfo buildInfo(ServerPlayer player, VillagerNobleEntity noble) {
        NobleVillageInfo info = new NobleVillageInfo();
        info.nobleId = noble.getUUID();

        RecruitsVillage village = getVillageOfNoble(noble);
        info.hasVillage = village != null;
        if (village == null) {
            info.joblessVillagers = findHireableVillagers(player.serverLevel(), noble).size();
            return info;
        }

        long day = getDay(player.level());
        info.inhabitants = village.getInhabitants(day);
        info.joblessVillagers = findJoblessResidents(player.serverLevel(), village).size();

        info.reputationActive = isReputationEnabled();
        if (info.reputationActive) {
            String faction = getFactionId(player);
            info.playerReputation = village.getPlayerReputation(player.getUUID());
            info.hasFaction = faction != null;
            info.factionReputation = village.getFactionReputation(faction);
            info.isLeader = isLeader(player.getUUID(), faction);
            info.effectiveReputation = getEffectiveReputation(village, player.getUUID(), faction);
        }

        info.questsActive = isQuestsEnabled();
        if (info.questsActive) {
            for (NobleQuest offer : village.offers) {
                info.offers.add(offer);
                info.offerTaken.add(offer.takenBy.contains(player.getUUID()));
            }
        }

        NobleQuestProgress progress = activeQuests.get(player.getUUID());
        long gameTime = player.serverLevel().getGameTime();
        if (progress != null && !progress.isExpired(gameTime)) {
            info.activeQuest = progress.quest;
            info.activeProgress = progress.progress;
            info.activeCompleted = progress.completed;
            info.activeQuestHere = progress.villageId.equals(village.getId());
            info.activeTicksLeft = progress.expiresAt - gameTime;
        }
        return info;
    }

    public void sendInfo(ServerPlayer player, VillagerNobleEntity noble, boolean openScreen) {
        Main.SIMPLE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new MessageToClientNobleInfo(buildInfo(player, noble), openScreen));
    }

    @Nullable
    public static VillagerNobleEntity findNoble(ServerPlayer player, UUID nobleId, double range) {
        return player.serverLevel().getEntitiesOfClass(VillagerNobleEntity.class,
                        player.getBoundingBox().inflate(range),
                        noble -> noble.getUUID().equals(nobleId) && noble.isAlive())
                .stream().findAny().orElse(null);
    }

    /** Für Debug/Admin: Anker eines Nobles. */
    @Nullable
    public Vec3 getAnchor(VillagerNobleEntity noble) {
        RecruitsVillage village = getVillageOfNoble(noble);
        if (village == null) return null;
        RecruitsVillage.NobleEntry entry = village.nobles.get(noble.getUUID());
        return village.getAnchor(entry != null ? entry.slot : Math.max(0, noble.getVillageSlot()));
    }
}
