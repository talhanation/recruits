package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.events.ClientEvent;
import com.talhanation.recruits.inventory.CommandMenu;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;


@OnlyIn(Dist.CLIENT)
public class CommandScreen extends ScreenBase<CommandMenu> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/command_gui.png");

    private static final MutableComponent TOOLTIP_DISMOUNT = Component.translatable("gui.recruits.command.tooltip.dismount");
    private static final MutableComponent TOOLTIP_MOUNT = Component.translatable("gui.recruits.command.tooltip.mount");
    private static final MutableComponent TOOLTIP_SHIELDS = Component.translatable("gui.recruits.command.tooltip.shields");
    private static final MutableComponent TOOLTIP_ESCORT = Component.translatable("gui.recruits.command.tooltip.escort");
    private static final MutableComponent TOOLTIP_MOVE = Component.translatable("gui.recruits.command.tooltip.move");
    private static final MutableComponent TOOLTIP_FOLLOW = Component.translatable("gui.recruits.command.tooltip.follow");
    private static final MutableComponent TOOLTIP_WANDER = Component.translatable("gui.recruits.command.tooltip.wander");
    private static final MutableComponent TOOLTIP_HOLD_MY_POS = Component.translatable("gui.recruits.command.tooltip.holdMyPos");
    private static final MutableComponent TOOLTIP_HOLD_POS = Component.translatable("gui.recruits.command.tooltip.holdPos");
    private static final MutableComponent TOOLTIP_BACK_TO_POS = Component.translatable("gui.recruits.command.tooltip.backToPos");
    private static final MutableComponent TOOLTIP_PASSIVE = Component.translatable("gui.recruits.command.tooltip.passive");
    private static final MutableComponent TOOLTIP_NEUTRAL = Component.translatable("gui.recruits.command.tooltip.neutral");
    private static final MutableComponent TOOLTIP_AGGRESSIVE = Component.translatable("gui.recruits.command.tooltip.aggressive");
    private static final MutableComponent TOOLTIP_RAID = Component.translatable("gui.recruits.command.tooltip.raid");
    private static final MutableComponent TOOLTIP_UPKEEP = Component.translatable("gui.recruits.command.tooltip.upkeep");
    private static final MutableComponent TOOLTIP_TEAM = Component.translatable("gui.recruits.command.tooltip.team");
    private static final MutableComponent TOOLTIP_CLEAR_TARGET = Component.translatable("gui.recruits.command.tooltip.clearTargets");
    private static final MutableComponent TEXT_EVERYONE = Component.translatable("gui.recruits.command.text.everyone");
    private static final MutableComponent TEXT_ESCORT = Component.translatable("gui.recruits.command.text.escort");
    private static final MutableComponent TEXT_MOVE = Component.translatable("gui.recruits.command.text.move");
    private static final MutableComponent TEXT_SHIELDS = Component.translatable("gui.recruits.command.text.shields");
    private static final MutableComponent TEXT_DISMOUNT = Component.translatable("gui.recruits.command.text.dismount");
    private static final MutableComponent TEXT_MOUNT = Component.translatable("gui.recruits.command.text.mount");
    private static final MutableComponent TEXT_FOLLOW = Component.translatable("gui.recruits.command.text.follow");
    private static final MutableComponent TEXT_WANDER = Component.translatable("gui.recruits.command.text.wander");
    private static final MutableComponent TEXT_HOLD_MY_POS = Component.translatable("gui.recruits.command.text.holdMyPos");
    private static final MutableComponent TEXT_HOLD_POS = Component.translatable("gui.recruits.command.text.holdPos");
    private static final MutableComponent TEXT_BACK_TO_POS = Component.translatable("gui.recruits.command.text.backToPos");

    private static final MutableComponent TEXT_PASSIVE = Component.translatable("gui.recruits.command.text.passive");
    private static final MutableComponent TEXT_NEUTRAL = Component.translatable("gui.recruits.command.text.neutral");
    private static final MutableComponent TEXT_AGGRESSIVE = Component.translatable("gui.recruits.command.text.aggressive");
    private static final MutableComponent TEXT_RAID = Component.translatable("gui.recruits.command.text.raid");
    private static final MutableComponent TEXT_HAILOFARROWS = Component.translatable("gui.recruits.command.text.arrow");
    private static final MutableComponent TEXT_CLEAR_TARGET = Component.translatable("gui.recruits.command.text.clearTargets");
    private static final MutableComponent TEXT_UPKEEP = Component.translatable("gui.recruits.command.text.upkeep");
    private static final MutableComponent TEXT_TEAM = Component.translatable("gui.recruits.command.text.team");
    private static final int fontColor = 16250871;
    private Player player;
    private int group;
    private int recCount;
    private boolean shields;
    private boolean hailOfArrows;

    public CommandScreen(CommandMenu commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, Component.literal(""));
        imageWidth = 201;
        imageHeight = 170;
        player = playerInventory.player;
    }

    @Override
    public boolean keyReleased(int x, int y, int z) {
        super.keyReleased(x, y, z);
        this.onClose();

        return true;
    }

    @Override
    protected void init() {
        super.init();
        int zeroLeftPos = leftPos + 150;
        int zeroTopPos = topPos + 10;
        int topPosGab = 7;
        int mirror = 240 - 60;

        //TEAM SCREEN
        addRenderableWidget(new Button(zeroLeftPos - 90, zeroTopPos + (20 + topPosGab) * 5 + 60, 80, 20, TEXT_TEAM,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageTeamMainScreen(player));
                }));

        //Dismount
        addRenderableWidget(new Button(zeroLeftPos - 90, zeroTopPos + (20 + topPosGab) * 5 + 10, 80, 20, TEXT_DISMOUNT,
                button -> {
                    CommandEvents.sendFollowCommandInChat(98, player, group);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageDismount(player.getUUID(), group));
                }));

        //Mount
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 5 + 10, 80, 20, TEXT_MOUNT,
                button -> {
                    CommandEvents.sendFollowCommandInChat(99, player, group);
                    Entity entity = ClientEvent.getEntityByLooking();
                    if (entity != null) {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageMountEntity(player.getUUID(), entity.getUUID(), group));
                    }
                }));

        //HAIL OF ARROWS
        addRenderableWidget(new Button(zeroLeftPos - 90, zeroTopPos + (20 + topPosGab) * 5 + 35, 80, 20, TEXT_HAILOFARROWS,
                button -> {
                    this.hailOfArrows = !getSavedHailOfArrowsBool(player);

                    if (hailOfArrows)
                        CommandEvents.sendFollowCommandInChat(96, player, group);
                    else
                        CommandEvents.sendFollowCommandInChat(94, player, group);

                    Main.SIMPLE_CHANNEL.sendToServer(new MessageHailOfArrows(player.getUUID(), group, hailOfArrows));

                    saveHailOfArrowsBool(player);
                }));

        //MOVE
        addRenderableWidget(new Button(zeroLeftPos - 90, zeroTopPos - (20 + topPosGab), 80, 20, TEXT_MOVE,
                button -> {
                    CommandEvents.sendFollowCommandInChat(97, player, group);
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageMove(player.getUUID(), group));
                }));

        //UPKEEP
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 5 + 35, 80, 20, TEXT_UPKEEP,
                button -> {
                    CommandEvents.sendFollowCommandInChat(92, player, group);
                    Entity entity = ClientEvent.getEntityByLooking();
                    //Main.LOGGER.debug("client: entity: " + entity);

                    if (entity != null) {
                        //Main.LOGGER.debug("client: uuid: " + entity.getUUID());
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpkeepEntity(player.getUUID(), entity.getUUID(), group));
                    } else
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageUpkeepPos(player.getUUID(), group));
                }));

        //SHIELDS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 5 + 35, 80, 20, TEXT_SHIELDS,
                button -> {
                    this.shields = !getSavedShieldBool(player);

                    if (shields)
                        CommandEvents.sendFollowCommandInChat(95, player, group);
                    else
                        CommandEvents.sendFollowCommandInChat(93, player, group);

                    Main.SIMPLE_CHANNEL.sendToServer(new MessageShields(player.getUUID(), group, shields));

                    saveShieldBool(player);
                }));

        //Guard
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 5 + 10, 80, 20, TEXT_ESCORT,
                button -> {
                    CommandEvents.sendFollowCommandInChat(5, player, group);
                    Entity entity = ClientEvent.getEntityByLooking();
                    if (entity != null) {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageGuardEntity(player.getUUID(), entity.getUUID(), group));
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 5, group));
                    }
                }));

        //PASSIVE
        addRenderableWidget(new Button(zeroLeftPos - mirror + 40, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_PASSIVE,
                button -> {
            CommandEvents.sendAggroCommandInChat(3, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 3, group));

        }));

        //NEUTRAL
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_NEUTRAL,
                button -> {
            CommandEvents.sendAggroCommandInChat(0, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 0, group));

        }));

        //AGGRESSIVE
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_AGGRESSIVE,
                button -> {
            CommandEvents.sendAggroCommandInChat(1, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 1, group));
        }));

        //RAID
        addRenderableWidget(new Button(zeroLeftPos - mirror, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_RAID,
                button -> {
            CommandEvents.sendAggroCommandInChat(2, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAggro(player.getUUID(), 2, group));

        }));

        //CLEAR TARGET
        addRenderableWidget(new Button(zeroLeftPos - mirror + 40, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_CLEAR_TARGET,
                button -> {
            //Main.LOGGER.debug("client: clear target");
            Main.SIMPLE_CHANNEL.sendToServer(new MessageClearTarget(player.getUUID(), group));
        }));


        //WANDER
        addRenderableWidget(new Button(zeroLeftPos - 40, zeroTopPos + (20 + topPosGab) * 0, 80, 20, TEXT_WANDER,
                button -> {
            CommandEvents.sendFollowCommandInChat(0, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 0, group));

        }));


        //FOLLOW
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 1, 80, 20, TEXT_FOLLOW,
                button -> {
            CommandEvents.sendFollowCommandInChat(1, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 1, group));

        }));


        //HOLD POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 2, 80, 20, TEXT_HOLD_POS,
                button -> {
            CommandEvents.sendFollowCommandInChat(2, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 2, group));

        }));


        //BACK TO POS
        addRenderableWidget(new Button(zeroLeftPos, zeroTopPos + (20 + topPosGab) * 3, 80, 20, TEXT_BACK_TO_POS,
                button -> {
            CommandEvents.sendFollowCommandInChat(3, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 3, group));

        }));


        //HOLD MY POS
        addRenderableWidget(new Button(zeroLeftPos - 40, zeroTopPos + (20 + topPosGab) * 4, 80, 20, TEXT_HOLD_MY_POS,
                button -> {
            CommandEvents.sendFollowCommandInChat(4, player, group);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 4, group));

        }));

        //GROUP
        addRenderableWidget(new ExtendedButton(leftPos - 4 + imageWidth / 2, topPos - 40 + imageHeight / 2, 11, 20, Component.literal("+"),
                button -> {
            this.group = getSavedCurrentGroup(player);

            if (this.group != 9) {
                this.group++;

                this.saveCurrentGroup(player);
            }
        }));

        addRenderableWidget(new ExtendedButton(leftPos - 4 + imageWidth / 2, topPos + imageHeight / 2, 11, 20, Component.literal("-"),
                button -> {
            this.group = getSavedCurrentGroup(player);

            if (this.group != 0) {
                this.group--;

                this.saveCurrentGroup(player);
            }
        }));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);

        //Main.SIMPLE_CHANNEL.sendToServer(new MessageRecruitsInCommand(player.getUUID()));

        //this.recCount = getSavedRecruitCount(player);
        this.group = getSavedCurrentGroup(player);
        //player.sendMessage(new StringTextComponent("SCREEN int: " + recCount), player.getUUID());

        int k = 78;//rechst links
        int l = 71;//höhe

        font.draw(matrixStack, "" + handleGroupText(this.group), k, l, fontColor);
        //font.draw(matrixStack, "" +  handleRecruitCountText(currentRecruits), k - 30 , 0, fontColor);
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
    }

    public static String handleGroupText(int group) {
        if (group == 0) {
            return TEXT_EVERYONE.getString();
        } else
            return TEXT_GROUP(String.valueOf(group)).getString();
    }

    public int getSavedCurrentGroup(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getInt("CommandingGroup");
    }

    public void saveCurrentGroup(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putInt("CommandingGroup", this.group);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    public boolean getSavedShieldBool(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getBoolean("Shields");
    }

    public void saveShieldBool(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putBoolean("Shields", this.shields);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    public boolean getSavedHailOfArrowsBool(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        return nbt.getBoolean("HailOfArrows");
    }

    public void saveHailOfArrowsBool(Player player) {
        CompoundTag playerNBT = player.getPersistentData();
        CompoundTag nbt = playerNBT.getCompound(Player.PERSISTED_NBT_TAG);

        nbt.putBoolean("HailOfArrows", this.hailOfArrows);
        playerNBT.put(Player.PERSISTED_NBT_TAG, nbt);
    }

    private static MutableComponent TEXT_GROUP(String group) {
            return Component.translatable("gui.recruits.command.text.group", group);
    }
}
