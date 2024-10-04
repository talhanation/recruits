package com.talhanation.recruits.init;

import com.talhanation.recruits.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =  DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Main.MOD_ID);

    public static final RegistryObject<SoundEvent> MESSENGER_HORN = SOUNDS.register("messenger_horn",
            () -> new SoundEvent(new ResourceLocation(Main.MOD_ID,"messenger_horn")));
}
