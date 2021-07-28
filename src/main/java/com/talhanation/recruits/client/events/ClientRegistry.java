package com.talhanation.recruits.client.events;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientRegistry {

    public static KeyBinding registerKeyBinding(String name, String category, int keyCode) {
        KeyBinding keyBinding = new KeyBinding(name, keyCode, category);
        net.minecraftforge.fml.client.registry.ClientRegistry.registerKeyBinding(keyBinding);
        return keyBinding;
    }

}