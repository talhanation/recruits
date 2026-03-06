package com.talhanation.recruits;

import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Siege-Events werden auf dem {@code MinecraftForge.EVENT_BUS} gepostet
 * und können von anderen Mods abonniert werden.
 *
 * <pre>
 *   {@code @SubscribeEvent}
 *   public void onSiegeStart(SiegeEvent.Start event) {
 *       // event.getClaim(), event.getLevel()
 *       // event.setCanceled(true) verhindert den Siege-Start
 *   }
 * </pre>
 */
public abstract class SiegeEvent extends Event {

    private final RecruitsClaim claim;
    private final ServerLevel level;

    protected SiegeEvent(RecruitsClaim claim, ServerLevel level) {
        this.claim = claim;
        this.level = level;
    }

    /** Der Claim, der belagert wird / wurde. */
    public RecruitsClaim getClaim() {
        return claim;
    }

    /** Das ServerLevel, in dem der Siege stattfindet. */
    public ServerLevel getLevel() {
        return level;
    }

    // -------------------------------------------------------------------------

    /**
     * Wird gefeuert, kurz bevor ein Siege startet.
     * <p>Ist cancelable: {@code event.setCanceled(true)} verhindert den Start.</p>
     */
    @Cancelable
    public static class Start extends SiegeEvent {
        public Start(RecruitsClaim claim, ServerLevel level) {
            super(claim, level);
        }
    }

    /**
     * Wird gefeuert, wenn ein laufender Siege endet, ohne dass der
     * Angreifer gewonnen hat (z.B. zu wenig Angreifer).
     */
    public static class End extends SiegeEvent {
        public End(RecruitsClaim claim, ServerLevel level) {
            super(claim, level);
        }
    }

    /**
     * Wird gefeuert, wenn ein Angreifer den Claim erfolgreich erobert hat.
     * Zu diesem Zeitpunkt wurde der Besitzer des Claims bereits gewechselt.
     */
    public static class Success extends SiegeEvent {
        public Success(RecruitsClaim claim, ServerLevel level) {
            super(claim, level);
        }
    }
}
