package com.talhanation.recruits.client.events;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.component.ImageToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class RecruitsToastManager {
    private static final ResourceLocation LETTER_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/letter.png");
    private static final ResourceLocation ALLY_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/ally.png");
    private static final ResourceLocation ENEMY_IMAGE = new ResourceLocation(Main.MOD_ID, "textures/gui/enemy.png");
    public static void setToastForPlayer(Images id, @Nullable Component title, @Nullable Component text){
        Toast toast = null;
        switch (id){
            case ALLY -> {
                toast = new ImageToast(ALLY_IMAGE, title, text);
            }
            case ENEMY -> {
                toast = new ImageToast(ENEMY_IMAGE, title, text);
            }
            default -> {
                toast = new ImageToast(LETTER_IMAGE, title, text);
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getToasts().addToast(toast);
        //minecraft.getSoundManager().play
    }


    public static enum Images{
        LETTER,
        ALLY,
        ENEMY
    }
}
