package com.talhanation.recruits.api.governor;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * An addon that gives the Governor more work, e.g. the production part of Workers.
 * Register it once with {@link GovernorModules#register} during mod setup.
 * <p>
 * All hooks run on the server thread. Keep them cheap: {@link #onInterval} runs for every ruling Governor
 * every few seconds. Heavy work belongs into {@link #onDailyCheck} or should be spread over several calls.
 * A module that throws is logged and skipped, the Governor keeps working.
 * <p>
 * Units a module takes from a handed-over group (workers) join the garrison: no player commands, upkeep and
 * pay from the Governor's posts, counted against the garrison limit. Only the military types guard and patrol.
 * <p>
 * Hooks for unloaded claims ({@link #onSimulatedDay}, {@link #onClaimLoaded}, {@link #onClaimUnloaded}) exist
 * already so addons compile against this version; Recruits starts calling them with the simulation of
 * unloaded claims.
 */
public interface IGovernorModule {

    /** Unique id, also the key of the module's data in the claim ({@link GovernorContext#getModuleData()}). */
    ResourceLocation id();

    /** Every few seconds while the Governor rules a loaded claim (slow tick, about every 200 ticks). */
    default void onInterval(GovernorContext ctx) {
    }

    /**
     * Planning: about once per minute while the Governor rules a loaded claim, and right away when the owner
     * opens the Governor screen (at most every few seconds). Decide what to build, hire and trade here.
     */
    default void onUpdate(GovernorContext ctx) {
    }

    /** Once per in-game day at the villagers' meeting, claim loaded. */
    default void onDailyCheck(GovernorContext ctx) {
    }

    /** Once per in-game day for a claim whose Governor is not loaded. Not called yet. */
    default void onSimulatedDay(GovernorContext ctx) {
    }

    /** The Governor of the claim was loaded again (apply what was simulated). Not called yet. */
    default void onClaimLoaded(GovernorContext ctx) {
    }

    /** The Governor of the claim was unloaded (take a snapshot of chests and treasury). Not called yet. */
    default void onClaimUnloaded(GovernorContext ctx) {
    }

    /** The claim was conquered. Runs before the Governor stops ruling; the claim already has its new owner. */
    default void onOwnerChanged(ServerLevel level, RecruitsClaim claim, @Nullable UUID oldOwner, @Nullable UUID newOwner) {
    }

    /**
     * Data for the Governor screen. Written to {@code modules/<id>} of the snapshot if not empty.
     * A list {@code units} in it is also shown in the garrison tab, one row per unit:
     * {@code {type: entity type id, name: name, status: text component as JSON}}.
     */
    default void writeSnapshot(GovernorContext ctx, CompoundTag tag) {
    }

    /**
     * The owner hands a group over to the Governor and this unit can not serve in the garrison (e.g. a worker).
     * Return true if the module takes the unit, e.g. as a worker of the claim.
     */
    default boolean onUnitHandedOver(GovernorContext ctx, AbstractRecruitEntity unit) {
        return false;
    }

    /** A unit of the garrison that this module took ({@link #onUnitHandedOver}) went back to the owner. */
    default void onUnitReleased(GovernorContext ctx, AbstractRecruitEntity unit) {
    }

    /** Lines for the daily report, right after {@link #onDailyCheck}. */
    default void appendReport(GovernorContext ctx, GovernorReport report) {
    }
}
