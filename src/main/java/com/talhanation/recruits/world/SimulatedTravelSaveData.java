package com.talhanation.recruits.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Saves the simulated journeys in the overworld. The maps are the live maps of the
 * {@link SimulatedTravelManager}, so a world save always writes the current state.
 */
public class SimulatedTravelSaveData extends SavedData {
    private static final String DATA_NAME = "recruits_simulated_travel";

    /** Groups that are out of the world, by leader. */
    final Map<UUID, SimulatedTravel> travels = new HashMap<>();
    /** Leaders on a journey who are in the world and really walk, by leader. */
    final Map<UUID, Walker> walkers = new HashMap<>();

    /** Where a really walking leader was last seen, to find him again when his chunk was unloaded under him. */
    static class Walker {
        ResourceLocation dimension;
        BlockPos pos;
        /** Game time (overworld) of his last sign of life. Not saved: after a restart everybody is looked for. */
        long lastSeen;
        /** He just came back into the world and is not taken out again before this game time. Not saved. */
        long noPackBefore;
        long nextRecover;
        int attempts;

        Walker(ResourceLocation dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos;
        }
    }

    public static SimulatedTravelSaveData load(CompoundTag nbt) {
        SimulatedTravelSaveData data = new SimulatedTravelSaveData();
        ListTag travels = nbt.getList("travels", Tag.TAG_COMPOUND);
        for (int i = 0; i < travels.size(); i++) {
            SimulatedTravel travel = SimulatedTravel.load(travels.getCompound(i));
            if (travel != null) data.travels.put(travel.leader, travel);
        }
        ListTag walkers = nbt.getList("walkers", Tag.TAG_COMPOUND);
        for (int i = 0; i < walkers.size(); i++) {
            CompoundTag tag = walkers.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(tag.getString("dimension"));
            if (!tag.hasUUID("leader") || dimension == null) continue;
            data.walkers.put(tag.getUUID("leader"), new Walker(dimension, BlockPos.of(tag.getLong("pos"))));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag travelList = new ListTag();
        for (SimulatedTravel travel : travels.values()) travelList.add(travel.save());
        nbt.put("travels", travelList);

        ListTag walkerList = new ListTag();
        for (Map.Entry<UUID, Walker> entry : walkers.entrySet()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("leader", entry.getKey());
            tag.putString("dimension", entry.getValue().dimension.toString());
            tag.putLong("pos", entry.getValue().pos.asLong());
            walkerList.add(tag);
        }
        nbt.put("walkers", walkerList);
        return nbt;
    }

    public static SimulatedTravelSaveData get(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(SimulatedTravelSaveData::load, SimulatedTravelSaveData::new, DATA_NAME);
    }
}
