package com.talhanation.recruits;

import com.talhanation.recruits.config.RecruitsServerConfig;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class DamageEvent {


    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        if (!event.isCanceled()) {
            Player player = event.getEntity();
            if (player.getCommandSenderWorld().isClientSide()) {
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
    public void onKnockback(LivingKnockBackEvent event) {
        if (!event.isCanceled()) {
            LivingEntity entity = event.getEntity();
            if (entity.swinging) {
                event.setCanceled(true);
                entity.swinging = false;
            }
        }
    }

    @SubscribeEvent
    public void onEntityHurt(LivingIncomingDamageEvent event) {
        if (!event.isCanceled()) {

            LivingEntity target = event.getEntity();

            if (target.getCommandSenderWorld().isClientSide()) {
                return;
            }

            DamageSource source = event.getSource();
            Entity sourceEntity = event.getEntity();

            if(!RecruitsServerConfig.NoDamageImmunity.get()) return;

            if (target.getCommandSenderWorld().isClientSide()) {
                return;
            }

            //Velocity Damage
            if(source != null && sourceEntity != null){

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