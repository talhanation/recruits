package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class DamageEvent {


    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (!event.isCanceled()) {
            Player player = event.getEntity();
            if (player.getLevel().isClientSide()) {
                return;
            }
            float str = player.getAttackStrengthScale(0);
            if (str <= 0.1) {
                event.setCanceled(true);
                return;
            }
            if (str <= 0.75) {
                Entity target = event.getTarget();
                if (target != null && target instanceof LivingEntity) {
                    ((LivingEntity)target).swinging = true;
                }
            }
        }
    }


    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (!event.isCanceled()) {
            LivingEntity entity = event.getEntity();
            if (entity.swinging) {
                event.setCanceled(true);
                entity.swinging = false;
            }
        }
    }

    @SubscribeEvent
    public void onEntityHurt(LivingHurtEvent event) {
        if (!event.isCanceled()) {

            LivingEntity target = event.getEntity();

            if (target.getLevel().isClientSide()) {
                return;
            }
            if(!RecruitsServerConfig.NoDamageImmunity.get()) return;

            DamageSource source = event.getSource();

            if (target.getLevel().isClientSide()) {
                return;
            }
            //Velocity Damage
            if(source != null && source.getEntity() != null){

            }

            //NO Damage Immunity
            if(!RecruitsServerConfig.NoDamageImmunity.get()) return;

            if (source != null && RecruitsServerConfig.AcceptedDamagesourceImmunity.get().contains(source.getMsgId())) {
                return;
            }
            target.invulnerableTime = 0;
        }
    }

    @SubscribeEvent
    public void onEntityHurtByPlayer(AttackEntityEvent event) {
        if (!event.isCanceled()) {
            Player player = event.getEntity();
            Entity target = event.getTarget();

            if(target.getFirstPassenger() instanceof LivingEntity passenger){
                if (!RecruitEvents.canHarmTeam(player, passenger)) {
                    event.setCanceled(true);
                }
            }
        }
    }
}