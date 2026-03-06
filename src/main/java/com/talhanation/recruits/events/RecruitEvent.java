package com.talhanation.recruits;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

/**
 * Events für individuelle Recruits. Alle Sub-Events werden auf dem
 * {@code MinecraftForge.EVENT_BUS} gepostet und sind server-side only.
 *
 * <pre>
 *   {@code @SubscribeEvent}
 *   public void onRecruitHired(RecruitEvent.Hired event) {
 *       AbstractRecruitEntity recruit = event.getRecruit();
 *       Player owner = event.getPlayer();
 *   }
 * </pre>
 */
public abstract class RecruitEvent extends Event {

    private final AbstractRecruitEntity recruit;

    protected RecruitEvent(AbstractRecruitEntity recruit) {
        this.recruit = recruit;
    }

    /** Der betroffene Recruit. */
    public AbstractRecruitEntity getRecruit() {
        return recruit;
    }

    // -------------------------------------------------------------------------

    /**
     * Wird gefeuert, kurz bevor ein Recruit angeheuert wird.
     * <p>Cancelable: {@code setCanceled(true)} verhindert das Anheuern.</p>
     * <p>Der zugehörige Spieler ist immer gesetzt.</p>
     */
    @Cancelable
    public static class Hired extends RecruitEvent {
        private final Player player;

        public Hired(AbstractRecruitEntity recruit, Player player) {
            super(recruit);
            this.player = player;
        }

        /** Der Spieler, der den Recruit anheuert. */
        public Player getPlayer() {
            return player;
        }
    }

    /**
     * Wird gefeuert, kurz bevor ein Recruit entlassen wird (disband).
     * <p>Cancelable: {@code setCanceled(true)} verhindert das Entlassen.</p>
     *
     * @see AbstractRecruitEntity#disband(Player, boolean, boolean)
     */
    @Cancelable
    public static class Dismissed extends RecruitEvent {

        @Nullable
        private final Player player;
        private final boolean keepTeam;

        public Dismissed(AbstractRecruitEntity recruit, @Nullable Player player, boolean keepTeam) {
            super(recruit);
            this.player = player;
            this.keepTeam = keepTeam;
        }

        /**
         * Der Spieler, der den Recruit entlässt, oder {@code null} wenn
         * das Entlassen automatisch (z.B. durch Traktate/NPCs) geschieht.
         */
        @Nullable
        public Player getPlayer() {
            return player;
        }

        /** Ob der Recruit nach dem Entlassen im Team verbleibt. */
        public boolean isKeepTeam() {
            return keepTeam;
        }
    }

    /**
     * Wird gefeuert, wenn ein Recruit ein neues Level erreicht.
     * Zum Zeitpunkt des Events wurde das Level bereits erhöht.
     *
     * @see AbstractRecruitEntity#checkLevel()
     */
    public static class LevelUp extends RecruitEvent {
        private final int newLevel;

        public LevelUp(AbstractRecruitEntity recruit, int newLevel) {
            super(recruit);
            this.newLevel = newLevel;
        }

        /** Das neue Level des Recruits. */
        public int getNewLevel() {
            return newLevel;
        }
    }

    /**
     * Wird gefeuert, kurz bevor ein Recruit zu einem höheren Rang befördert wird.
     * <p>Cancelable: {@code setCanceled(true)} verhindert die Beförderung.</p>
     *
     * @see RecruitEvents#promoteRecruit(AbstractRecruitEntity, int, String, net.minecraft.server.level.ServerPlayer)
     */
    @Cancelable
    public static class Promoted extends RecruitEvent {
        private final int newProfession;
        private final String newName;
        private final net.minecraft.server.level.ServerPlayer promotingPlayer;

        public Promoted(AbstractRecruitEntity recruit, int newProfession, String newName,
                        net.minecraft.server.level.ServerPlayer promotingPlayer) {
            super(recruit);
            this.newProfession = newProfession;
            this.newName = newName;
            this.promotingPlayer = promotingPlayer;
        }

        /** Profession-ID des neuen Rangs (0=Messenger, 1=Scout, 2=PatrolLeader, 3=Captain). */
        public int getNewProfession() {
            return newProfession;
        }

        /** Der neue Name des beförderten Recruits. */
        public String getNewName() {
            return newName;
        }

        /** Der Spieler, der die Beförderung durchführt. */
        public net.minecraft.server.level.ServerPlayer getPromotingPlayer() {
            return promotingPlayer;
        }
    }
}
