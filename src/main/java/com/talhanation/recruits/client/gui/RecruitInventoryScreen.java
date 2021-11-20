package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.MessageAttack;
import com.talhanation.recruits.network.MessageFollow;
import com.talhanation.recruits.network.MessageGroup;
import com.talhanation.recruits.network.MessageListen;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class RecruitInventoryScreen extends ScreenBase<RecruitInventoryContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/recruit_gui.png");

    private static final ITextComponent TEXT_HEALTH = new TranslationTextComponent("gui.recruits.health");
    private static final ITextComponent TEXT_LEVEL = new TranslationTextComponent("gui.recruits.level");
    private static final ITextComponent TEXT_GROUP = new TranslationTextComponent("gui.recruits.group");
    private static final ITextComponent TEXT_KILLS = new TranslationTextComponent("gui.recruits.kills");

    private static final int fontColor = 4210752;

    private final AbstractRecruitEntity recruit;
    private final PlayerInventory playerInventory;

    private int followState;
    private int aggroState;
    private int group;

    public RecruitInventoryScreen(RecruitInventoryContainer recruitContainer, PlayerInventory playerInventory, ITextComponent title) {
        super(RESOURCE_LOCATION, recruitContainer, playerInventory, title);
        this.recruit = recruitContainer.getRecruit();
        this.playerInventory = playerInventory;

        imageWidth = 176;
        imageHeight = 218;
    }


    @Override
    protected void init() {
        super.init();

        //FOLLOW
        addButton(new Button(leftPos + 77, topPos + 74, 8, 12, new StringTextComponent("<"), button -> {
            if (this.followState != 3){
                this.followState ++;

                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(recruit.getOwnerUUID(), recruit.getUUID(), this.followState, true));
            }
        }));

        addButton(new Button(leftPos + 77 + 85, topPos + 74, 8, 12, new StringTextComponent(">"), button -> {
            if (this.followState != 0) {
                this.followState--;

                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(recruit.getOwnerUUID(), recruit.getUUID(), this.followState, true));
            }
        }));

        //LISTEN
        addButton(new Button(leftPos + 77, topPos + 113, 8, 12, new StringTextComponent("<"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageListen(!recruit.getListen(), recruit.getUUID()));
        }));

        addButton(new Button(leftPos + 77 + 85, topPos + 113, 8, 12, new StringTextComponent(">"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageListen(!recruit.getListen(), recruit.getUUID()));
        }));

        //AGGRO
        addButton(new Button(leftPos + 77, topPos + 87, 8, 12, new StringTextComponent("<"), button -> {
            if (this.aggroState != 3){
                this.aggroState ++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(recruit.getOwnerUUID(), recruit.getUUID(),this.aggroState, true));
            }
        }));

        addButton(new Button(leftPos + + 77 + 85, topPos + 87, 8, 12, new StringTextComponent(">"), button -> {
            if (this.aggroState != 0){
                this.aggroState --;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(recruit.getOwnerUUID(), recruit.getUUID(),this.aggroState, true));
            }
        }));

        //GROUP
        addButton(new Button(leftPos + 77, topPos + 100, 8, 12, new StringTextComponent("<"), button -> {
            this.group = recruit.getGroup();
            if (this.group != 0) {
                this.group--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageGroup(this.group, recruit.getUUID()));
            }
        }));

        addButton(new Button(leftPos + 77 + 85, topPos + 100, 8, 12, new StringTextComponent(">"), button -> {
            this.group = recruit.getGroup();
            if (this.group != 9) {
                this.group++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageGroup(this.group, recruit.getUUID()));
            }
        }));
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int health = MathHelper.ceil(recruit.getHealth());
        int k = 79;//rechst links
        int l = 19;//höhe
        //Titles
        font.draw(matrixStack, recruit.getDisplayName().getVisualOrderText(), 8, 5, fontColor);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor);

        //Info
        font.draw(matrixStack, "Hp:", k, l, fontColor);
        font.draw(matrixStack, "" + health, k + 25, l , fontColor);
        font.draw(matrixStack, "Lvl:", k , l  + 10, fontColor);
        font.draw(matrixStack, "" + recruit.getXpLevel(), k + 25 , l + 10, fontColor);
        font.draw(matrixStack, "Exp:", k, l + 20, fontColor);
        font.draw(matrixStack, "" + recruit.getXp(), k + 25, l + 20, fontColor);
        font.draw(matrixStack, "Kills:", k, l + 30, fontColor);
        font.draw(matrixStack, ""+ recruit.getKills(), k + 25, l + 30, fontColor);

        // command
        String follow;
        switch (recruit.getFollowState()){
            default:
            case 0:
                follow = "Wandering";
                break;
            case 1:
                follow = "Following";
                break;
            case 2:
                follow = "Holding Pos.";
                break;
        }
        font.draw(matrixStack, follow, k + 15, l + 58 + 0, fontColor);

        String aggro;
        switch (recruit.getState()){
            default:
            case 0:
                aggro = "Neutral";
                break;
            case 1:
                aggro = "Aggressive";
                break;
            case 2:
                aggro = "Raid";
                break;
        }
        font.draw(matrixStack, aggro, k + 15, l + 56 + 15, fontColor);

        font.draw(matrixStack, CommandScreen.handleGroupText(this.group), k + 15, l + 56 + 28, fontColor);

        String listen;
        if (recruit.getListen()) listen = "Listening";
        else listen = "Ignoring";
        font.draw(matrixStack, listen, k + 15, l + 56 + 41, fontColor);
    }

    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        InventoryScreen.renderEntityInInventory(i + 50, j + 82, 30, (float)(i + 50) - mouseX, (float)(j + 75 - 50) - mouseY, this.recruit);
    }
}
