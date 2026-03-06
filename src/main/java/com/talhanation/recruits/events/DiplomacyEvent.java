package com.talhanation.recruits;

import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Events für Diplomatie-Änderungen zwischen Fraktionen. Werden auf dem
 * {@code MinecraftForge.EVENT_BUS} gepostet und sind server-side only.
 *
 * <pre>
 *   {@code @SubscribeEvent}
 *   public void onRelationChanged(DiplomacyEvent.RelationChanged event) {
 *       if (event.getNewStatus() == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY) {
 *           // Krieg begonnen!
 *       }
 *   }
 * </pre>
 */
public abstract class DiplomacyEvent extends Event {

    private final String factionA;
    private final String factionB;
    private final ServerLevel level;

    protected DiplomacyEvent(String factionA, String factionB, ServerLevel level) {
        this.factionA = factionA;
        this.factionB = factionB;
        this.level = level;
    }

    /** String-ID der ersten Fraktion (initiiert die Änderung). */
    public String getFactionA() {
        return factionA;
    }

    /** String-ID der zweiten Fraktion. */
    public String getFactionB() {
        return factionB;
    }

    /** Das ServerLevel, in dem die Änderung stattfindet. */
    public ServerLevel getLevel() {
        return level;
    }

    // -------------------------------------------------------------------------

    /**
     * Wird gefeuert, kurz bevor die diplomatische Beziehung zwischen zwei
     * Fraktionen geändert wird.
     * <p>Cancelable: {@code setCanceled(true)} verhindert die Änderung.</p>
     *
     * @see RecruitsDiplomacyManager#setRelation(String, String, RecruitsDiplomacyManager.DiplomacyStatus, ServerLevel)
     */
    @Cancelable
    public static class RelationChanged extends DiplomacyEvent {

        private final RecruitsDiplomacyManager.DiplomacyStatus oldStatus;
        private final RecruitsDiplomacyManager.DiplomacyStatus newStatus;

        public RelationChanged(String factionA, String factionB, ServerLevel level,
                               RecruitsDiplomacyManager.DiplomacyStatus oldStatus,
                               RecruitsDiplomacyManager.DiplomacyStatus newStatus) {
            super(factionA, factionB, level);
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
        }

        /** Der bisherige Diplomatie-Status. */
        public RecruitsDiplomacyManager.DiplomacyStatus getOldStatus() {
            return oldStatus;
        }

        /** Der neue Diplomatie-Status, der gesetzt werden soll. */
        public RecruitsDiplomacyManager.DiplomacyStatus getNewStatus() {
            return newStatus;
        }
    }
}
