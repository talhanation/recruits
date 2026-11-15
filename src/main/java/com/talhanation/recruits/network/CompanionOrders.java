package com.talhanation.recruits.network;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.MessengerEntity;
import com.talhanation.recruits.governor.GovernorRemoteOrders;
import com.talhanation.recruits.world.CompanionRecord;
import com.talhanation.recruits.world.RecruitsCompanionManager;
import com.talhanation.recruits.world.RecruitsGroup;
import com.talhanation.recruits.world.SimulatedTravelManager;
import de.maxhenkel.corelib.net.Message;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Orders to companions, given next to them or carried by a Messenger.
 * <p>
 * Next to the companion an order takes effect at once. From the companion list of a Messenger the orders are
 * collected in a draft first; when the player sends the Messenger off, he takes them along
 * ({@link MessengerEntity#startOrders}) and hands them over when he arrives ({@link #deliver}).
 * <p>
 * A Governor is the exception: what he manages is data of his claim, not of his entity. His orders are collected
 * as changes to a copy of that data, see {@link GovernorRemoteOrders}.
 * <p>
 * Everything here runs on the server thread.
 */
public final class CompanionOrders {
    /** Up to here a companion is given his orders directly. One value for all orders of a screen. */
    public static final double NEAR_RADIUS = 100.0D;
    /** The Messenger has to stand this close to the player to be given orders for somebody else. */
    public static final double MESSENGER_RANGE = 16.0D;
    private static final int MAX_ORDERS = 32;
    /** A draft nobody touched for this long is forgotten (real time, ms). */
    private static final long DRAFT_MILLIS = 10L * 60L * 1000L;
    /** Type of the saved orders for a Governor; all others are named after their packet. */
    private static final String GOVERNOR = "Governor";

    private static final Map<String, Supplier<CompanionOrder<?>>> TYPES = new HashMap<>();
    private static final Map<UUID, Draft> DRAFTS = new HashMap<>();

    static {
        register(MessagePatrolLeaderSetRoute::new);
        register(MessagePatrolLeaderSetPatrolState::new);
        register(MessagePatrolLeaderSetInfoMode::new);
        register(MessagePatrolLeaderSetEnemyAction::new);
        register(MessageSetTargetPrio::new);
        register(MessageScoutTask::new);
        register(MessageCaptainHarbour::new);
        register(MessageSetLeaderGroup::new);
        register(MessageAssignGroupToCompanion::new);
        register(MessageRemoveAssignedGroupFromCompanion::new);
    }

    private CompanionOrders() {
    }

    private static void register(Supplier<CompanionOrder<?>> factory) {
        TYPES.put(factory.get().getClass().getSimpleName(), factory);
    }

    private static final class Draft {
        final UUID messenger;
        final UUID companion;
        final String companionName;
        final List<CompanionOrder<?>> orders = new ArrayList<>();
        /** Set if the companion is a Governor: the copy of his claim's data the player is changing. */
        @Nullable
        GovernorRemoteOrders.Draft governor;
        long touched = System.currentTimeMillis();

        Draft(UUID messenger, UUID companion, String companionName) {
            this.messenger = messenger;
            this.companion = companion;
            this.companionName = companionName;
        }
    }

    /** A new world: nothing of the last one counts any more. */
    public static void clearDrafts() {
        DRAFTS.clear();
    }

    // ------------------------------------------------------------------
    // From the screens
    // ------------------------------------------------------------------

    /**
     * An order packet arrived. The companion is near: it is carried out. He is not, but the player is writing
     * orders for him at a Messenger: it is written down. Otherwise nothing happens, as before.
     */
    public static <T extends AbstractRecruitEntity> void handle(@Nullable ServerPlayer player, CompanionOrder<T> order) {
        if (player == null || order.getCompanion() == null) return;

        T companion = findNear(player, order.getCompanion(), order.getCompanionType(), NEAR_RADIUS);
        if (companion != null) {
            order.applyTo(companion, player);
            return;
        }

        Draft draft = DRAFTS.get(player.getUUID());
        if (draft == null || !draft.companion.equals(order.getCompanion())) return;
        if (System.currentTimeMillis() - draft.touched > DRAFT_MILLIS) {
            DRAFTS.remove(player.getUUID());
            return;
        }
        // the last word counts: pressing a button five times is one order
        draft.orders.removeIf(written -> written.getClass() == order.getClass());
        if (draft.orders.size() < MAX_ORDERS) draft.orders.add(order);
        draft.touched = System.currentTimeMillis();
    }

    /** The player's own living companion of this type, if he is within the radius. */
    @Nullable
    public static <T extends AbstractRecruitEntity> T findNear(ServerPlayer player, UUID uuid, Class<T> type, double radius) {
        Entity entity = player.serverLevel().getEntity(uuid);
        if (!type.isInstance(entity) || entity.distanceToSqr(player) > radius * radius) return null;
        T companion = type.cast(entity);
        return NetworkSecurity.canControl(player, companion) ? companion : null;
    }

    /** The player who hears about an order: the one who gave it, or the owner if he is online. */
    @Nullable
    public static ServerPlayer playerFor(AbstractRecruitEntity companion, @Nullable ServerPlayer sender) {
        if (sender != null) return sender;
        UUID owner = companion.getOwnerUUID();
        MinecraftServer server = companion.getServer();
        return owner == null || server == null ? null : server.getPlayerList().getPlayer(owner);
    }

    /** Delivered by a Messenger there is no sender: then the group has to belong to the companion's owner. */
    public static boolean canManageGroup(AbstractRecruitEntity companion, @Nullable ServerPlayer sender, @Nullable RecruitsGroup group) {
        if (group == null) return false;
        if (sender != null) return NetworkSecurity.canManageGroup(sender, group);
        return group.getPlayerUUID() != null && group.getPlayerUUID().equals(companion.getOwnerUUID());
    }

    // ------------------------------------------------------------------
    // Drafts at a Messenger
    // ------------------------------------------------------------------

    /** The player's own Messenger who stands next to him with nothing to do, or null (the player is told why). */
    @Nullable
    public static MessengerEntity idleMessenger(ServerPlayer player, UUID messengerId) {
        MessengerEntity messenger = findNear(player, messengerId, MessengerEntity.class, MESSENGER_RANGE);
        if (messenger != null && messenger.getMessengerState() == MessengerEntity.MessengerState.IDLE) return messenger;
        player.sendSystemMessage(Component.translatable("chat.recruits.text.messenger_not_available"));
        return null;
    }

    /**
     * The player starts (or goes on) writing orders for one of his companions at this Messenger.
     * @return the companion's record, null if he may not (the player is told why)
     */
    @Nullable
    public static CompanionRecord beginDraft(ServerPlayer player, UUID messengerId, UUID companionId) {
        RecruitsCompanionManager companions = RecruitEvents.recruitsCompanionManager;
        if (companions == null || messengerId.equals(companionId)) return null;
        if (idleMessenger(player, messengerId) == null) return null;

        CompanionRecord record = companions.get(companionId);
        if (record == null || !player.getUUID().equals(record.owner)) return null;
        if (!record.dimension.equals(player.level().dimension().location())) {
            player.sendSystemMessage(Component.translatable("chat.recruits.text.messenger_orders_other_dimension", record.name));
            return null;
        }

        Draft draft = DRAFTS.get(player.getUUID());
        if (draft == null || !draft.messenger.equals(messengerId) || !draft.companion.equals(companionId)) {
            draft = new Draft(messengerId, companionId, record.name);
            DRAFTS.put(player.getUUID(), draft);
        }
        draft.touched = System.currentTimeMillis();
        return record;
    }

    /**
     * The companion of the player's draft is a Governor: opens (or goes on with) the copy of his claim's data.
     * Call after {@link #beginDraft}.
     * @return null if the Governor has no office, or the claim is not the player's
     */
    @Nullable
    public static GovernorRemoteOrders.Draft beginGovernorDraft(ServerPlayer player, UUID governorId) {
        Draft draft = DRAFTS.get(player.getUUID());
        if (draft == null || !draft.companion.equals(governorId)) return null;
        if (draft.governor == null) draft.governor = GovernorRemoteOrders.begin(player, governorId);
        return draft.governor;
    }

    /** The copy of this claim's Governor data the player is changing at his Messenger right now, or null. */
    @Nullable
    public static GovernorRemoteOrders.Draft governorDraft(ServerPlayer player, @Nullable UUID claimId) {
        Draft draft = DRAFTS.get(player.getUUID());
        if (draft == null || draft.governor == null || !draft.governor.claim.equals(claimId)) return null;
        if (System.currentTimeMillis() - draft.touched > DRAFT_MILLIS) {
            DRAFTS.remove(player.getUUID());
            return null;
        }
        draft.touched = System.currentTimeMillis();
        return draft.governor;
    }

    /**
     * The player is done with his orders.
     * @param send true: the Messenger sets off with them. false: they are thrown away.
     */
    public static void endDraft(ServerPlayer player, UUID messengerId, boolean send) {
        Draft draft = DRAFTS.remove(player.getUUID());
        if (!send || draft == null || !draft.messenger.equals(messengerId)) return;

        MessengerEntity messenger = idleMessenger(player, messengerId);
        if (messenger == null) return;
        ListTag orders = new ListTag();
        for (CompanionOrder<?> order : draft.orders) orders.add(save(order));
        CompoundTag governorOrders = draft.governor == null ? null : GovernorRemoteOrders.collect(draft.governor);
        if (governorOrders != null) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Type", GOVERNOR);
            tag.put("Orders", governorOrders);
            orders.add(tag);
        }
        if (orders.isEmpty()) {
            player.sendSystemMessage(Component.translatable("chat.recruits.text.messenger_no_orders", messenger.getName().getString()));
            return;
        }
        messenger.startOrders(draft.companion, draft.companionName, orders);
    }

    // ------------------------------------------------------------------
    // Delivery
    // ------------------------------------------------------------------

    /**
     * A Messenger arrived where the companion is.
     * <ul>
     * <li>The companion is in the world: the orders are carried out.</li>
     * <li>He is on a simulated journey: they are carried out on him out of the world, so a patrol changes its
     *     route right away. Orders that need the world around him wait until he is back.</li>
     * <li>His chunk is not loaded: all of them wait until it is. No chunk is loaded for this.</li>
     * <li>He is a Governor: his orders change the data of his claim, wherever he is.</li>
     * </ul>
     * @return false if there is no such companion of this owner (any more)
     */
    public static boolean deliver(ServerLevel level, UUID owner, UUID companionId, ListTag allOrders) {
        ListTag orders = new ListTag();
        boolean governorOrders = false;
        boolean governorFound = false;
        for (int i = 0; i < allOrders.size(); i++) {
            CompoundTag tag = allOrders.getCompound(i);
            if (GOVERNOR.equals(tag.getString("Type"))) {
                governorOrders = true;
                governorFound |= GovernorRemoteOrders.apply(owner, companionId, tag.getCompound("Orders"));
            } else {
                orders.add(tag);
            }
        }
        if (governorOrders && orders.isEmpty()) return governorFound;

        if (level.getEntity(companionId) instanceof AbstractRecruitEntity companion && companion.isAlive()) {
            if (!owner.equals(companion.getOwnerUUID())) return false;
            applyAll(companion, orders, false);
            return true;
        }

        RecruitsCompanionManager companions = RecruitEvents.recruitsCompanionManager;
        CompanionRecord record = companions == null ? null : companions.get(companionId);
        if (record == null || !owner.equals(record.owner)) return false;

        ListTag waiting = orders;
        SimulatedTravelManager travels = SimulatedTravelManager.get();
        if (travels != null && travels.isOutOfWorld(companionId)) {
            ListTag rest = new ListTag();
            boolean reached = travels.orderOutOfWorld(level, companionId, leader -> {
                if (!owner.equals(leader.getOwnerUUID())) return;
                rest.addAll(applyAll(leader, orders, true));
                companions.snapshot(leader);
            });
            if (reached) waiting = rest;
        }
        if (!waiting.isEmpty()) companions.addPending(companionId, waiting);
        return true;
    }

    /**
     * Carries the saved orders out on the companion.
     * @param outOfWorld the companion is a copy outside of any world: orders that need the world are left out
     * @return the orders that were left out
     */
    public static ListTag applyAll(AbstractRecruitEntity companion, ListTag orders, boolean outOfWorld) {
        ListTag left = new ListTag();
        for (int i = 0; i < orders.size(); i++) {
            CompoundTag tag = orders.getCompound(i);
            CompanionOrder<?> order = load(tag);
            if (order == null) continue;
            if (outOfWorld && order.needsWorld()) {
                left.add(tag);
                continue;
            }
            apply(order, companion);
        }
        return left;
    }

    private static <T extends AbstractRecruitEntity> void apply(CompanionOrder<T> order, AbstractRecruitEntity companion) {
        if (!companion.getUUID().equals(order.getCompanion()) || !order.getCompanionType().isInstance(companion)) return;
        try {
            order.applyTo(order.getCompanionType().cast(companion), null);
        } catch (RuntimeException e) {
            // one bad order must not cost the others, nor the Messenger
            Main.LOGGER.error("Recruits: could not carry out the order {} on {}", order.getClass().getSimpleName(), companion.getUUID(), e);
        }
    }

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    public static CompoundTag save(CompanionOrder<?> order) {
        CompoundTag tag = new CompoundTag();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ((Message<?>) order).toBytes(buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            tag.putString("Type", order.getClass().getSimpleName());
            tag.putByteArray("Data", data);
        } finally {
            buf.release();
        }
        return tag;
    }

    /** @return null if the tag is not an order (any more), e.g. after an update that removed its packet */
    @Nullable
    public static CompanionOrder<?> load(CompoundTag tag) {
        Supplier<CompanionOrder<?>> factory = TYPES.get(tag.getString("Type"));
        if (factory == null) return null;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(tag.getByteArray("Data")));
        try {
            Object order = ((Message<?>) factory.get()).fromBytes(buf);
            return order instanceof CompanionOrder<?> companionOrder ? companionOrder : null;
        } catch (RuntimeException e) {
            Main.LOGGER.warn("Recruits: could not read a saved order of type {}", tag.getString("Type"), e);
            return null;
        } finally {
            buf.release();
        }
    }

    // ------------------------------------------------------------------
    // Screen data
    // ------------------------------------------------------------------

    /**
     * The synced values of the entity, packed like the vanilla packet does it. With them the client builds a copy
     * of a far companion that the companion screens can read, see {@code CompanionListScreen}.
     */
    public static byte[] packSynced(Entity entity) {
        List<SynchedEntityData.DataValue<?>> values = entity.getEntityData().getNonDefaultValues();
        if (values == null) values = List.of();
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new ClientboundSetEntityDataPacket(0, values).write(buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return data;
        } finally {
            buf.release();
        }
    }
}
