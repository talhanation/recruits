package com.talhanation.recruits.util;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.common.ForgeMod;

import java.util.Random;

public abstract class AttackUtil {
    public static void checkAndPerformAttack(double distanceSqrToTarget, double reach, AbstractRecruitEntity recruit, LivingEntity target){
        if(distanceSqrToTarget <= reach){
            performAttack(recruit, target);
        }
    }

    public static void performAttack(AbstractRecruitEntity recruit, LivingEntity target) {
        if(recruit.attackCooldown == 0 && !recruit.swinging && recruit.getLookControl().isLookingAtTarget()){
            if(canPerformHorseAttack(recruit, target)){
                if(target.getVehicle() != null) recruit.doHurtTarget(target.getVehicle());
            }
            else recruit.doHurtTarget(target);

            recruit.swing(InteractionHand.MAIN_HAND);
            recruit.attackCooldown = getAttackCooldown(recruit);
        }
    }

    public static boolean canPerformHorseAttack(AbstractRecruitEntity recruit, LivingEntity target) {
        Random random = new Random();
        if(target.getVehicle() instanceof Animal){
            int level = recruit.getXpLevel();
            int chance = Math.min(level*2, 100);

            return random.nextInt(0, 100) <= chance;
        }
        else
            return false;
    }

    public static int getAttackCooldown(AbstractRecruitEntity recruit) {
        double attackSpeed = recruit.getAttributeValue(Attributes.ATTACK_SPEED);

        int base = (int) Math.round(20/attackSpeed);

        return base + 7;
    }

    /*
    Cooldown Infos MC-Wiki
    Swords: 0.6s
    Stone and Wood axe: 1.25s
    Gold/Dia/Neatherite: 1s
    Iron: 1.1s

    1s = 20ticks

    cooldown should be + 5 ticks for gameplay
     */
    public static double getAttackReachSqr(LivingEntity living) {
        float base = 5F;
        if(living.getAttribute(ForgeMod.ENTITY_REACH.get()) != null){
            double attackReach = living.getAttributeValue(ForgeMod.ENTITY_REACH.get());
            //Vanilla reach is 10
            //Epic fight mod:
            // reach +1.0 == 18
            // reach +2.0 == 45
            // reach +3.0 == 60
            if(attackReach > 0){
                return 2*base * attackReach;
            }
        }
        return base;
    }
}
