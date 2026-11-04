package com.talhanation.recruits.api.governor;

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

    /** Data for the Governor screen. Written to {@code modules/<id>} of the snapshot if not empty. */
    default void writeSnapshot(GovernorContext ctx, CompoundTag tag) {
    }

    /** Lines for the daily report, right after {@link #onDailyCheck}. */
    default void appendReport(GovernorContext ctx, GovernorReport report) {
    }
}
