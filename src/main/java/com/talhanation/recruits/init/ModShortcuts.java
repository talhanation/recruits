package com.talhanation.recruits.init;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class ModShortcuts {
    public static KeyMapping COMMAND_SCREEN_KEY;
    public static KeyMapping TEAM_SCREEN_KEY;

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerBindings(RegisterKeyMappingsEvent event) {
        COMMAND_SCREEN_KEY = new KeyMapping("key.recruits.command_screen_key", GLFW.GLFW_KEY_R, "category.recruits");
        TEAM_SCREEN_KEY =  new KeyMapping("key.recruits.team_screen_key", GLFW.GLFW_KEY_U, "category.recruits");


        event.register(COMMAND_SCREEN_KEY);
        event.register(TEAM_SCREEN_KEY);
    }
}
