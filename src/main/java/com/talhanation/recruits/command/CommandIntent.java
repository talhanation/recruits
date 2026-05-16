package com.talhanation.recruits.command;

import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * First-class server-side representation of a player recruit command.
 *
 * <p>Network packets still define the wire format. Intents provide a common internal
 * contract that later dispatcher, queue, and audit-log work can consume uniformly.</p>
 */
public sealed interface CommandIntent {

    long issuedAtGameTime();

    int priority();

    boolean queueMode();

    CommandIntentType type();

    record Movement(
            long issuedAtGameTime,
            int priority,
            boolean queueMode,
            int movementState,
            int formation,
            boolean tight,
            boolean holdFormation,
            @Nullable Vec3 targetPos
    ) implements CommandIntent {
        @Override
        public CommandIntentType type() {
            return CommandIntentType.MOVEMENT;
        }
    }

    record Face(
            long issuedAtGameTime,
            int priority,
            boolean queueMode,
            int formation,
            boolean tight,
            boolean holdFormation
    ) implements CommandIntent {
        @Override
        public CommandIntentType type() {
            return CommandIntentType.FACE;
        }
    }

    record Attack(
            long issuedAtGameTime,
            int priority,
            boolean queueMode,
            UUID groupUuid
    ) implements CommandIntent {
        @Override
        public CommandIntentType type() {
            return CommandIntentType.ATTACK;
        }
    }

    record StrategicFire(
            long issuedAtGameTime,
            int priority,
            boolean queueMode,
            UUID groupUuid,
            boolean shouldFire
    ) implements CommandIntent {
        @Override
        public CommandIntentType type() {
            return CommandIntentType.STRATEGIC_FIRE;
        }
    }

    record Aggro(
            long issuedAtGameTime,
            int priority,
            boolean queueMode,
            int state,
            UUID groupUuid,
            boolean fromGui
    ) implements CommandIntent {
        @Override
        public CommandIntentType type() {
            return CommandIntentType.AGGRO;
        }
    }

    record SiegeMachine(
            long issuedAtGameTime,
            int priority,
            boolean queueMode,
            UUID mountUuid,
            UUID groupUuid,
            boolean returnToKnownMount
    ) implements CommandIntent {
        @Override
        public CommandIntentType type() {
            return CommandIntentType.SIEGE_MACHINE;
        }
    }
}
