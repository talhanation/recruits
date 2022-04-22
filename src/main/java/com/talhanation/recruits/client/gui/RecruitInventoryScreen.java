package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.RecruitInventoryContainer;
import com.talhanation.recruits.network.*;
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
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/recruit_gui.png" );

    private static final ITextComponent TEXT_HEALTH = new TranslationTextComponent("gui.recruits.inv.health");
    private static final ITextComponent TEXT_LEVEL = new TranslationTextComponent("gui.recruits.inv.level");
    private static final ITextComponent TEXT_KILLS = new TranslationTextComponent("gui.recruits.inv.kills");

    private static final ITextComponent TEXT_DISBAND = new TranslationTextComponent("gui.recruits.inv.text.disband");

    private static final ITextComponent TOOLTIP_DISBAND = new TranslationTextComponent("gui.recruits.inv.tooltip.disband");

    private static final ITextComponent TOOLTIP_FOLLOW = new TranslationTextComponent("gui.recruits.inv.tooltip.follow");
    private static final ITextComponent TOOLTIP_WANDER = new TranslationTextComponent("gui.recruits.inv.tooltip.wander");
    private static final ITextComponent TOOLTIP_HOLD_MY_POS = new TranslationTextComponent("gui.recruits.inv.tooltip.holdMyPos");
    private static final ITextComponent TOOLTIP_HOLD_POS = new TranslationTextComponent("gui.recruits.inv.tooltip.holdPos");
    private static final ITextComponent TOOLTIP_BACK_TO_POS = new TranslationTextComponent("gui.recruits.inv.tooltip.backToPos");

    private static final ITextComponent TEXT_FOLLOW = new TranslationTextComponent("gui.recruits.inv.text.follow");
    private static final ITextComponent TEXT_WANDER = new TranslationTextComponent("gui.recruits.inv.text.wander");
    private static final ITextComponent TEXT_HOLD_MY_POS = new TranslationTextComponent("gui.recruits.inv.text.holdMyPos");
    private static final ITextComponent TEXT_HOLD_POS = new TranslationTextComponent("gui.recruits.inv.text.holdPos");
    private static final ITextComponent TEXT_BACK_TO_POS = new TranslationTextComponent("gui.recruits.inv.text.backToPos");


    private static final ITextComponent TEXT_PASSIVE = new TranslationTextComponent("gui.recruits.inv.text.passive");
    private static final ITextComponent TEXT_NEUTRAL = new TranslationTextComponent("gui.recruits.inv.text.neutral");
    private static final ITextComponent TEXT_AGGRESSIVE = new TranslationTextComponent("gui.recruits.inv.text.aggressive");
    private static final ITextComponent TEXT_RAID = new TranslationTextComponent("gui.recruits.inv.text.raid");
    private static final ITextComponent TEXT_CLEAR_TARGET = new TranslationTextComponent("gui.recruits.command.text.clearTargets");

    private static final ITextComponent TOOLTIP_PASSIVE = new TranslationTextComponent("gui.recruits.inv.tooltip.passive");
    private static final ITextComponent TOOLTIP_NEUTRAL = new TranslationTextComponent("gui.recruits.inv.tooltip.neutral");
    private static final ITextComponent TOOLTIP_AGGRESSIVE = new TranslationTextComponent("gui.recruits.inv.tooltip.aggressive");
    private static final ITextComponent TOOLTIP_RAID = new TranslationTextComponent("gui.recruits.inv.tooltip.raid");

    private static final int fontColor = 4210752;

    private final AbstractRecruitEntity recruit;
    private final PlayerInventory playerInventory;

    private int group;
    private int follow;
    private int aggro;

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

        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos + 10;

        int topPosGab = 5;


        //PASSIVE
        addButton(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_PASSIVE, button -> {
            this.aggro = recruit.getState();
            if (this.aggro != 3) {
                this.aggro = 3;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
            }
        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_PASSIVE, c, d);
        }));

        //NEUTRAL
        addButton(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_NEUTRAL, button -> {
            this.aggro = recruit.getState();
            if (this.aggro != 0) {
                this.aggro = 0;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
            }
        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_NEUTRAL, c, d);
        }));

        //AGGRESSIVE
        addButton(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_AGGRESSIVE, button -> {
            this.aggro = recruit.getState();
            if (this.aggro != 1) {
                this.aggro = 1;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
            }

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_AGGRESSIVE, c, d);
        }));

        //RAID
        addButton(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_RAID, button -> {
            this.aggro = recruit.getState();
            if (this.aggro != 2) {
                this.aggro = 2;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
            }

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_RAID, c, d);
        }));

        //CLEAR TARGET
        addButton(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_CLEAR_TARGET, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageClearTargetGui(recruit.getUUID()));
        }
        ));

        //WANDER
        addButton(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_WANDER, button -> {
            this.follow = recruit.getFollowState();
            if (this.follow != 0) {
                this.follow = 0;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
            }

            },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_WANDER, c, d);
        }));


        //FOLLOW
        addButton(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_FOLLOW, button -> {
            this.follow = recruit.getFollowState();
            if (this.follow != 1) {
                this.follow = 1;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
            }

        },   (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_FOLLOW, c, d);
        }));


        //HOLD POS
        addButton(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_HOLD_POS, button -> {
            this.follow = recruit.getFollowState();
            if (this.follow != 2) {
                this.follow = 2;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
            }


        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_HOLD_POS, c, d);
        }));


        //BACK TO POS
        addButton(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_BACK_TO_POS, button -> {
            this.follow = recruit.getFollowState();
            if (this.follow != 3) {
                this.follow = 3;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
            }


        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_BACK_TO_POS, c, d);
        }));


        //HOLD MY POS
        addButton(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_HOLD_MY_POS, button -> {
            this.follow = recruit.getFollowState();
            if (this.follow != 4) {
                this.follow = 4;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
            }

        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_HOLD_MY_POS, c, d);
        }));


        //LISTEN
        addButton(new Button(leftPos + 77, topPos + 113, 8, 12, new StringTextComponent("<"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageListen(!recruit.getListen(), recruit.getUUID()));
        }));

        addButton(new Button(leftPos + 77 + 85, topPos + 113, 8, 12, new StringTextComponent(">"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageListen(!recruit.getListen(), recruit.getUUID()));
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


        //DISBAND
        addButton(new Button(leftPos + 77 + 55, topPos + 4, 40, 12, TEXT_DISBAND, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDisband(recruit.getUUID()));
            this.onClose();
        },  (a, b, c, d) -> {
            this.renderTooltip(b, TOOLTIP_DISBAND, c, d);
        }));
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int health = MathHelper.ceil(recruit.getHealth());
        this.follow = recruit.getFollowState();
        this.aggro = recruit.getState();

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
        //font.draw(matrixStack, "A.Dmg:", k, l + 40, fontColor);
        //font.draw(matrixStack, ""+ recruit.getAttackDamage(), k + 35, l + 40, fontColor);
        /*
        font.draw(matrixStack, "Moral:", k, l + 30, fontColor);
        font.draw(matrixStack, ""+ recruit.getKills(), k + 25, l + 30, fontColor);
        */

        // command
        String follow;
        switch (this.follow){
            default:
            case 0:
                follow = "Wandering";
                break;
            case 1:
                follow = "Following";
                break;

            case 2:
            case 3:
            case 4:
                follow = "Holding Pos.";
                break;
        }
        font.draw(matrixStack, follow, k + 15, l + 58 + 0, fontColor);

        String aggro;
        switch (this.aggro){
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
            case 3:
                aggro = "Passive";
                break;
        }
        font.draw(matrixStack, aggro, k + 15, l + 56 + 15, fontColor);

        font.draw(matrixStack, CommandScreen.handleGroupText(recruit.getGroup()), k + 15, l + 56 + 28, fontColor);

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
