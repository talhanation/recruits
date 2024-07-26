package com.talhanation.recruits.client.events;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.network.MessageForamtionFollowMovement;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
@OnlyIn(Dist.CLIENT)
public class PlayerEvents {
    private Vec3 oldPos;
    public static boolean followFormation;
    @SubscribeEvent
    public void onClientPlayerTick(TickEvent.PlayerTickEvent event){

        if(followFormation && event.player != null && event.player.tickCount % 20 == 0 && (oldPos == null || oldPos != null && event.player.position().distanceToSqr(oldPos) > 150)){
            oldPos = event.player.position();
            Main.SIMPLE_CHANNEL.sendToServer(new MessageForamtionFollowMovement(event.player.getUUID(), CommandScreen.group, CommandScreen.formation));
        }
    }
}
