package com.talhanation.recruits;

import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

/**
 * Events für den Lebenszyklus von Fraktionen. Alle Sub-Events werden auf dem
 * {@code MinecraftForge.EVENT_BUS} gepostet und sind server-side only.
 *
 * <pre>
 *   {@code @SubscribeEvent}
 *   public void onFactionCreated(FactionEvent.Created event) {
 *       RecruitsFaction faction = event.getFaction();
 *       ServerPlayer leader = event.getCreator();
 *   }
 * </pre>
 */
public abstract class FactionEvent extends Event {

    private final RecruitsFaction faction;
    private final ServerLevel level;

    protected FactionEvent(RecruitsFaction faction, ServerLevel level) {
        this.faction = faction;
        this.level = level;
    }

    /** Die betroffene Fraktion. */
    public RecruitsFaction getFaction() {
        return faction;
    }

    /** Das ServerLevel, in dem das Event stattfindet. */
    public ServerLevel getLevel() {
        return level;
    }

    // -------------------------------------------------------------------------

    /**
     * Wird gefeuert, kurz nachdem eine Fraktion erfolgreich erstellt wurde.
     * <p>Cancelable: {@code setCanceled(true)} rollt die Erstellung zurück.</p>
     */
    @Cancelable
    public static class Created extends FactionEvent {
        @Nullable
        private final ServerPlayer creator;

        public Created(RecruitsFaction faction, ServerLevel level, @Nullable ServerPlayer creator) {
            super(faction, level);
            this.creator = creator;
        }

        /**
         * Der Spieler, der die Fraktion erstellt hat, oder {@code null} wenn
         * sie per Konsolen-Command erstellt wurde.
         */
        @Nullable
        public ServerPlayer getCreator() {
            return creator;
        }
    }

    /**
     * Wird gefeuert, kurz bevor eine Fraktion aufgelöst wird.
     * Zu diesem Zeitpunkt existiert die Fraktion noch.
     */
    public static class Disbanded extends FactionEvent {
        public Disbanded(RecruitsFaction faction, ServerLevel level) {
            super(faction, level);
        }
    }

    /**
     * Wird gefeuert, kurz bevor ein Spieler einer Fraktion beitritt.
     * <p>Cancelable: {@code setCanceled(true)} verhindert den Beitritt.</p>
     */
    @Cancelable
    public static class PlayerJoined extends FactionEvent {
        private final ServerPlayer player;

        public PlayerJoined(RecruitsFaction faction, ServerLevel level, ServerPlayer player) {
            super(faction, level);
            this.player = player;
        }

        /** Der Spieler, der der Fraktion beitritt. */
        public ServerPlayer getPlayer() {
            return player;
        }
    }

    /**
     * Wird gefeuert, wenn ein Spieler eine Fraktion verlässt.
     * Zu diesem Zeitpunkt ist der Spieler noch Mitglied.
     */
    public static class PlayerLeft extends FactionEvent {
        private final ServerPlayer player;
        private final boolean wasLeader;

        public PlayerLeft(RecruitsFaction faction, ServerLevel level, ServerPlayer player, boolean wasLeader) {
            super(faction, level);
            this.player = player;
            this.wasLeader = wasLeader;
        }

        /** Der Spieler, der die Fraktion verlässt. */
        public ServerPlayer getPlayer() {
            return player;
        }

        /**
         * Ob der austretende Spieler der Anführer war.
         * Falls {@code true}, wird die Fraktion im Anschluss aufgelöst.
         */
        public boolean wasLeader() {
            return wasLeader;
        }
    }
}
