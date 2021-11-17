package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.MessageAttack;
import com.talhanation.recruits.network.MessageFollow;
import com.talhanation.recruits.network.MessageListen;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class CommandScreen extends ScreenBase<CommandContainer> {

    private static final int fontColor = 4210752;


    public CommandScreen(CommandContainer commandContainer, PlayerInventory playerInventory, ITextComponent title) {
        super(null, commandContainer, playerInventory, title);

        imageWidth = 176;
        imageHeight = 218;
    }


    @Override
    protected void init() {
        super.init();
        int k = 79;//höhe
        int l = 19;//rechst links


        //FOLLOW
        addButton(new Button(0, 0, 40, 40, new StringTextComponent("Follow me!"), button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(, recruit.getUUID(), this.followState, true));
        }));


    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);

    }

    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }



}
