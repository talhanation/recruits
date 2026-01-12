package com.talhanation.recruits.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecruitsRoute {
    private UUID id;
    private String name;
    private List<Waypoint> waypoints;

    public RecruitsRoute(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.waypoints = new ArrayList<>();
    }

    public RecruitsRoute(UUID id, String name, List<Waypoint> waypoints) {
        this.id = id;
        this.name = name;
        this.waypoints = waypoints;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void addWaypoint(Waypoint waypoint) {
        waypoints.add(waypoint);
    }

    public void removeWaypoint(Waypoint waypoint) {
        waypoints.remove(waypoint);
    }

    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putUUID("ID", id);
        nbt.putString("Name", name);

        ListTag waypointList = new ListTag();
        for (Waypoint waypoint : waypoints) {
            waypointList.add(waypoint.toNBT());
        }
        nbt.put("Waypoints", waypointList);

        return nbt;
    }

    public static RecruitsRoute fromNBT(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) return null;

        UUID id = nbt.getUUID("ID");
        String name = nbt.getString("Name");

        List<Waypoint> waypoints = new ArrayList<>();
        ListTag waypointList = nbt.getList("Waypoints", 10); // 10 = CompoundTag type

        for (int i = 0; i < waypointList.size(); i++) {
            CompoundTag waypointTag = waypointList.getCompound(i);
            Waypoint waypoint = Waypoint.fromNBT(waypointTag);
            if (waypoint != null) {
                waypoints.add(waypoint);
            }
        }

        return new RecruitsRoute(id, name, waypoints);
    }

    public void saveToFile(File directory) throws IOException {
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File routeFile = new File(directory, name + ".nbt");
        CompoundTag nbt = this.toNBT();
        NbtIo.write(nbt, routeFile);
    }

    @Nullable
    public static RecruitsRoute loadFromFile(File file) throws IOException {
        if (!file.exists()) return null;

        CompoundTag nbt = NbtIo.read(file);
        if (nbt == null) return null;

        return fromNBT(nbt);
    }

    // Lädt alle Routen aus einem Verzeichnis
    public static List<RecruitsRoute> loadAllRoutes(File directory) throws IOException {
        List<RecruitsRoute> routes = new ArrayList<>();

        if (!directory.exists()) {
            return routes;
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".nbt"));
        if (files == null) return routes;

        for (File file : files) {
            RecruitsRoute route = loadFromFile(file);
            if (route != null) {
                routes.add(route);
            }
        }

        return routes;
    }

    public static File getRoutesDirectory() {
        return new File(Minecraft.getInstance().gameDirectory, "recruits/routes/");
    }
    public static class Waypoint {
        private String name;
        private BlockPos position;

        public Waypoint(String name, BlockPos position) {
            this.name = name;
            this.position = position;
        }

        public String getName() {
            return name;
        }

        public BlockPos getPosition() {
            return position;
        }
        public CompoundTag toNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("Name", name);
            nbt.putInt("X", position.getX());
            nbt.putInt("Y", position.getY());
            nbt.putInt("Z", position.getZ());
            return nbt;
        }

        @Nullable
        public static Waypoint fromNBT(CompoundTag nbt) {
            if (nbt == null || nbt.isEmpty()) return null;

            String name = nbt.getString("Name");
            int x = nbt.getInt("X");
            int y = nbt.getInt("Y");
            int z = nbt.getInt("Z");

            return new Waypoint(name, new BlockPos(x, y, z));
        }
    }
}
