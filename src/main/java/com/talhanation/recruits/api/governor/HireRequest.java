package com.talhanation.recruits.api.governor;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * A module asks the Governor to hire a unit of an entity type for his owner, e.g. a farmer for a new farm.
 * The Governor walks to a free unit in the claim or to a noble that offers it, pays from the treasury
 * (within the daily budget) and calls {@link #onHired}. Module requests are served before the garrison.
 * <p>
 * Requests live in the running Governor only. They are dropped when the Governor is unloaded or after
 * {@code validDays} days; a module simply asks again at its next daily check.
 */
public class HireRequest {

    private final String key;
    private final ResourceLocation entityId;
    private final int priority;
    private final Consumer<AbstractRecruitEntity> onHired;
    private final int validDays;

    private long expiresDay = Long.MAX_VALUE;
    /** Why it was not served yet, key below {@code gui.recruits.governor.recruit_status}, for the module's report. */
    private String status = "waiting";

    /**
     * @param key      unique per claim and purpose, a new request with the same key replaces the old one
     * @param entityId entity type to hire, e.g. {@code workers:farmer}
     * @param priority higher first
     * @param onHired  called right after the unit was spawned or hired, owner and team are set already
     */
    public HireRequest(String key, ResourceLocation entityId, int priority, Consumer<AbstractRecruitEntity> onHired) {
        this(key, entityId, priority, 1, onHired);
    }

    public HireRequest(String key, ResourceLocation entityId, int priority, int validDays, Consumer<AbstractRecruitEntity> onHired) {
        this.key = key;
        this.entityId = entityId;
        this.priority = priority;
        this.validDays = Math.max(1, validDays);
        this.onHired = onHired;
    }

    public String getKey() {
        return key;
    }

    public ResourceLocation getEntityId() {
        return entityId;
    }

    public int getPriority() {
        return priority;
    }

    public Consumer<AbstractRecruitEntity> getOnHired() {
        return onHired;
    }

    /** "waiting", "hiring", "no_source", "too_expensive", "locked", "no_villagers", "no_budget", "limit" ... */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getValidDays() {
        return validDays;
    }

    public long getExpiresDay() {
        return expiresDay;
    }

    public void setExpiresDay(long day) {
        this.expiresDay = day;
    }
}
