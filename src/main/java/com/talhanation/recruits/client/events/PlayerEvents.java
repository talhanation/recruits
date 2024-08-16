package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import com.talhanation.recruits.network.MessageFormationFollowMovement;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PlayerEvents {
    private Vec3 oldPos;
    public static boolean followFormation;
    public static List<RecruitsGroup> activeGroups;
    @SubscribeEvent
    public void onClientPlayerTick(TickEvent.PlayerTickEvent event){

        if(followFormation && activeGroups != null && !activeGroups.isEmpty() && event.player != null && event.player.tickCount % 20 == 0 && (oldPos == null || oldPos != null && event.player.position().distanceToSqr(oldPos) > 150)){
            oldPos = event.player.position();

            for(RecruitsGroup group: activeGroups){
                if(!group.isDisabled()){
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageFormationFollowMovement(event.player.getUUID(), group.getId(), CommandScreen.formation.getIndex()));
                }
            }
        }
    }
}
