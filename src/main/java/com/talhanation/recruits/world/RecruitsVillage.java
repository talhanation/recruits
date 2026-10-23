package com.talhanation.recruits.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Ein Dorf = eine Mitte (die erste gefundene Glocke) und ein Radius von 48 Blöcken.
 */
public class RecruitsVillage {

    /** Reichweite, in der Vanilla-Villager ihre Glocke suchen (AcquirePoi). */
    public static final int RADIUS = 48;
    public static final long RADIUS_SQR = (long) RADIUS * RADIUS;
    /** Abstand der Nobles von der Mitte. */
    public static final double ANCHOR_RADIUS = 3D;

    public static class NobleEntry {
        public final int slot;
        public String traderType;

        public NobleEntry(int slot, String traderType) {
            this.slot = slot;
            this.traderType = traderType == null ? "" : traderType;
        }
    }

    private final UUID id;
    private final ResourceKey<Level> dimension;
    private final BlockPos center;

    /** Villager-UUID → letzter Tag, an dem er mit Bett im Dorf gesehen wurde. */
    public final Map<UUID, Long> residents = new HashMap<>();
    public final Map<UUID, NobleEntry> nobles = new LinkedHashMap<>();

    public long lastElectionDay = -1;
    public long lastNobleDeathDay = Long.MIN_VALUE / 2;

    /** Spielzeit, zu der zuletzt ein Einwohner oder Noble gesehen wurde. */
    public long lastResidentSeenTime;
    /** Ticks, die die Mitte geladen war, ohne dass ein Einwohner gesehen wurde. */
    public long emptyLoadedTicks;

    public final Map<UUID, Integer> playerReputation = new HashMap<>();
    public final Map<String, Integer> factionReputation = new HashMap<>();

    public final List<NobleQuest> offers = new ArrayList<>();
    public long offersDay = -1;

    public RecruitsVillage(UUID id, ResourceKey<Level> dimension, BlockPos center) {
        this.id = id;
        this.dimension = dimension;
        this.center = center.immutable();
    }

    public UUID getId() {
        return id;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getCenter() {
        return center;
    }

    public boolean isInRange(ResourceKey<Level> dim, BlockPos pos) {
        return dimension.equals(dim) && center.distSqr(pos) <= RADIUS_SQR;
    }

    public double distanceSqr(BlockPos pos) {
        return center.distSqr(pos);
    }

    /* ===================== Einwohner und Nobles ===================== */

    /** Entfernt Einwohner, die seit mehr als 2 Tagen nicht gesehen wurden. */
    public void removeStaleResidents(long day) {
        residents.values().removeIf(lastSeen -> day - lastSeen > 2);
    }

    /** Einwohner = Villager mit Bett im Dorf (letzte 2 Tage) + lebende Nobles. */
    public int getInhabitants(long day) {
        int count = 0;
        for (long lastSeen : residents.values()) {
            if (day - lastSeen <= 2) count++;
        }
        return count + nobles.size();
    }

    /** 7–16 Einwohner: 1 · 17–26: 2 · ab 27: 3 (mit Defaults). */
    public static int getDesiredNobles(int inhabitants, int minInhabitants, int perAdditional, int max) {
        if (inhabitants < minInhabitants) return 0;
        int desired = 1 + (inhabitants - minInhabitants) / Math.max(1, perAdditional);
        return Math.min(desired, max);
    }

    public int getNextFreeSlot() {
        Set<Integer> used = new HashSet<>();
        for (NobleEntry entry : nobles.values()) used.add(entry.slot);
        int slot = 0;
        while (used.contains(slot)) slot++;
        return slot;
    }

    public void addNoble(UUID uuid, int slot, String traderType) {
        nobles.put(uuid, new NobleEntry(slot, traderType));
    }

    public Set<String> getUsedTraderTypes() {
        Set<String> types = new HashSet<>();
        for (NobleEntry entry : nobles.values()) {
            if (!entry.traderType.isEmpty()) types.add(entry.traderType);
        }
        return types;
    }

    /** Halteposition eines Nobles: Mitte + Versatz auf 120°-Teilung, Radius 3. */
    public Vec3 getAnchor(int slot) {
        int ring = Math.max(0, slot) / 3;
        double angle = Math.toRadians((slot % 3) * 120D + ring * 60D);
        double radius = ANCHOR_RADIUS + ring;
        return new Vec3(
                center.getX() + 0.5D + Math.cos(angle) * radius,
                center.getY(),
                center.getZ() + 0.5D + Math.sin(angle) * radius);
    }

    /* ===================== Ruf ===================== */

    public int getPlayerReputation(UUID player) {
        return playerReputation.getOrDefault(player, 0);
    }

    public int getFactionReputation(String faction) {
        if (faction == null || faction.isEmpty()) return 0;
        return factionReputation.getOrDefault(faction, 0);
    }

    public void addPlayerReputation(UUID player, int delta) {
        if (player == null || delta == 0) return;
        int value = NobleReputation.clamp(getPlayerReputation(player) + delta);
        if (value == 0) playerReputation.remove(player);
        else playerReputation.put(player, value);
    }

    public void addFactionReputation(String faction, int delta) {
        if (faction == null || faction.isEmpty() || delta == 0) return;
        int value = NobleReputation.clamp(getFactionReputation(faction) + delta);
        if (value == 0) factionReputation.remove(faction);
        else factionReputation.put(faction, value);
    }

    /* ===================== Aufträge ===================== */

    public NobleQuest getOffer(UUID offerId) {
        for (NobleQuest quest : offers) {
            if (quest.id.equals(offerId)) return quest;
        }
        return null;
    }

    /* ===================== NBT ===================== */

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("dimension", dimension.location().toString());
        tag.put("center", NbtUtils.writeBlockPos(center));
        tag.putLong("lastElectionDay", lastElectionDay);
        tag.putLong("lastNobleDeathDay", lastNobleDeathDay);
        tag.putLong("lastResidentSeenTime", lastResidentSeenTime);
        tag.putLong("emptyLoadedTicks", emptyLoadedTicks);
        tag.putLong("offersDay", offersDay);

        ListTag residentList = new ListTag();
        residents.forEach((uuid, day) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", uuid);
            entry.putLong("day", day);
            residentList.add(entry);
        });
        tag.put("residents", residentList);

