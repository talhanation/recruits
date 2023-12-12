package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;

public class DebugEvents {
    public static void handleMessage(int id, AbstractRecruitEntity recruits) {
        switch (id){
            case 0 -> {recruits.addXp(1); recruits.checkLevel();}
            case 1 -> {recruits.addXp(RecruitsServerConfig.RecruitsMaxXpForLevelUp.get()); recruits.checkLevel();}
            case 2 -> {recruits.setCost(recruits.getCost() + 1);}
            case 3 -> {recruits.setCost(recruits.getCost() - 1);}

            case 4 -> {recruits.setHunger(recruits.getHunger() + 1);}
            case 5 -> {recruits.setHunger(recruits.getHunger() - 1);}

            case 6 -> {recruits.setMoral(recruits.getMoral() + 1);}
            case 7 -> {recruits.setMoral(recruits.getMoral() - 1);}

            case 8 -> {recruits.setHealth(recruits.getHealth() + 1);}
            case 9 -> {recruits.setHealth(recruits.getHealth() - 1);}
        }
    }
}
