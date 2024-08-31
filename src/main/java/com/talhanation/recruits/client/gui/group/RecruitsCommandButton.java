package com.talhanation.recruits.client.gui.group;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class RecruitsCommandButton extends Button {

    public RecruitsCommandButton(int xPos, int yPos, Component displayString, OnPress handler, OnTooltip tooltip) {
        super(xPos - 40, yPos - 10, 80, 20, displayString, handler, tooltip);
    }
}
