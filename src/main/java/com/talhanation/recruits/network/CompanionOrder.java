package com.talhanation.recruits.network;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A packet from a companion's screen that is an order to this companion. Standing next to him it takes effect
 * at once, as before. From far away it is written down for a Messenger, who carries it to the companion
 * ({@link CompanionOrders}).
 * <p>
 * Implemented by the packet itself: its bytes are what the Messenger carries.
 */
public interface CompanionOrder<T extends AbstractRecruitEntity> {

    UUID getCompanion();

    Class<T> getCompanionType();

    /**
     * Carries the order out. Who may give it was checked before.
     * @param sender the player who gives the order himself, null when a Messenger delivers it - the owner may be
     *               offline then, and the companion may be out of the world (see {@link #needsWorld()})
     */
    void applyTo(T companion, @Nullable ServerPlayer sender);

    /**
     * true: the order looks at the world around the companion (nearby recruits, his ship). Delivered while he is on
     * a simulated journey or in an unloaded chunk, it waits until he is back in the world.
     */
    default boolean needsWorld() {
        return false;
    }
}
