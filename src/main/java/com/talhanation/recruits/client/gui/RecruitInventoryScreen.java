package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.RecruitInventoryMenu;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecruitInventoryScreen extends ScreenBase<RecruitInventoryMenu> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/recruit_gui.png" );

    private static final MutableComponent TEXT_HEALTH = Component.translatable("gui.recruits.inv.health");
    private static final MutableComponent TEXT_LEVEL = Component.translatable("gui.recruits.inv.level");
    private static final MutableComponent TEXT_KILLS = Component.translatable("gui.recruits.inv.kills");

    private static final MutableComponent TEXT_DISBAND = Component.translatable("gui.recruits.inv.text.disband");
    private static final MutableComponent TEXT_DISMOUNT = Component.translatable("gui.recruits.inv.text.dismount");

    private static final MutableComponent TOOLTIP_DISBAND = Component.translatable("gui.recruits.inv.tooltip.disband");
    private static final MutableComponent TOOLTIP_DISMOUNT = Component.translatable("gui.recruits.inv.tooltip.dismount");
    private static final MutableComponent TOOLTIP_FOLLOW = Component.translatable("gui.recruits.inv.tooltip.follow");
    private static final MutableComponent TOOLTIP_WANDER = Component.translatable("gui.recruits.inv.tooltip.wander");
    private static final MutableComponent TOOLTIP_HOLD_MY_POS = Component.translatable("gui.recruits.inv.tooltip.holdMyPos");
    private static final MutableComponent TOOLTIP_HOLD_POS = Component.translatable("gui.recruits.inv.tooltip.holdPos");
    private static final MutableComponent TOOLTIP_BACK_TO_POS = Component.translatable("gui.recruits.inv.tooltip.backToPos");
    private static final MutableComponent TOOLTIP_CLEAR_TARGET = Component.translatable("gui.recruits.inv.tooltip.clearTargets");


    private static final MutableComponent TEXT_FOLLOW = Component.translatable("gui.recruits.inv.text.follow");
    private static final MutableComponent TEXT_WANDER = Component.translatable("gui.recruits.inv.text.wander");
    private static final MutableComponent TEXT_HOLD_MY_POS = Component.translatable("gui.recruits.inv.text.holdMyPos");
    private static final MutableComponent TEXT_HOLD_POS = Component.translatable("gui.recruits.inv.text.holdPos");
    private static final MutableComponent TEXT_BACK_TO_POS = Component.translatable("gui.recruits.inv.text.backToPos");

    private static final MutableComponent TEXT_PASSIVE = Component.translatable("gui.recruits.inv.text.passive");
    private static final MutableComponent TEXT_NEUTRAL = Component.translatable("gui.recruits.inv.text.neutral");
    private static final MutableComponent TEXT_AGGRESSIVE = Component.translatable("gui.recruits.inv.text.aggressive");
    private static final MutableComponent TEXT_RAID = Component.translatable("gui.recruits.inv.text.raid");
    private static final MutableComponent TEXT_CLEAR_TARGET = Component.translatable("gui.recruits.inv.text.clearTargets");

    private static final MutableComponent TOOLTIP_PASSIVE = Component.translatable("gui.recruits.inv.tooltip.passive");
    private static final MutableComponent TOOLTIP_NEUTRAL = Component.translatable("gui.recruits.inv.tooltip.neutral");
    private static final MutableComponent TOOLTIP_AGGRESSIVE = Component.translatable("gui.recruits.inv.tooltip.aggressive");
    private static final MutableComponent TOOLTIP_RAID = Component.translatable("gui.recruits.inv.tooltip.raid");

    private static final int fontColor = 4210752;

    private final AbstractRecruitEntity recruit;
    private final Inventory playerInventory;

    private int group;
    private int follow;
    private int aggro;

    public RecruitInventoryScreen(RecruitInventoryMenu recruitContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, recruitContainer, playerInventory, Component.literal(""));
        this.recruit = recruitContainer.getRecruit();
        this.playerInventory = playerInventory;
        imageWidth = 176;
        imageHeight = 223;
    }


    @Override
    protected void init() {
        super.init();

        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos + 10;

        int topPosGab = 5;


        //PASSIVE
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_PASSIVE, button -> {
            this.aggro = recruit.getState();
            if (this.aggro != 3) {
                this.aggro = 3;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
            }
        }));

        //NEUTRAL
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_NEUTRAL, button -> {
            this.aggro = recruit.getState();
            if (this.aggro != 0) {
                this.aggro = 0;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
            }
        }));

        //AGGRESSIVE
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_AGGRESSIVE, button -> {
            this.aggro = recruit.getState();
            if (this.aggro != 1) {
                this.aggro = 1;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
            }
        }));

        //RAID
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_RAID, button -> {
            this.aggro = recruit.getState();
            if (this.aggro != 2) {
                this.aggro = 2;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
            }
        }));

        //CLEAR TARGET
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_CLEAR_TARGET, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageClearTargetGui(playerInventory.player.getUUID(), recruit.getUUID()));
        }));

        //WANDER
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_WANDER, button -> {
            this.follow = recruit.getFollowState();
            if (this.follow != 0) {
                this.follow = 0;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
            }
        }));


        //FOLLOW
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_FOLLOW, button -> {
            this.follow = recruit.getFollowState();
            if (this.follow != 1) {
                this.follow = 1;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
            }
        }));


        //HOLD POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_HOLD_POS, button -> {
            this.follow = recruit.getFollowState();
            if (this.follow != 2) {
                this.follow = 2;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
            }
        }));


        //BACK TO POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_BACK_TO_POS, button -> {
            this.follow = recruit.getFollowState();
            if (this.follow != 3) {
                this.follow = 3;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
            }
        }));


        //HOLD MY POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_HOLD_MY_POS, button -> {
            this.follow = recruit.getFollowState();
            if (this.follow != 4) {
                this.follow = 4;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
            }
        }));

        //LISTEN
        addRenderableWidget(new Button(leftPos + 77, topPos + 113, 8, 12, Component.literal("<"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageListen(!recruit.getListen(), recruit.getUUID()));
        }));

        addRenderableWidget(new Button(leftPos + 77 + 85, topPos + 113, 8, 12, Component.literal(">"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageListen(!recruit.getListen(), recruit.getUUID()));
        }));


        //GROUP
        addRenderableWidget(new Button(leftPos + 77, topPos + 100, 8, 12, Component.literal("<"), button -> {
            this.group = recruit.getGroup();
            if (this.group != 0) {
                this.group--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageGroup(this.group, recruit.getUUID()));
            }
        }));

        addRenderableWidget(new Button(leftPos + 77 + 85, topPos + 100, 8, 12, Component.literal(">"), button -> {
            this.group = recruit.getGroup();
            if (this.group != 9) {
                this.group++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageGroup(this.group, recruit.getUUID()));
            }
        }));


        //DISBAND
        addRenderableWidget(new Button(leftPos + 77 + 55, topPos + 4, 40, 12, TEXT_DISBAND, button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDisband(recruit.getUUID()));
            this.onClose();


        }
        ));
    }


    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int health = Mth.ceil(recruit.getHealth());
        int moral = Mth.ceil(recruit.getMoral());
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
        font.draw(matrixStack, "Moral:", k, l + 40, fontColor);
        font.draw(matrixStack, ""+ moral, k + 30, l + 40, fontColor);
        /*
        font.draw(matrixStack, "Moral:", k, l + 30, fontColor);
        font.draw(matrixStack, ""+ recruit.getKills(), k + 25, l + 30, fontColor);
        */

        // command
        String follow = switch (this.follow) {
            case 0 -> "Wandering";
            case 1 -> "Following";
            case 2, 3, 4 -> "Holding Pos.";
            case 5 -> "Escorting";
            default -> throw new IllegalStateException("Unexpected value: " + this.follow);
        };
        font.draw(matrixStack, follow, k + 15, l + 58 + 0, fontColor);

        String aggro = switch (this.aggro) {
            case 0 -> "Neutral";
            case 1 -> "Aggressive";
            case 2 -> "Raid";
            case 3 -> "Passive";
            default -> throw new IllegalStateException("Unexpected value: " + this.aggro);
        };
        font.draw(matrixStack, aggro, k + 15, l + 56 + 15, fontColor);

        font.draw(matrixStack, CommandScreen.handleGroupText(recruit.getGroup()), k + 15, l + 56 + 28, fontColor);

        String listen;
        if (recruit.getListen()) listen = "Listening";
        else listen = "Ignoring";
        font.draw(matrixStack, listen, k + 15, l + 56 + 41, fontColor);
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);

        RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        InventoryScreen.renderEntityInInventory(i + 50, j + 82, 30, (float)(i + 50) - mouseX, (float)(j + 75 - 50) - mouseY, this.recruit);
    }
}