        ListTag nobleList = new ListTag();
        nobles.forEach((uuid, noble) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", uuid);
            entry.putInt("slot", noble.slot);
            entry.putString("type", noble.traderType);
            nobleList.add(entry);
        });
        tag.put("nobles", nobleList);

        ListTag playerRepList = new ListTag();
        playerReputation.forEach((uuid, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", uuid);
            entry.putInt("value", value);
            playerRepList.add(entry);
        });
        tag.put("playerReputation", playerRepList);

        ListTag factionRepList = new ListTag();
        factionReputation.forEach((faction, value) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("faction", faction);
            entry.putInt("value", value);
            factionRepList.add(entry);
        });
        tag.put("factionReputation", factionRepList);

        ListTag offerList = new ListTag();
        for (NobleQuest quest : offers) offerList.add(quest.toNBT());
        tag.put("offers", offerList);

        return tag;
    }

    public static RecruitsVillage fromNBT(CompoundTag tag) {
        if (!tag.hasUUID("id")) return null;
        ResourceLocation dimId = ResourceLocation.tryParse(tag.getString("dimension"));
        if (dimId == null) return null;

        RecruitsVillage village = new RecruitsVillage(
                tag.getUUID("id"),
                ResourceKey.create(Registries.DIMENSION, dimId),
                NbtUtils.readBlockPos(tag.getCompound("center")));

        village.lastElectionDay = tag.getLong("lastElectionDay");
        village.lastNobleDeathDay = tag.contains("lastNobleDeathDay") ? tag.getLong("lastNobleDeathDay") : Long.MIN_VALUE / 2;
        village.lastResidentSeenTime = tag.getLong("lastResidentSeenTime");
        village.emptyLoadedTicks = tag.getLong("emptyLoadedTicks");
        village.offersDay = tag.contains("offersDay") ? tag.getLong("offersDay") : -1;

        ListTag residentList = tag.getList("residents", Tag.TAG_COMPOUND);
        for (int i = 0; i < residentList.size(); i++) {
            CompoundTag entry = residentList.getCompound(i);
            if (entry.hasUUID("uuid")) village.residents.put(entry.getUUID("uuid"), entry.getLong("day"));
        }

        ListTag nobleList = tag.getList("nobles", Tag.TAG_COMPOUND);
        for (int i = 0; i < nobleList.size(); i++) {
            CompoundTag entry = nobleList.getCompound(i);
            if (entry.hasUUID("uuid")) village.addNoble(entry.getUUID("uuid"), entry.getInt("slot"), entry.getString("type"));
        }

        ListTag playerRepList = tag.getList("playerReputation", Tag.TAG_COMPOUND);
        for (int i = 0; i < playerRepList.size(); i++) {
            CompoundTag entry = playerRepList.getCompound(i);
            if (entry.hasUUID("uuid")) village.playerReputation.put(entry.getUUID("uuid"), NobleReputation.clamp(entry.getInt("value")));
        }

        ListTag factionRepList = tag.getList("factionReputation", Tag.TAG_COMPOUND);
        for (int i = 0; i < factionRepList.size(); i++) {
            CompoundTag entry = factionRepList.getCompound(i);
            village.factionReputation.put(entry.getString("faction"), NobleReputation.clamp(entry.getInt("value")));
        }

        ListTag offerList = tag.getList("offers", Tag.TAG_COMPOUND);
        for (int i = 0; i < offerList.size(); i++) {
            village.offers.add(NobleQuest.fromNBT(offerList.getCompound(i)));
        }

        return village;
    }
}
