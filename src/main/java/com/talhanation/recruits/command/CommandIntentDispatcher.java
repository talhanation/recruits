package com.talhanation.recruits.command;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Single server-side entry point for recruit command intents.
 */
public final class CommandIntentDispatcher {
    private CommandIntentDispatcher() {
    }

    public static CommandIntent dispatch(@Nullable ServerPlayer player, CommandIntent intent, List<AbstractRecruitEntity> actors) {
        if (intent == null) {
            return null;
        }
        List<AbstractRecruitEntity> safeActors = actors == null ? List.of() : actors;
        CommandIntentLog.instance().record(player, intent, safeActors.size());
        if (player == null || safeActors.isEmpty()) {
            return intent;
        }
        applyIntentDirectly(player, intent, safeActors);
        return intent;
    }

    static void applyIntentDirectly(ServerPlayer player, CommandIntent intent, List<AbstractRecruitEntity> actors) {
        if (intent instanceof CommandIntent.Movement move) {
            CommandEvents.onMovementCommand(player, actors, move.movementState(), move.formation(), move.tight(), move.holdFormation());
        } else if (intent instanceof CommandIntent.Face face) {
            CommandEvents.onFaceCommand(player, actors, face.formation(), face.tight(), face.holdFormation());
        } else if (intent instanceof CommandIntent.Attack attack) {
            CommandEvents.onAttackCommand(player, player.getUUID(), actors, attack.groupUuid());
        } else if (intent instanceof CommandIntent.StrategicFire fire) {
            for (AbstractRecruitEntity recruit : actors) {
                CommandEvents.onStrategicFireCommand(player, player.getUUID(), recruit, fire.groupUuid(), fire.shouldFire());
            }
        } else if (intent instanceof CommandIntent.Aggro aggro) {
            for (AbstractRecruitEntity recruit : actors) {
                CommandEvents.onAggroCommand(player.getUUID(), recruit, aggro.state(), aggro.groupUuid(), aggro.fromGui());
            }
        } else if (intent instanceof CommandIntent.SiegeMachine siegeMachine) {
            for (AbstractRecruitEntity recruit : actors) {
                CommandEvents.onMountButton(player.getUUID(), recruit, siegeMachine.returnToKnownMount() ? null : siegeMachine.mountUuid(), siegeMachine.groupUuid());
            }
        }
    }
}
