package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class DebugEvents {
    public static void handleMessage(int id, AbstractRecruitEntity recruits, ServerPlayer player) {
        switch (id){
            default -> {}
            case 0 -> {recruits.addXp(1); recruits.checkLevel();}
            case 1 -> {recruits.addXp(RecruitsServerConfig.RecruitsMaxXpForLevelUp.get()); recruits.checkLevel();}
            case 2 -> {recruits.setCost(recruits.getCost() + 1);}
            case 3 -> {recruits.setCost(recruits.getCost() - 1);}

            case 4 -> {recruits.setHunger(recruits.getHunger() + 1);}
            case 5 -> {recruits.setHunger(recruits.getHunger() - 1);}

            case 6 -> {recruits.setMoral(recruits.getMorale() + 1);}
            case 7 -> {recruits.setMoral(recruits.getMorale() - 1);}

            case 8 -> {recruits.setHealth(recruits.getHealth() + 1);}
            case 9 -> {recruits.setHealth(recruits.getHealth() - 1);}

            case 10 -> {
                int current =  recruits.getVariant();
                int next = current + 1;
                if(next > 19) next = 19;
                recruits.setVariant((byte) next);
            }
            case 11 -> {
                int current =  recruits.getVariant();
                int next = current - 1;
                if(next < 0) next = 0;
                recruits.setVariant((byte) next);
            }

            case 12 -> {
                int current =  recruits.getBiome();
                int next = current + 1;
                if(next > 6) next = 6;
                recruits.setBiome((byte) next);
            }

            case 13 -> {
                int current =  recruits.getBiome();
                int next = current - 1;
                if(next < 0) next = 0;
                recruits.setBiome((byte) next);
            }

            case 14 -> {recruits.heal(1000);}
            case 15 -> {recruits.kill();}

            case 16 -> {recruits.clearUpkeepEntity(); recruits.clearUpkeepPos();}
            case 17 -> {recruits.clearHoldPos();}

            case 18 -> {recruits.shouldProtect(false, null);}
            case 19 -> {recruits.shouldMount(false, null);}

            case 20 -> {
                if(recruits instanceof ICompanion companion) companion.openSpecialGUI(player);
                else RecruitEvents.openPromoteScreen(player, recruits);
            }
            case 21 -> {recruits.addXp(10); recruits.checkLevel();}
            case 22 -> {
                for(int i = 0; i < 5; i++){
                    recruits.addXp(RecruitsServerConfig.RecruitsMaxXpForLevelUp.get()); recruits.checkLevel();
                }
            }
            case 23 -> {TeamEvents.removeRecruitFromTeam(recruits, recruits.getTeam(), (ServerLevel) recruits.getCommandSenderWorld());}

            case 24 -> {
                int current =  recruits.getColor();
                int next = current + 1;
                if(next > 24) next = 24;
                recruits.setColor((byte) next);
            }

            case 25 -> {
                int current =  recruits.getColor();
                int next = current - 1;
                if(next < 0) next = 0;
                recruits.setColor((byte) next);
            }

            case 26 -> {
                recruits.disband(recruits.getOwner(), true, false);
            }
        }
    }
}
