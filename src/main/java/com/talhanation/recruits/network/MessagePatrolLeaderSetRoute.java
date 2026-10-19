package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractLeaderEntity;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.Objects;
import java.util.UUID;

/**
 * Client → Server: assigns a route to the leader AND delivers the full waypoint
 * data (positions + per-waypoint wait seconds). Routes are stored client-side,
 * so the positions must be included in the packet — the server cannot read the
 * client's filesystem.
 */
public class MessagePatrolLeaderSetRoute implements Message<MessagePatrolLeaderSetRoute> {

    private UUID recruit;
    @Nullable private UUID routeId;       // null = clear route
    private List<BlockPos> waypoints;     // ordered waypoint positions
    private List<Integer>  waitSeconds;   // parallel: wait time per waypoint (0 = none)

    public MessagePatrolLeaderSetRoute() {}

    /** Assign a route with its waypoint data. */
    public MessagePatrolLeaderSetRoute(UUID recruit,
                                       @Nullable UUID routeId,
                                       List<BlockPos> waypoints,
                                       List<Integer> waitSeconds) {
        this.recruit     = recruit;
        this.routeId     = routeId;
        this.waypoints   = waypoints;
        this.waitSeconds = waitSeconds;
    }

    /** Clear the route. */
    public MessagePatrolLeaderSetRoute(UUID recruit) {
        this(recruit, null, List.of(), List.of());
    }

    @Override
    public Dist getExecutingSide() {
        return Dist.DEDICATED_SERVER;
    }

    @Override
    public void executeServerSide(NetworkEvent.Context context) {
        ServerPlayer player = Objects.requireNonNull(context.getSender());
        player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractLeaderEntity.class,
                player.getBoundingBox().inflate(64.0D),
                leader -> leader.getUUID().equals(this.recruit) && NetworkSecurity.canControl(player, leader)
        ).forEach(leader -> {
            if (routeId != null) {
                leader.setRouteID(routeId);
            } else {
                leader.clearRouteID();
            }
            leader.loadRouteWaypointsFromData(waypoints, waitSeconds);
        });
    }

    private static final int MAX_ROUTE_WAYPOINTS = 1024;

    private static <T> List<T> readBounded(FriendlyByteBuf buf, Function<FriendlyByteBuf, T> reader) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ROUTE_WAYPOINTS) throw new IllegalArgumentException("Too many route waypoints: " + size);
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(reader.apply(buf));
        return list;
    }

    @Override
    public MessagePatrolLeaderSetRoute fromBytes(FriendlyByteBuf buf) {
        this.recruit     = buf.readUUID();
        boolean hasRoute = buf.readBoolean();
        this.routeId     = hasRoute ? buf.readUUID() : null;
        // readList() pre-allocates the size the client claims -> bound it first.
        this.waypoints   = readBounded(buf, FriendlyByteBuf::readBlockPos);
        this.waitSeconds = readBounded(buf, FriendlyByteBuf::readVarInt);
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.recruit);
        buf.writeBoolean(this.routeId != null);
        if (this.routeId != null) buf.writeUUID(this.routeId);
        buf.writeCollection(this.waypoints, FriendlyByteBuf::writeBlockPos);
        buf.writeCollection(this.waitSeconds, FriendlyByteBuf::writeVarInt);
    }
}
