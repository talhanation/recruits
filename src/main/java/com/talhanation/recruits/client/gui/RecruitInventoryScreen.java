package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class RecruitInventoryScreen extends ScreenBase<RecruitInventoryContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/recruit_gui.png");

    private static final ITextComponent TEXT_HEALTH = new TranslationTextComponent("gui.recruits.health");
    private static final ITextComponent TEXT_LEVEL = new TranslationTextComponent("gui.recruits.level");
    private static final ITextComponent TEXT_GROUP = new TranslationTextComponent("gui.recruits.group");//maybe something to interact with later
    private static final ITextComponent TEXT_KILLS = new TranslationTextComponent("gui.recruits.kills");

    private static final int fontColor = 4210752;

    private final AbstractRecruitEntity recruit;
    private final PlayerInventory playerInventory;

    public RecruitInventoryScreen(RecruitInventoryContainer recruitContainer, PlayerInventory playerInventory, ITextComponent title) {
        super(RESOURCE_LOCATION, recruitContainer, playerInventory, title);
        this.recruit = recruitContainer.getRecruit();
        this.playerInventory = playerInventory;

        imageWidth = 176;
        imageHeight = 202;
    }
    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int health = MathHelper.ceil(recruit.getHealth());
        int k = 79;
        int l = 19;
        //Titles
        font.draw(matrixStack, recruit.getDisplayName().getVisualOrderText(), 8, 5, fontColor);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor);
        //Info
        font.draw(matrixStack, "Hp:    "+ health, k, l, fontColor);
        font.draw(matrixStack, "Lvl:   "+ recruit.getXpLevel(), k , l + 10, fontColor);
        font.draw(matrixStack, "Exp:   "+ recruit.getXp(), k, l + 20, fontColor);
        font.draw(matrixStack, "Kills: "+ recruit.getKills(), k, l + 30, fontColor);

        // command

        String follow = "";
        switch (recruit.getFollowState()){
            default:
            case 0:
                follow = "Wandering";
                break;
            case 1:
                follow = "Following";
                break;
            case 2:
                follow = "Holding Position";
                break;

        }
        font.draw(matrixStack, follow, k, l + 50, fontColor);


        String aggro = "";
        switch (recruit.getState()){
            default:
            case 2:
                aggro = "Neutral";
                break;
            case 0:
                aggro = "Aggressive";
                break;
            case 1:
                aggro = "Raid";
                break;
        }
        font.draw(matrixStack, aggro, k, l + 65, fontColor);

        font.draw(matrixStack, "Group: " + recruit.getGroup(), k, l + 80, fontColor);
    }

    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);

        //drawHealth(matrixStack, health);

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        //this.minecraft.getTextureManager().bind(RESOURCE_LOCATION);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        InventoryScreen.renderEntityInInventory(i + 50, j + 82, 30, (float)(i + 50) - mouseX, (float)(j + 75 - 50) - mouseY, this.recruit);
    }



}
