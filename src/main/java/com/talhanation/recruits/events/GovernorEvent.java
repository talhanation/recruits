package com.talhanation.recruits;

import com.talhanation.recruits.entities.GovernorEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

/**
 * Governor events on the {@code MinecraftForge.EVENT_BUS}, server-side only.
 *
 * <pre>
 *   {@code @SubscribeEvent}
 *   public void onTaxCollected(GovernorEvent.TaxCollected event) {
 *       // e.g. lower the owner's village reputation by event.getReputationPenalty()
 *   }
 * </pre>
 */
public abstract class GovernorEvent extends Event {

    private final GovernorEntity governor;
    private final RecruitsClaim claim;
    private final ServerLevel level;

    protected GovernorEvent(GovernorEntity governor, RecruitsClaim claim, ServerLevel level) {
        this.governor = governor;
        this.claim = claim;
        this.level = level;
    }

    public GovernorEntity getGovernor() {
        return governor;
    }

    public RecruitsClaim getClaim() {
        return claim;
    }

    public ServerLevel getLevel() {
        return level;
    }

    /**
     * Fired once per day when the Governor collected the taxes of the village in his claim.
     * The reputation penalty comes from the config (tax level 1/2/3); the reputation system of the
     * noble rework is meant to subscribe here.
     */
    public static class TaxCollected extends GovernorEvent {
        private final UUID owner;
        private final int taxLevel;
        private final int amount;
        private final int inhabitants;
        private final int reputationPenalty;

        public TaxCollected(GovernorEntity governor, RecruitsClaim claim, ServerLevel level, UUID owner,
                            int taxLevel, int amount, int inhabitants, int reputationPenalty) {
            super(governor, claim, level);
            this.owner = owner;
            this.taxLevel = taxLevel;
            this.amount = amount;
            this.inhabitants = inhabitants;
            this.reputationPenalty = reputationPenalty;
        }

        public UUID getOwner() {
            return owner;
        }

        public int getTaxLevel() {
            return taxLevel;
        }

        public int getAmount() {
            return amount;
        }

        public int getInhabitants() {
            return inhabitants;
        }

        public int getReputationPenalty() {
            return reputationPenalty;
        }
    }
}
