package com.talhanation.recruits.client.events;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageWriteSpawnEgg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


@OnlyIn(Dist.CLIENT)

public class KeyEvents {
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null)
            return;

        if (Main.R_KEY.isDown()) {
            CommandEvents.openCommandScreen(clientPlayerEntity);
        }

        if (Main.U_KEY.isDown()) {
            TeamEvents.openTeamMainScreen(clientPlayerEntity);
        }
    }

    @SubscribeEvent
    public void onPlayerPick(InputEvent.ClickInputEvent event){
        if(event.isPickBlock()){
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer clientPlayerEntity = minecraft.player;
            if (clientPlayerEntity == null || !clientPlayerEntity.isCreative())
                return;
            

            Entity target = ClientEvent.getEntityByLooking();
            if(target instanceof AbstractRecruitEntity recruitEntity){
                Main.SIMPLE_CHANNEL.sendToServer(new MessageWriteSpawnEgg(recruitEntity.getUUID()));
                event.setCanceled(true);
            }
        }
    }

}
