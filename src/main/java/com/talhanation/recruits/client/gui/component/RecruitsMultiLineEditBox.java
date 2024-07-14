package com.talhanation.recruits.client.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecruitsMultiLineEditBox extends MultiLineEditBox {

    public boolean enableEditing;
    public RecruitsMultiLineEditBox(Font p_239008_, int p_239009_, int p_239010_, int p_239011_, int p_239012_, Component p_239013_, Component p_239014_) {
        super(p_239008_, p_239009_, p_239010_, p_239011_, p_239012_, p_239013_, p_239014_);
        this.enableEditing = false;

    }

    public boolean keyPressed(int p_239433_, int p_239434_, int p_239435_) {
        if(enableEditing) return super.keyPressed(p_239433_, p_239434_, p_239435_);
        else return false;
    }

    public boolean charTyped(char p_239387_, int p_239388_) {
        if(enableEditing) return super.charTyped(p_239387_, p_239388_);
        else return false;
    }

    public void setEnableEditing(boolean enableEditing) {
        this.enableEditing = enableEditing;
    }

    protected boolean scrollbarVisible() {
        return false;
    }
}
