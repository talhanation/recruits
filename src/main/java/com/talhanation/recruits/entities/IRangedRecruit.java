package com.talhanation.recruits.entities;

import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.item.ItemStack;

import java.util.Random;
import java.util.function.Predicate;

public interface IRangedRecruit extends RangedAttackMob {
    Random random = new Random();
    Predicate<ItemStack> getWeaponType();
    static double getAngleHeightModifier(double distance, double heightDiff, double modifier) {
        if(distance >= 2000){
            return heightDiff * (1.15 * modifier);
        }
        else if(distance >= 1750){
            return heightDiff * (1.05 * modifier);
        }
        else if(distance >= 1500){
            return heightDiff * (0.6 * modifier);
        }

        else if(distance >= 1250){
            return heightDiff * (0.5 * modifier);
        }

        else if(distance >= 1000){
            return heightDiff * (0.4 * modifier);
        }
        else if(distance >= 750){
            return heightDiff * (0.3 * modifier);
        }
        else if(distance >= 500){
            return heightDiff * (0.2 * modifier);
        }
        else
            return 0;
    }

    static double getCrossbowAngleHeightModifier(double distance, double heightDiff) {
        if(distance >= 2500){
            return heightDiff * (0.3);
        }
        else if(distance >= 2000){
            return heightDiff * (0.25);
        }
        else if(distance >= 1750){
            return heightDiff * (0.2);
        }
        else if(distance >= 1500){
            return heightDiff * (0.15);
        }
        else if(distance >= 1250){
            return heightDiff * (0.125);
        }
        else if(distance >= 1000){
            return heightDiff * (0.1);
        }
        else if(distance >= 750){
            return heightDiff * (0.05);
        }
        else if(distance >= 500){
            return heightDiff * (0.025);
        }
        else
            return 0;
    }

    static double getAngleDistanceModifier(double distance, int x, int random) {
        double modifier = distance/x;
        return (modifier - IRangedRecruit.random.nextInt(-random, random)) /100;
    }

    static float getForceDistanceModifier(double distance, double base) {
        double modifier = 0;
        if(distance > 4000){
            modifier = base * 0.09;
        }
        else if(distance > 3750){
            modifier = base * 0.075;
        }
        else if(distance > 3500){
            modifier = base * 0.055;
        }
        else if(distance > 3000){
            modifier = base * 0.030;
        }
        else if(distance > 2500){
            modifier = base * 0.010;
        }

        return (float) modifier;
    }

    static double getCannonAngleDistanceModifier(double distanceSqr, int randomSpread) {
        return calcCannonAngle(distanceSqr, 0, randomSpread);
    }

    static double getCannonAngleHeightModifier(double distanceSqr, double heightDiff) {
        // Old method returned raw value that was divided by 100 at call site
        // New: return 0 since height is handled in calcCannonAngle directly
        return 0;
    }

    // ========================= CANNON ANGLE (PHYSICS) =========================
    // Cannon: speed=3.2, gravity=0.06, drag=0.99 (standard arrow physics)
    // Returns yShootVec offset (not degrees, not range)

    /**
     * Calculates the yShootVec offset for a cannon to hit a target.
     * Uses flight-time physics to account for gravity drop over distance.
     *
     * @param distanceSqr squared distance to target (from distanceToSqr)
     * @param heightDiff target Y minus cannon Y
     * @param randomSpread random spread amount (0 = perfect, 2 = normal)
     * @return yShootVec offset value
     */
    static double calcCannonAngle(double distanceSqr, double heightDiff, int randomSpread) {
        double horizontalDist = Math.sqrt(distanceSqr);
        double speed = 3.2;
        double gravity = 0.065;
        double drag = 0.99;

        double flightTime = calcFlightTime(horizontalDist, speed, drag);
        double drop = calcGravityDrop(flightTime, gravity, drag);

        double aimHeight = heightDiff + drop;
        double angle = aimHeight / horizontalDist;

        if(randomSpread > 0){
            angle += (IRangedRecruit.random.nextInt(-randomSpread, randomSpread + 1)) / 100.0;
        }

        return angle;
    }


