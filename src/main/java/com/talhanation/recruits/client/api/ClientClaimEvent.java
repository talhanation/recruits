package com.talhanation.recruits.client.api;

import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;


@OnlyIn(Dist.CLIENT)
public abstract class ClientClaimEvent extends Event {

    private final RecruitsClaim claim;

    protected ClientClaimEvent(RecruitsClaim claim) {
        this.claim = claim;
    }

    public RecruitsClaim getClaim() {
        return claim;
    }


    public static class Enter extends ClientClaimEvent {

        @Nullable
        private final RecruitsClaim previousClaim;

        public Enter(RecruitsClaim newClaim, @Nullable RecruitsClaim previousClaim) {
            super(newClaim);
            this.previousClaim = previousClaim;
        }

        @Nullable
        public RecruitsClaim getPreviousClaim() {
            return previousClaim;
        }
    }

    public static class Leave extends ClientClaimEvent {
        @Nullable
        private final RecruitsClaim nextClaim;

        public Leave(RecruitsClaim leftClaim, @Nullable RecruitsClaim nextClaim) {
            super(leftClaim);
            this.nextClaim = nextClaim;
        }

        @Nullable
        public RecruitsClaim getNextClaim() {
            return nextClaim;
        }
    }

    public static class DataUpdated extends ClientClaimEvent {

        private final boolean isCurrentClaim;

        public DataUpdated(RecruitsClaim claim, boolean isCurrentClaim) {
            super(claim);
            this.isCurrentClaim = isCurrentClaim;
        }

        public boolean isCurrentClaim() {
            return isCurrentClaim;
        }
    }

    @Cancelable
    public static class SiegeStarted extends ClientClaimEvent {
        public SiegeStarted(RecruitsClaim claim) {
            super(claim);
        }
    }

    public static class SiegeEnded extends ClientClaimEvent {
        private final boolean wasConquered;

        public SiegeEnded(RecruitsClaim claim, boolean wasConquered) {
            super(claim);
            this.wasConquered = wasConquered;
        }

        public boolean wasConquered() {
            return wasConquered;
        }
    }

    public static class HealthChanged extends ClientClaimEvent {
        private final int previousHealth;
        private final int newHealth;

        public HealthChanged(RecruitsClaim claim, int previousHealth, int newHealth) {
            super(claim);
            this.previousHealth = previousHealth;
            this.newHealth = newHealth;
        }

        public int getPreviousHealth() { return previousHealth; }
        public int getNewHealth() { return newHealth; }

        public boolean isDamage() { return newHealth < previousHealth; }
    }
}
