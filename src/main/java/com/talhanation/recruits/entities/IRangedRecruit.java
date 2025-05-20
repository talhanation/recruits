package com.talhanation.recruits.entities;

import com.talhanation.recruits.Main;
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

    static float calcRangeForCatapult(SiegeEngineerEntity siegeEngineer, float distance) {
        if (distance <= 1000) return 0;
        if (distance >= 15000) return 100;

        // Linearisierung über eine Wurzel-Kurve
        float minDist = 1000;
        float maxDist = 15000;
        float normDist = (distance - minDist) / (maxDist - minDist); // → 0 bis 1

        // Wurzelfunktion, die langsamer ansteigt
        float curved = (float) Math.sqrt(normDist);

        // Skalierung auf Zielbereich
        float result = curved * 88F;//TODO: Need test higher distances

        // Optional kleine Zufallsabweichung (max ±2) 
        //result += siegeEngineer.getRandom().nextInt(3) - siegeEngineer.getRandom().nextInt(3);

        return Math.min(result, 100);
    }

    /*
    static float calcRangeForCatapult(SiegeEngineerEntity siegeEngineer, float distance) {
        if(distance < 1000) return 0;
        float range = 0;
        if(distance < 1500){
            range =+ 5;
        }
        else if(distance < 2000){
            range =+ 20;
        }
        else if(distance < 2500){
            range =+ 25;
        }
        else if(distance < 3000){
            range =+ 30;
        }
        else if(distance < 3500){
            range =+ 25;
        }
        else if(distance < 4000){
            range =+ 30;
        }
        else if(distance < 4500){
            range =+ 35;
        }
        else if(distance < 5000){
            range =+ 40;
        }
        else if(distance < 5500){
            range =+ 45;
        }
        else if(distance < 6000){
            range =+ 48;
        }
        else if(distance < 6500){
            range =+ 52;
        }
        else if(distance < 7000){
            range =+ 50;
        }
        else if(distance < 7500){
            range =+ 52;
        }
        else if(distance < 8000){
            range =+ 55;
        }
        else if(distance < 8500){
            range =+ 57;
        }
        else if(distance < 9000){
            range =+ 62;
        }
        else if(distance < 9500){
            range =+ 60;
        }
        else if(distance < 10000){
            range =+ 68;
        }
        else if(distance < 10500){
            range =+ 70;
        }
        else if(distance < 11000){
            range =+ 72;
        }
        else if(distance < 11500){
            range =+ 75;
        }
        else if(distance < 12000){
            range =+ 77;
        }
        else if(distance < 12500){
            range =+ 80;
        }
        else if(distance < 13000){
            range =+ 82;
        }
        else if(distance < 13500){
            range =+ 85;
        }
        else if(distance < 14000){
            range =+ 87;
        }
        else if(distance < 14500){
            range =+ 90;
        }
        else if(distance < 15000){
            range =+ 92;
        }
        else if(distance < 15500){
            range =+ 95;
        }
        else if(distance < 16000){
            range =+ 98;
        }
        else{
            range =+ 100;
        }

        return range;// + siegeEngineer.getRandom().nextInt(2) - siegeEngineer.getRandom().nextInt(4);
    }

     */
}
