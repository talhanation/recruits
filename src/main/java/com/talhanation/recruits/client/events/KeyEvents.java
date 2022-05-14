package com.talhanation.recruits.client.events;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
    /*
        if(Main.C_KEY.isDown()){
            Main.SIMPLE_CHANNEL.sendToServer(new MessageMove(clientPlayerEntity.getUUID(), 0));
            //clientPlayerEntity.sendMessage(new StringTextComponent("Everyone! Move!"), clientPlayerEntity.getUUID());
            //clientPlayerEntity.sendMessage(new StringTextComponent("Mount!"), clientPlayerEntity.getUUID());
        }


     */

         /*
        if (Main.X_KEY.isDown()) {

            X_state++;
            if (X_state > 3) X_state = 0;
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(clientPlayerEntity.getUUID(), X_state, group));
            CommandEvents.sendAggroCommandInChat(X_state, clientPlayerEntity);
        }


        if(Main.V_KEY.isDown()){
            group++;
            if (group > 4) group = 0;
            //CommandEvents.sendGroupInChat(group, clientPlayerEntity);
        }
        */

    }
}
