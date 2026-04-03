package com.talhanation.recruits.entities;

/**
 * Implement this interface on any entity (e.g. Workers-Addon merchants)
 * that should be blocked when the interacting player has an active embargo
 * against the entity's team.
 */
public interface ICanTradeEmbargo {

    /**
     * @return the scoreboard team-ID (faction stringID) this entity belongs to,
     *         or {@code null} if the entity has no team affiliation.
     */
    String getEmbargoTeamID();
}
