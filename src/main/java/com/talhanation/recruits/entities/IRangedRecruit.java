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
}
