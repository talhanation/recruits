package com.talhanation.recruits.world;

import com.talhanation.recruits.client.gui.worldmap.storage.WorldMapStorageId;
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
    public static final int MAX_WAYPOINTS = 512;

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
        this.waypoints = new ArrayList<>();
        if (waypoints != null) {
            for (Waypoint waypoint : waypoints) {
                addWaypoint(waypoint);
            }
        }
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
        if (waypoint == null || waypoints.size() >= MAX_WAYPOINTS) return;
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

        UUID id = nbt.hasUUID("ID") ? nbt.getUUID("ID") : UUID.randomUUID();
        String name = nbt.getString("Name");

        List<Waypoint> waypoints = new ArrayList<>();
        ListTag waypointList = nbt.getList("Waypoints", 10);
        int waypointCount = Math.min(waypointList.size(), MAX_WAYPOINTS);
        for (int i = 0; i < waypointCount; i++) {
            Waypoint waypoint = Waypoint.fromNBT(waypointList.getCompound(i));
            if (waypoint != null) waypoints.add(waypoint);
        }
        return new RecruitsRoute(id, name, waypoints);
    }

    public void saveToFile(File directory) throws IOException {
        if (!directory.exists()) directory.mkdirs();
        File routeFile = routeFile(directory);
        NbtIo.write(this.toNBT(), routeFile);

        File legacyFile = legacyRouteFile(directory);
        if (!legacyFile.equals(routeFile) && legacyFile.exists()) legacyFile.delete();
    }

    public void deleteFile(File directory) {
        File routeFile = routeFile(directory);
        if (routeFile.exists()) routeFile.delete();

        File legacyFile = legacyRouteFile(directory);
        if (!legacyFile.equals(routeFile) && legacyFile.exists()) legacyFile.delete();
    }

    @Nullable
    public static RecruitsRoute loadFromFile(File file) throws IOException {
        if (!file.exists()) return null;
        CompoundTag nbt = NbtIo.read(file);
        if (nbt == null) return null;
        return fromNBT(nbt);
    }

    public static List<RecruitsRoute> loadAllRoutes(File directory) throws IOException {
        List<RecruitsRoute> routes = new ArrayList<>();
        if (!directory.exists()) return routes;
        File[] files = directory.listFiles((dir, n) -> n.endsWith(".nbt"));
        if (files == null) return routes;
        for (File file : files) {
            RecruitsRoute route = loadFromFile(file);
            if (route != null) {
                routes.add(route);
                if (!isCurrentRouteFile(file, route)) {
                    route.saveToFile(directory);
                    file.delete();
                }
            }
        }
        return routes;
    }

    public static File getRoutesDirectory() {
        return new File(Minecraft.getInstance().gameDirectory, "recruits/routes/" + WorldMapStorageId.detectCurrent());
    }

    public static String sanitiseName(String name) {
        if (name == null || name.isEmpty()) return "route";
        return name.replaceAll("[^a-zA-Z0-9_\\-\\.]", "_");
    }

    private File routeFile(File directory) {
        return new File(directory, id + ".nbt");
    }

    private File legacyRouteFile(File directory) {
        return new File(directory, sanitiseName(name) + ".nbt");
    }

    private static boolean isCurrentRouteFile(File file, RecruitsRoute route) {
        return file.getName().equals(route.getId() + ".nbt");
    }

    // -------------------------------------------------------------------------

    public static class WaypointAction {

        public enum Type {
            WAIT;

            public static Type fromString(String s) {
                try {
                    return valueOf(s);
                } catch (Exception e) {
                    return WAIT;
                }
            }
        }

        private final Type type;
        private int waitSeconds;

        public WaypointAction(Type type, int waitSeconds) {
            this.type = type;
            this.waitSeconds = waitSeconds;
        }

        public Type getType() {
            return type;
        }

        public int getWaitSeconds() {
            return waitSeconds;
        }

        public void setWaitSeconds(int seconds) {
            this.waitSeconds = seconds;
        }

        public CompoundTag toNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("ActionType", type.name());
            nbt.putInt("WaitSeconds", waitSeconds);
            return nbt;
        }

        @Nullable
        public static WaypointAction fromNBT(CompoundTag nbt) {
            if (nbt == null || nbt.isEmpty()) return null;
            return new WaypointAction(Type.fromString(nbt.getString("ActionType")), nbt.getInt("WaitSeconds"));
        }

        @Override
        public String toString() {
            return "Wait " + waitSeconds + "s";
        }
    }

    // -------------------------------------------------------------------------

    public static class Waypoint {
        private final String name;
        private BlockPos position;
        @Nullable
        private WaypointAction action;

        public Waypoint(String name, BlockPos position, @Nullable WaypointAction action) {
            this.name = name;
            this.position = position;
            this.action = action;
        }

        public String getName() {
            return name;
        }

        public BlockPos getPosition() {
            return position;
        }

        public void setPosition(BlockPos position) {
            this.position = position;
        }

        @Nullable
        public WaypointAction getAction() {
            return action;
        }

        public void setAction(@Nullable WaypointAction action) {
            this.action = action;
        }

        public CompoundTag toNBT() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("Name", name);
            nbt.putInt("X", position.getX());
            nbt.putInt("Y", position.getY());
            nbt.putInt("Z", position.getZ());
            if (action != null) nbt.put("Action", action.toNBT());
            return nbt;
        }

        @Nullable
        public static Waypoint fromNBT(CompoundTag nbt) {
            if (nbt == null || nbt.isEmpty()) return null;

            WaypointAction action = null;
            if (nbt.contains("Action")) {
                action = WaypointAction.fromNBT(nbt.getCompound("Action"));
            }

            return new Waypoint(
                    nbt.getString("Name"),
                    new BlockPos(nbt.getInt("X"), nbt.getInt("Y"), nbt.getInt("Z")),
                    action
            );
        }
    }
}
