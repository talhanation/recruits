package com.talhanation.recruits;

import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Events für den Lebenszyklus von Claims. Werden auf dem
 * {@code MinecraftForge.EVENT_BUS} gepostet und sind server-side only.
 *
 * <p>Hinweis: {@code ClaimEvent.Updated} deckt sowohl die erstmalige Erstellung
 * eines Claims als auch spätere Änderungen (Chunks hinzugefügt/entfernt, Name,
 * Einstellungen etc.) ab. Ob es sich um einen neuen Claim handelt, lässt sich
 * mit {@link Updated#isNew()} abfragen.</p>
 *
 * <pre>
 *   {@code @SubscribeEvent}
 *   public void onClaimCreated(ClaimEvent.Updated event) {
 *       if (event.isNew()) {
 *           RecruitsClaim claim = event.getClaim();
 *           // Neuer Claim wurde registriert
 *       }
 *   }
 * </pre>
 */
public abstract class ClaimEvent extends Event {

    private final RecruitsClaim claim;
    private final ServerLevel level;

    protected ClaimEvent(RecruitsClaim claim, ServerLevel level) {
        this.claim = claim;
        this.level = level;
    }

    /** Der betroffene Claim. */
    public RecruitsClaim getClaim() {
        return claim;
    }

    /** Das ServerLevel, in dem das Event stattfindet. */
    public ServerLevel getLevel() {
        return level;
    }

    // -------------------------------------------------------------------------

    /**
     * Wird gefeuert, wenn ein Claim neu erstellt oder aktualisiert wird
     * (z.B. Chunks hinzugefügt, Name geändert, Schutzeinstellungen geändert).
     *
     * <p>Cancelable: {@code setCanceled(true)} verhindert das Speichern der Änderung.</p>
     *
     * @see com.talhanation.recruits.world.RecruitsClaimManager#addOrUpdateClaim(ServerLevel, RecruitsClaim)
     */
    @Cancelable
    public static class Updated extends ClaimEvent {
        private final boolean isNew;

        public Updated(RecruitsClaim claim, ServerLevel level, boolean isNew) {
            super(claim, level);
            this.isNew = isNew;
        }

        /**
         * {@code true} wenn der Claim gerade zum ersten Mal registriert wird,
         * {@code false} wenn ein bestehender Claim aktualisiert wurde.
         */
        public boolean isNew() {
            return isNew;
        }
    }

    /**
     * Wird gefeuert, kurz bevor ein Claim vollständig entfernt wird.
     * Zu diesem Zeitpunkt existiert der Claim noch im Manager.
     *
     * @see com.talhanation.recruits.world.RecruitsClaimManager#removeClaim(RecruitsClaim)
     */
    public static class Removed extends ClaimEvent {
        public Removed(RecruitsClaim claim, ServerLevel level) {
            super(claim, level);
        }
    }
}
