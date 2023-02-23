package com.talhanation.recruits.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModShortcuts {
    public static KeyMapping COMMAND_SCREEN_KEY = new KeyMapping(
            Component.translatable("key.recruits.command_screen_key").getString(),
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            "Recruits"
    );

    public static KeyMapping TEAM_SCREEN_KEY = new KeyMapping(
            Component.translatable("key.recruits.team_screen_key").getString(),
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_U,
            "Recruits"
    );

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(COMMAND_SCREEN_KEY);
        event.register(TEAM_SCREEN_KEY);
    }
}
