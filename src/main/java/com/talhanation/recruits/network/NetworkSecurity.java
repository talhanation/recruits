package com.talhanation.recruits.network;

import com.talhanation.recruits.FactionEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.net.Message;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Team;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Central server-side guards for every packet coming from a client.
 *
 * Rule of thumb for all handlers: the client only says WHAT it wants, never WHO it is.
 * The acting player is always {@code context.getSender()} and every permission is
 * re-checked here on the server.
 */
public final class NetworkSecurity {

    /** Max packets a player can send in one burst. */
    private static final double RATE_BURST = 80.0D;
    /** Packets per second that refill the burst budget. */
    private static final double RATE_PER_SECOND = 25.0D;
    private static final long WARN_INTERVAL_NANOS = 10_000_000_000L;

    /** Only touched on the server thread (inside enqueueWork). */
    private static final Map<UUID, Bucket> BUCKETS = new HashMap<>();

    private NetworkSecurity() {
    }

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    /**
     * Same as corelib's CommonRegistry.registerMessage, but
     * - a server-bound message is only executed when it really arrived on the server,
     * - a client-bound message is only executed when it really arrived on the client,
     * - server-bound messages go through a per-player rate limit.
     *
     * corelib does not check the direction, so a modified client could send any
     * "MessageToClient..." packet to the server (or to a LAN host) and have it executed there.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends Message<?>> void registerMessage(SimpleChannel channel, int id, Class<T> message) {
        channel.registerMessage(id, (Class) message, Message::toBytes, (buf) -> {
            try {
                Message<?> msg = message.getDeclaredConstructor().newInstance();
                return msg.fromBytes(buf);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, (msg, fun) -> {
            NetworkEvent.Context context = fun.get();
            boolean arrivedOnServer = context.getDirection().getReceptionSide().isServer();

            if (msg.getExecutingSide().equals(Dist.CLIENT)) {
                if (arrivedOnServer) {
                    // Client-bound packet sent by a client: never execute it.
                    context.setPacketHandled(true);
                    return;
                }
                context.enqueueWork(() -> msg.executeClientSide(context));
                context.setPacketHandled(true);
            } else if (msg.getExecutingSide().equals(Dist.DEDICATED_SERVER)) {
                if (!arrivedOnServer) {
                    context.setPacketHandled(true);
                    return;
                }
                context.enqueueWork(() -> {
                    ServerPlayer sender = context.getSender();
                    if (sender == null) return;
                    if (!allowPacket(sender)) return;
                    msg.executeServerSide(context);
                });
                context.setPacketHandled(true);
            }
        });
    }

    // ------------------------------------------------------------------
    // Rate limit (token bucket per player, server thread only)
    // ------------------------------------------------------------------

    private static final class Bucket {
        double tokens = RATE_BURST;
        long lastRefill = System.nanoTime();
        long lastWarn;
    }

    static boolean allowPacket(ServerPlayer player) {
        long now = System.nanoTime();
        Bucket bucket = BUCKETS.computeIfAbsent(player.getUUID(), uuid -> new Bucket());

        double seconds = (now - bucket.lastRefill) / 1_000_000_000.0D;
        bucket.tokens = Math.min(RATE_BURST, bucket.tokens + seconds * RATE_PER_SECOND);
        bucket.lastRefill = now;

        if (bucket.tokens < 1.0D) {
            if (now - bucket.lastWarn > WARN_INTERVAL_NANOS) {
                bucket.lastWarn = now;
                Main.LOGGER.warn("[Recruits] Dropping packets from {}: too many Recruits packets per second.", player.getScoreboardName());
            }
            return false;
        }
        bucket.tokens -= 1.0D;

        if (BUCKETS.size() > 512) cleanupBuckets(now);
        return true;
    }

    private static void cleanupBuckets(long now) {
        Iterator<Map.Entry<UUID, Bucket>> it = BUCKETS.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().lastRefill > 60_000_000_000L) it.remove();
        }
    }

    // ------------------------------------------------------------------
    // Permission helpers
    // ------------------------------------------------------------------

    public static boolean isAdmin(ServerPlayer player) {
        return player != null && player.hasPermissions(2);
    }

    public static boolean isAdminCreative(ServerPlayer player) {
        return isAdmin(player) && player.isCreative();
    }

    public static boolean isOwner(ServerPlayer player, @Nullable AbstractRecruitEntity recruit) {
        return player != null
                && recruit != null
                && recruit.isOwned()
                && player.getUUID().equals(recruit.getOwnerUUID());
    }

    /** Owner of the recruit (or an operator) and the recruit is alive. */
    public static boolean canControl(ServerPlayer player, @Nullable AbstractRecruitEntity recruit) {
        if (recruit == null || !recruit.isAlive()) return false;
        return isOwner(player, recruit) || isAdmin(player);
    }

    public static boolean canManageGroup(ServerPlayer player, @Nullable RecruitsGroup group) {
        if (player == null || group == null) return false;
        return player.getUUID().equals(group.getPlayerUUID()) || isAdmin(player);
    }

    @Nullable
    public static RecruitsFaction getFaction(ServerPlayer player) {
        if (player == null || FactionEvents.recruitsFactionManager == null) return null;
        Team team = player.getTeam();
        if (team == null) return null;
        return FactionEvents.recruitsFactionManager.getFactionByStringID(team.getName());
    }

    public static boolean isFactionLeader(ServerPlayer player, @Nullable RecruitsFaction faction) {
        return player != null && faction != null && player.getUUID().equals(faction.getTeamLeaderUUID());
    }

    /** True when the player is the leader of the faction with this string id AND currently in it. */
    public static boolean isLeaderOf(ServerPlayer player, @Nullable String factionId) {
        if (factionId == null) return false;
        RecruitsFaction own = getFaction(player);
        return own != null && factionId.equals(own.getStringID()) && isFactionLeader(player, own);
    }

    /** Same rule as MessengerEntity#mobInteract: only the addressed player, only while the messenger is there. */
    public static boolean canAnswerMessenger(ServerPlayer player, @Nullable MessengerEntity messenger) {
        if (player == null || messenger == null || !messenger.isAlive()) return false;
        RecruitsPlayerInfo target = messenger.getTargetPlayerInfo();
        if (target == null || !player.getUUID().equals(target.getUUID())) return false;
        if (player.getUUID().equals(messenger.getOwnerUUID())) return false;
        MessengerEntity.MessengerState state = messenger.getMessengerState();
        return state == MessengerEntity.MessengerState.ARRIVED
                || state == MessengerEntity.MessengerState.WAITING
                || state == MessengerEntity.MessengerState.MOVING_TO_TARGET_PLAYER;
    }

    public static boolean isNear(ServerPlayer player, @Nullable Entity entity, double maxDistance) {
        return entity != null && player.distanceToSqr(entity) <= maxDistance * maxDistance;
    }

    /** Trims and caps client supplied text. Never returns null. */
    public static String cleanText(@Nullable String text, int maxLength) {
        if (text == null) return "";
        String s = text.strip();
        return s.length() <= maxLength ? s : s.substring(0, maxLength);
    }
}
