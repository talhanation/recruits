package com.talhanation.recruits.client.gui.group;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.widget.ExtendedButton;

@OnlyIn(Dist.CLIENT)
public class RecruitsCommandButton extends ExtendedButton {

    public RecruitsCommandButton(int xPos, int yPos, Component displayString, OnPress handler) {
        super(xPos - 40, yPos - 10, 80, 20, displayString, handler);
    }
}
