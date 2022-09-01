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

        if (Main.C_KEY.isDown()) {
            CommandEvents.openTeamCreationGUI(clientPlayerEntity);
        }


    }
}
