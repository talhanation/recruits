package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.TeamEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.entities.ICompanion;
import com.talhanation.recruits.inventory.RecruitInventoryMenu;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.w3c.dom.Text;

@OnlyIn(Dist.CLIENT)
public class RecruitInventoryScreen extends ScreenBase<RecruitInventoryMenu> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/recruit_gui.png" );
	
    private static final MutableComponent TEXT_HEALTH = new TranslatableComponent("gui.recruits.inv.health");
    private static final MutableComponent TEXT_LEVEL = new TranslatableComponent("gui.recruits.inv.level");
    private static final MutableComponent TEXT_KILLS = new TranslatableComponent("gui.recruits.inv.kills");
    private static final MutableComponent TEXT_DISBAND = new TranslatableComponent("gui.recruits.inv.text.disband");

    private static final MutableComponent TEXT_INFO_FOLLOW = new TranslatableComponent("gui.recruits.inv.info.text.follow");
    private static final MutableComponent TEXT_INFO_WANDER = new TranslatableComponent("gui.recruits.inv.info.text.wander");
    private static final MutableComponent TEXT_INFO_HOLD_POS = new TranslatableComponent("gui.recruits.inv.info.text.hold_pos");
    private static final MutableComponent TEXT_INFO_PASSIVE = new TranslatableComponent("gui.recruits.inv.info.text.passive");
    private static final MutableComponent TEXT_INFO_NEUTRAL = new TranslatableComponent("gui.recruits.inv.info.text.neutral");
    private static final MutableComponent TEXT_INFO_AGGRESSIVE = new TranslatableComponent("gui.recruits.inv.info.text.aggressive");
    private static final MutableComponent TEXT_INFO_LISTEN = new TranslatableComponent("gui.recruits.inv.info.text.listen");
    private static final MutableComponent TEXT_INFO_IGNORE = new TranslatableComponent("gui.recruits.inv.info.text.ignore");
    private static final MutableComponent TEXT_INFO_RAID = new TranslatableComponent("gui.recruits.inv.info.text.raid");
    private static final MutableComponent TEXT_INFO_PROTECT = new TranslatableComponent("gui.recruits.inv.info.text.protect");
    private static final MutableComponent TEXT_DISMOUNT = new TranslatableComponent("gui.recruits.inv.text.dismount");
    private static final MutableComponent TOOLTIP_DISMOUNT = new TranslatableComponent("gui.recruits.inv.tooltip.dismount");
    private static final MutableComponent TOOLTIP_FOLLOW = new TranslatableComponent("gui.recruits.inv.tooltip.follow");
    private static final MutableComponent TOOLTIP_WANDER = new TranslatableComponent("gui.recruits.inv.tooltip.wander");
    private static final MutableComponent TOOLTIP_HOLD_MY_POS = new TranslatableComponent("gui.recruits.inv.tooltip.holdMyPos");
    private static final MutableComponent TOOLTIP_HOLD_POS = new TranslatableComponent("gui.recruits.inv.tooltip.holdPos");
    private static final MutableComponent TOOLTIP_BACK_TO_POS = new TranslatableComponent("gui.recruits.inv.tooltip.backToPos");
    private static final MutableComponent TOOLTIP_CLEAR_TARGET = new TranslatableComponent("gui.recruits.inv.tooltip.clearTargets");
    private static final MutableComponent TOOLTIP_PASSIVE = new TranslatableComponent("gui.recruits.inv.tooltip.passive");
    private static final MutableComponent TOOLTIP_NEUTRAL = new TranslatableComponent("gui.recruits.inv.tooltip.neutral");
    private static final MutableComponent TOOLTIP_AGGRESSIVE = new TranslatableComponent("gui.recruits.inv.tooltip.aggressive");
    private static final MutableComponent TOOLTIP_RAID = new TranslatableComponent("gui.recruits.inv.tooltip.raid");
    private static final MutableComponent TOOLTIP_MOUNT = new TranslatableComponent("gui.recruits.inv.tooltip.mount");
    private static final MutableComponent TEXT_FOLLOW = new TranslatableComponent("gui.recruits.inv.text.follow");
    private static final MutableComponent TEXT_WANDER = new TranslatableComponent("gui.recruits.inv.text.wander");
    private static final MutableComponent TEXT_HOLD_MY_POS = new TranslatableComponent("gui.recruits.inv.text.holdMyPos");
    private static final MutableComponent TEXT_HOLD_POS = new TranslatableComponent("gui.recruits.inv.text.holdPos");
    private static final MutableComponent TEXT_BACK_TO_POS = new TranslatableComponent("gui.recruits.inv.text.backToPos");
    private static final MutableComponent TEXT_PASSIVE = new TranslatableComponent("gui.recruits.inv.text.passive");
    private static final MutableComponent TEXT_NEUTRAL = new TranslatableComponent("gui.recruits.inv.text.neutral");
    private static final MutableComponent TEXT_AGGRESSIVE = new TranslatableComponent("gui.recruits.inv.text.aggressive");
    private static final MutableComponent TEXT_RAID = new TranslatableComponent("gui.recruits.inv.text.raid");
    private static final MutableComponent TEXT_CLEAR_TARGET = new TranslatableComponent("gui.recruits.inv.text.clearTargets");
	private static final MutableComponent TEXT_MOUNT = new TranslatableComponent("gui.recruits.command.text.mount");

    private static final MutableComponent TEXT_PROMOTE = new TranslatableComponent("gui.recruits.inv.text.promote");
    private static final MutableComponent TEXT_SPECIAL = new TranslatableComponent("gui.recruits.inv.text.special");
    private static final MutableComponent TOOLTIP_PROMOTE = new TranslatableComponent("gui.recruits.inv.tooltip.promote");
    private static final MutableComponent TOOLTIP_SPECIAL = new TranslatableComponent("gui.recruits.inv.tooltip.special");
    private static final int fontColor = 4210752;
    private final AbstractRecruitEntity recruit;
    private final Inventory playerInventory;

    private int group;
    private int follow;
    private int aggro;

    public RecruitInventoryScreen(RecruitInventoryMenu recruitContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, recruitContainer, playerInventory, new TextComponent(""));
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
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_PASSIVE,
            button -> {
                this.aggro = recruit.getState();
                if (this.aggro != 3) {
                    this.aggro = 3;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));

                }
            },
            (button1, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_PASSIVE, i, i1);
            }
        ));

        //NEUTRAL
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_NEUTRAL,
            button -> {
                this.aggro = recruit.getState();
                if (this.aggro != 0) {
                    this.aggro = 0;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
                }
            },
            (button1, poseStack, i, i1) -> {
                this.renderTooltip(poseStack, TOOLTIP_NEUTRAL, i, i1);
            }
        ));

        //AGGRESSIVE
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_AGGRESSIVE,
            button -> {
                this.aggro = recruit.getState();
                if (this.aggro != 1) {
                    this.aggro = 1;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
                }
            },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_AGGRESSIVE, i, i1);
                }
        ));

        //RAID
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_RAID,
            button -> {
                this.aggro = recruit.getState();
                if (this.aggro != 2) {
                    this.aggro = 2;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageAggroGui(aggro, recruit.getUUID()));
                }
            },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_RAID, i, i1);
                }
        ));

        //CLEAR TARGET
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_CLEAR_TARGET,
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageClearTargetGui(playerInventory.player.getUUID(), recruit.getUUID()));
            },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_CLEAR_TARGET, i, i1);
                }
        ));


        //MOUNT
        addRenderableWidget(new Button(zeroLeftPos - 270, zeroTopPos + (20 + topPosGab) * 5, 80, 20, TEXT_MOUNT,
            button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageMountEntityGui(recruit.getUUID()));
            },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_MOUNT, i, i1);
                }
        ));


        //WANDER
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_WANDER,
            button -> {
                this.follow = recruit.getFollowState();
                if (this.follow != 0) {
                    this.follow = 0;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
                }
            },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_WANDER, i, i1);
                }
        ));


        //FOLLOW
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_FOLLOW,
            button -> {
                this.follow = recruit.getFollowState();
                if (this.follow != 1) {
                    this.follow = 1;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
                }
            },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_FOLLOW, i, i1);
                }
        ));


        //HOLD POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_HOLD_POS,
            button -> {
                this.follow = recruit.getFollowState();
                if (this.follow != 2) {
                    this.follow = 2;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
                }
            },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_HOLD_POS, i, i1);
                }
        ));


        //BACK TO POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_BACK_TO_POS,
            button -> {
                this.follow = recruit.getFollowState();
                if (this.follow != 3) {
                    this.follow = 3;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
                }
            },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_BACK_TO_POS, i, i1);
                }
        ));


        //HOLD MY POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_HOLD_MY_POS,
            button -> {
                this.follow = recruit.getFollowState();
                if (this.follow != 4) {
                    this.follow = 4;
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageFollowGui(follow, recruit.getUUID()));
                }
            },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_HOLD_MY_POS, i, i1);
                }
        ));

        //HOLD MY POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 5, 80, 20, TEXT_DISMOUNT,
                button -> {
                    this.follow = recruit.getFollowState();
                    if (this.follow != 4) {
                        this.follow = 4;
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageDismountGui(playerInventory.player.getUUID(), recruit.getUUID()));
                    }
                },
                (button1, poseStack, i, i1) -> {
                    this.renderTooltip(poseStack, TOOLTIP_DISMOUNT, i, i1);
                }
        ));

        //LISTEN
        addRenderableWidget(new Button(leftPos + 77, topPos + 113, 8, 12, new TextComponent("<"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageListen(!recruit.getListen(), recruit.getUUID()));
        }));

        addRenderableWidget(new Button(leftPos + 77 + 85, topPos + 113, 8, 12, new TextComponent(">"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageListen(!recruit.getListen(), recruit.getUUID()));
        }));


        //GROUP
        addRenderableWidget(new Button(leftPos + 77, topPos + 100, 8, 12, new TextComponent("<"), button -> {
            this.group = recruit.getGroup();
            if (this.group != 0) {
                this.group--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageGroup(this.group, recruit.getUUID()));
            }
        }));

        addRenderableWidget(new Button(leftPos + 77 + 85, topPos + 100, 8, 12, new TextComponent(">"), button -> {
            this.group = recruit.getGroup();
            if (this.group != 9) {
                this.group++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageGroup(this.group, recruit.getUUID()));
            }
        }));

        //more
        addRenderableWidget(new Button(leftPos + 77 + 55, topPos + 4, 40, 12, new TextComponent("..."),
                button -> {
                    TeamEvents.openDisbandingScreen(this.playerInventory.player, this.recruit.getUUID());
                    this.onClose();
                }
        ));

        //promote
        if(recruit instanceof ICompanion){
            Button promoteButton = addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 7, 80, 20, TEXT_SPECIAL,
                    button -> {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenSpecialScreen(this.playerInventory.player, recruit.getUUID()));
                        this.onClose();
                    },
                    (button1, poseStack, i, i1) -> {
                        this.renderTooltip(poseStack, TOOLTIP_SPECIAL, i, i1);
                    }
            ));
            promoteButton.active = false && recruit.getXpLevel() >= 3;

        }
        else {
            Button promoteButton = addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 7, 80, 20, TEXT_PROMOTE,
                    button -> {
                        RecruitEvents.openPromoteScreen(this.playerInventory.player, this.recruit);
                        this.onClose();
                    },
                    (button1, poseStack, i, i1) -> {
                        this.renderTooltip(poseStack, TOOLTIP_PROMOTE, i, i1);
                    }
            ));
            promoteButton.active = false && recruit.getXpLevel() >= 3;
        }
    }


    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int health = Mth.ceil(recruit.getHealth());
        int hunger = Mth.ceil(recruit.getHunger());
        int moral = Mth.ceil(recruit.getMoral());
        this.follow = recruit.getFollowState();
        this.aggro = recruit.getState();

        int k = 79;//rechst links
        int l = 19;//höhe

        //Titles

        font.draw(matrixStack, recruit.getDisplayName().getVisualOrderText(), 8, 5, fontColor);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor);

        matrixStack.pushPose();
        matrixStack.scale(0.7F, 0.7F, 1F);

        k = 112;//rechst links
        l = 32;//höhe
        int gap = 42;
        //Info

        font.draw(matrixStack, "Health:", k, l, fontColor);
        font.draw(matrixStack, "" + health, k + gap, l, fontColor);
        font.draw(matrixStack, "Level.:", k, l + 10, fontColor);
        font.draw(matrixStack, "" + recruit.getXpLevel(), k + gap , l + 10, fontColor);
        font.draw(matrixStack, "Exp.:", k, l + 20, fontColor);
        font.draw(matrixStack, "" + recruit.getXp(), k + gap, l + 20, fontColor);
        font.draw(matrixStack, "Kills:", k, l + 30, fontColor);
        font.draw(matrixStack, ""+ recruit.getKills(), k + gap, l + 30, fontColor);
        font.draw(matrixStack, "Morale:", k, l + 40, fontColor);
        font.draw(matrixStack, ""+ moral, k + gap, l + 40, fontColor);
		font.draw(matrixStack, "Hunger:", k, l + 50, fontColor);
        font.draw(matrixStack, ""+ hunger, k + gap, l + 50, fontColor);
        matrixStack.popPose();
        /*
        font.draw(matrixStack, "Moral:", k, l + 30, fontColor);
        font.draw(matrixStack, ""+ recruit.getKills(), k + 25, l + 30, fontColor);
        */
        k = 79;//rechst links
        l = 19;//höhe
        // command
        String follow = switch (this.follow) {
            case 0 -> TEXT_INFO_WANDER.getString();
            case 1 -> TEXT_INFO_FOLLOW.getString();
            case 2, 3, 4 -> TEXT_INFO_HOLD_POS.getString();
            case 5 -> TEXT_INFO_PROTECT.getString();
            default -> throw new IllegalStateException("Unexpected value: " + this.follow);
        };
        font.draw(matrixStack, follow, k + 15, l + 58 + 0, fontColor);

        String aggro = switch (this.aggro) {
            case 0 -> TEXT_INFO_NEUTRAL.getString();
            case 1 -> TEXT_INFO_AGGRESSIVE.getString();
            case 2 -> TEXT_INFO_RAID.getString();
            case 3 -> TEXT_INFO_PASSIVE.getString();
            default -> throw new IllegalStateException("Unexpected value: " + this.aggro);
        };
        font.draw(matrixStack, aggro, k + 15, l + 56 + 15, fontColor);

        font.draw(matrixStack, CommandScreen.handleGroupText(recruit.getGroup()), k + 15, l + 56 + 28, fontColor);

        String listen;
        if (recruit.getListen()) listen = TEXT_INFO_LISTEN.getString();
        else listen = TEXT_INFO_IGNORE.getString();
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
