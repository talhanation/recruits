package com.talhanation.recruits.client.events;

import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.init.ModShortcuts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)

public class KeyEvents {
    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer clientPlayerEntity = minecraft.player;
        if (clientPlayerEntity == null)
            return;

        if (ModShortcuts.COMMAND_SCREEN_KEY.isDown()) {
            CommandEvents.openCommandScreen(clientPlayerEntity);
        }

        if (ModShortcuts.TEAM_SCREEN_KEY.isDown()) {
            TeamEvents.openTeamMainScreen(clientPlayerEntity);
        }
    }
}
