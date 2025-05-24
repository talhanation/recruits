package com.talhanation.recruits.entities;

import net.minecraft.world.entity.monster.RangedAttackMob;

import java.util.Random;

public interface IRangedRecruit extends RangedAttackMob {
    Random random = new Random();
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

    static double getCannonAngleDistanceModifier(double distance, int random) {
        double modifier = 0;
        //smaller modifier = bigger angle
        //higher modifier = smaller angle
        if(distance > 4500){
            modifier = 260;
        }
        else if(distance > 4000){
            modifier = 230;
        }
        else if(distance > 3500){
            modifier = 220;
        }
        else if(distance > 3000){
            modifier = 190;
        }
        else if(distance > 2600){
            modifier = 170;
        }
        else if(distance > 2400){
            modifier = 160;
        }
        else if(distance > 2250){
            modifier = 150;
        }
        else if(distance > 2000){
            modifier = 150;
        }
        else if(distance > 1750){
            modifier = 130;
        }
        else if(distance > 1500){
            modifier = 130;
        }
        else if(distance > 1350){
            modifier = 115;
        }
        else if(distance > 1250){
            modifier = 110;
        }
        else if(distance > 1100){
            modifier = 110;
        }
        else if(distance > 1000) {
            modifier = 110;
        }
        else if(distance > 600) {
            modifier = 120;
        }
        else
            modifier = 135;

        return (distance/modifier - IRangedRecruit.random.nextInt(-random, random)) /100;
    }

    static double getCannonAngleHeightModifier(double distance, double heightDiff) {
        return heightDiff * (2.55);
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

    static float applyPositiveHeightCorrection(float distance, float heightDiff) {
        float[] distances = {1123f, 6322f, 10922f, 18095f};
        float[] factors = {1.25f, 1.0f, 0.75f, 0.40f};

        if (distance <= distances[0]) return heightDiff * factors[0];
        if (distance >= distances[distances.length - 1]) return heightDiff * factors[factors.length - 1];

        for (int i = 0; i < distances.length - 1; i++) {
            float d0 = distances[i];
            float d1 = distances[i + 1];
            float f0 = factors[i];
            float f1 = factors[i + 1];

            if (distance >= d0 && distance <= d1) {
                float t = (distance - d0) / (d1 - d0);
                float interpolatedFactor = f0 + t * (f1 - f0);
                return heightDiff * interpolatedFactor;
            }
        }

        return heightDiff;
    }

    static float applyNegativeHeightCorrection(float distance, float heightDiff) {//TEST
        float[] distances = {1123f, 5620,  6322f, 10922f, 18095f};
        float[] factors = {1.15f,  1.35f ,1.25f, 0.85f, 0.60f};

        if (distance <= distances[0]) return heightDiff * factors[0];
        if (distance >= distances[distances.length - 1]) return heightDiff * factors[factors.length - 1];

        for (int i = 0; i < distances.length - 1; i++) {
            float d0 = distances[i];
            float d1 = distances[i + 1];
            float f0 = factors[i];
            float f1 = factors[i + 1];

            if (distance >= d0 && distance <= d1) {
                float t = (distance - d0) / (d1 - d0);
                float interpolatedFactor = f0 + t * (f1 - f0);
                return heightDiff * interpolatedFactor;
            }
        }

        return heightDiff;
    }
}