    /*
     *   0 Range == 1123 Distance
     *  25 Range == 2863 Distance
     *  50 Range == 6322 Distance
     *  75 Range == 10922 Distance
     * 100 Range == 18095 Distance
     */
    /* Height Diff
     *  1123 Distance = diff == 1.25 Range
     *  6322 Distance = diff == 1.25 Range
     *  10922 Distance = diff == 0.75 Range
     *  18095 Distance = diff == 0.40 Range
     */
    static float calcBaseRangeForCatapult(float distance) {
        float[] distances = {1123, 2863, 6322, 10922, 18095};
        float[] ranges =    {0,    25,   50,   75,    100};

        if (distance <= distances[0]) return ranges[0];
        if (distance >= distances[distances.length - 1]) return ranges[ranges.length - 1];

        for (int i = 0; i < distances.length - 1; i++) {
            float d0 = distances[i];
            float d1 = distances[i + 1];
            float r0 = ranges[i];
            float r1 = ranges[i + 1];

            if (distance >= d0 && distance <= d1) {
                float factor = (distance - d0) / (d1 - d0);
                return r0 + factor * (r1 - r0);
            }
        }

        return 0;
    }

    // ========================= UNIVERSAL PROJECTILE PHYSICS =========================

    /*
     * Flight-time based approach:
     *   distance = speed * (1 - drag^t) / (1 - drag)
     *   solved:  t = ln(1 - dist * (1 - drag) / speed) / ln(drag)
     *   drop = (gravity / (1 - drag)) * (t - (1 - drag^t) / (1 - drag))
     */

    static double calcFlightTime(double horizontalDist, double speed, double drag) {
        double a = 1.0 - drag;
        double factor = 1.0 - horizontalDist * a / speed;
        if(factor <= 0.01) factor = 0.01;
        return Math.log(factor) / Math.log(drag);
    }

    static double calcGravityDrop(double flightTime, double gravity, double drag) {
        double a = 1.0 - drag;
        double dragPowT = Math.pow(drag, flightTime);
        return (gravity / a) * (flightTime - (1.0 - dragPowT) / a);
    }

    // ========================= BALLISTA PITCH =========================
    // speed=3.5, gravity=0.05, drag=0.99, shootHeight=+1.3

    static float calcBallistaPitchAngle(double horizontalDist, double heightDiff) {
        double speed = 3.5;
        double gravity = 0.05;
        double drag = 0.99;

        double flightTime = calcFlightTime(horizontalDist, speed, drag);
        double drop = calcGravityDrop(flightTime, gravity, drag);

        double aimHeight = heightDiff - 1.3 + drop;

        float pitch = (float) -Math.toDegrees(Math.atan2(aimHeight, horizontalDist));
        return Math.max(-40F, Math.min(40F, pitch));
    }

    // ========================= CATAPULT RANGE =========================
    // gravity=0.06, drag=0.99, shootHeight=+3.75, yShootVec = forward.y + 0.875
    // horizontal_factor ≈ 0.753, vertical_factor ≈ 0.659
    // speed = 1.5 + 2.0 * range * 0.01

    static final double CATAPULT_GRAVITY = 0.06;
    static final double CATAPULT_DRAG = 0.99;
    static final double CATAPULT_HORIZONTAL_FACTOR = 0.753;
    static final double CATAPULT_VERTICAL_FACTOR = 0.659;

    static float calcCatapultRange(float distanceSqr, float heightDiff) {
        double horizontalDist = Math.sqrt(distanceSqr);
        double adjustedHeightDiff = heightDiff - 3.75;

        double lowSpeed = 1.5;
        double highSpeed = 3.5;
        double bestSpeed = 2.0;
        double bestError = Double.MAX_VALUE;

        for(int iter = 0; iter < 15; iter++){
            double midSpeed = (lowSpeed + highSpeed) / 2.0;

            double hSpeed = midSpeed * CATAPULT_HORIZONTAL_FACTOR;
            double vSpeed = midSpeed * CATAPULT_VERTICAL_FACTOR;

            double flightTime = calcFlightTime(horizontalDist, hSpeed, CATAPULT_DRAG);
            double drop = calcGravityDrop(flightTime, CATAPULT_GRAVITY, CATAPULT_DRAG);
            double rise = vSpeed * (1.0 - Math.pow(CATAPULT_DRAG, flightTime)) / (1.0 - CATAPULT_DRAG);
            double landingHeight = rise - drop;

            double error = landingHeight - adjustedHeightDiff;

            if(Math.abs(error) < bestError){
                bestError = Math.abs(error);
                bestSpeed = midSpeed;
            }

            if(error > 0) highSpeed = midSpeed;
            else lowSpeed = midSpeed;
        }

        float range = (float) ((bestSpeed - 1.5) / 0.02);
        return Math.max(0F, Math.min(100F, range));
    }

}
