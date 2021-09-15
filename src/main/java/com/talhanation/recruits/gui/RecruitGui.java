package com.talhanation.recruits.gui;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.awt.*;


public class RecruitGui extends ScreenBase<ContainerRecruit> {

    private AbstractRecruitEntity recruit;
    private PlayerInventory playerInventory;

    private static final ResourceLocation GUI_TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_recruit.png");
    private static final int TITLE_COLOR = Color.WHITE.getRGB();
    private static final int FONT_COLOR = Color.DARK_GRAY.getRGB();

    protected Button buttonListen;

    public RecruitGui(AbstractRecruitEntity recruit, PlayerInventory playerInventory, ITextComponent title) {
        super(GUI_TEXTURE,  , playerInventory, title);
        this.recruit = recruit;
        this.playerInventory = playerInventory;

        imageWidth = 176;
        imageHeight = 217;
    }

}
