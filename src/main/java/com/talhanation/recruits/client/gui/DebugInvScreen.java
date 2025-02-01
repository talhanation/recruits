package com.talhanation.recruits.client.gui;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.DebugInvMenu;
import com.talhanation.recruits.network.MessageDebugGui;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.common.ForgeMod;
import org.lwjgl.glfw.GLFW;


import org.lwjgl.glfw.GLFW;
import java.text.DecimalFormat;

@OnlyIn(Dist.CLIENT)
public class DebugInvScreen extends ScreenBase<DebugInvMenu> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/debug_gui.png" );

    private static final int fontColor = 4210752;
    private EditBox textField;

    private final AbstractRecruitEntity recruit;
    private final Player player;
    private final Inventory playerInventory;

    private int follow;
    private int aggro;
    public DebugInvScreen(DebugInvMenu commandContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, Component.literal(""));
        imageWidth = 201;
        imageHeight = 250;
        this.recruit = commandContainer.getRecruit();
        this.player = playerInventory.player;
        this.playerInventory = playerInventory;
    }

    @Override
    protected void init() {
        super.init();

        int zeroLeftPos = leftPos + 100;
        int zeroTopPos = topPos + 10;

        xpButton(zeroLeftPos, zeroTopPos);
        lvlButton(zeroLeftPos, zeroTopPos);
        costButton(zeroLeftPos, zeroTopPos);
        hungerButton(zeroLeftPos, zeroTopPos);
        moralButton(zeroLeftPos, zeroTopPos);
        healthButton(zeroLeftPos, zeroTopPos);
        variantButton(zeroLeftPos, zeroTopPos);
        biomeButton(zeroLeftPos, zeroTopPos);
        killHealButton(zeroLeftPos, zeroTopPos);
        clearButton(zeroLeftPos, zeroTopPos);
        clearButton2(zeroLeftPos, zeroTopPos);
        promoteButton(zeroLeftPos, zeroTopPos);
        clearTeam(zeroLeftPos, zeroTopPos);
        teamColorButton(zeroLeftPos, zeroTopPos);
        disband(zeroLeftPos, zeroTopPos);

        Component name = Component.literal("Name");
        if(recruit.getCustomName() != null) name = recruit.getCustomName();

        textField = new EditBox(font, leftPos + 18, topPos - 23, 140, 20, name);
        textField.setValue(name.getString());
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);

        addRenderableWidget(textField);
        setInitialFocus(textField);
    }

    protected void containerTick() {
        super.containerTick();
        if(textField != null) textField.tick();
    }


    public boolean mouseClicked(double p_100753_, double p_100754_, int p_100755_) {
        if (this.textField.isFocused()) {
            this.textField.mouseClicked(p_100753_, p_100754_, p_100755_);
        }
        return super.mouseClicked(p_100753_, p_100754_, p_100755_);
    }

    @Override
    public boolean keyPressed(int key, int a, int b) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        setFocused(textField);

        return textField.keyPressed(key, a, b) || textField.canConsumeInput() || super.keyPressed(key, a, b);
    }
    @Override
    public void onClose() {
        super.onClose();

        Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(99, recruit.getUUID(), textField.getValue()));
    }

    private void xpButton(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 0, 40, 20, Component.literal("+xp"), button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(0, recruit.getUUID(), textField.getValue()));
        }));
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 160, zeroTopPos + (20 + 5) * 0, 40, 20, Component.literal("+10xp"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(21, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void lvlButton(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 1, 40, 20, Component.literal("+lvl"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(1, recruit.getUUID(), textField.getValue()));
        }));
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 160, zeroTopPos + (20 + 5) * 1, 40, 20, Component.literal("+5lvl"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(22, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void costButton(int zeroLeftPos, int zeroTopPos){
        //increase cost
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 2, 40, 20, Component.literal("+cost"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(2, recruit.getUUID(), textField.getValue()));
        }));
        //decrease cost
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 160, zeroTopPos + (20 + 5) * 2, 40, 20, Component.literal("-cost"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(3, recruit.getUUID(), textField.getValue()));

        }));
    }
    private void hungerButton(int zeroLeftPos, int zeroTopPos){
        //increase hunger
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 3, 40, 20, Component.literal("+hunger"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(4, recruit.getUUID(), textField.getValue()));
        }));

        //decrease hunger
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 160, zeroTopPos + (20 + 5) * 3, 40, 20, Component.literal("-hunger"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(5, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void moralButton(int zeroLeftPos, int zeroTopPos){
        //increase moral
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 4, 40, 20, Component.literal("+morale"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(6, recruit.getUUID(), textField.getValue()));
        }));

        //decrease moral
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 160, zeroTopPos + (20 + 5) * 4, 40, 20, Component.literal("-morale"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(7, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void healthButton(int zeroLeftPos, int zeroTopPos){
        //increase health
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 5, 40, 20, Component.literal("+health"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(8, recruit.getUUID(), textField.getValue()));
        }));

        //decrease health
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 160, zeroTopPos + (20 + 5) * 5, 40, 20, Component.literal("-health"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(9, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void variantButton(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 6, 40, 20, Component.literal("+variant"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(10, recruit.getUUID(), textField.getValue()));
        }));

        addRenderableWidget(new ExtendedButton(zeroLeftPos - 160, zeroTopPos + (20 + 5) * 6, 40, 20, Component.literal("-variant"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(11, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void biomeButton(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 7, 40, 20, Component.literal("+biome"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(12, recruit.getUUID(), textField.getValue()));
        }));

        addRenderableWidget(new ExtendedButton(zeroLeftPos - 160, zeroTopPos + (20 + 5) * 7, 40, 20, Component.literal("-biome"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(13, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void teamColorButton(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 8, 40, 20, Component.literal("+tcolor"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(24, recruit.getUUID(), textField.getValue()));
        }));

        addRenderableWidget(new ExtendedButton(zeroLeftPos - 160, zeroTopPos + (20 + 5) * 8, 40, 20, Component.literal("-tcolor"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(25, recruit.getUUID(), textField.getValue()));
        }));
    }

    ////RIGHT SIDE

    private void killHealButton(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos + 130, zeroTopPos + (20 + 5) * 2, 40, 20, Component.literal("heal"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(14, recruit.getUUID(), textField.getValue()));
        }));

        addRenderableWidget(new ExtendedButton(zeroLeftPos + 80, zeroTopPos + (20 + 5) * 2, 40, 20, Component.literal("-kill"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(15, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void clearButton(int zeroLeftPos, int zeroTopPos) {
        addRenderableWidget(new ExtendedButton(zeroLeftPos + 130, zeroTopPos + (20 + 5) * 3, 40, 20, Component.literal("c upkeep"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(16, recruit.getUUID(), textField.getValue()));
        }));

        addRenderableWidget(new ExtendedButton(zeroLeftPos + 80, zeroTopPos + (20 + 5) * 3, 40, 20, Component.literal("c hold"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(17, recruit.getUUID(), textField.getValue()));
        }));
    }
    private void clearButton2(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos + 130, zeroTopPos + (20 + 5) * 4, 40, 20, Component.literal("c prot"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(18, recruit.getUUID(), textField.getValue()));
        }));

        addRenderableWidget(new ExtendedButton(zeroLeftPos + 80, zeroTopPos + (20 + 5) * 4, 40, 20, Component.literal("c mount"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(19, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void promoteButton(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos + 80, zeroTopPos + (20 + 5) * 5, 40, 20, Component.literal("pro"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(20, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void clearTeam(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos + 80, zeroTopPos + (20 + 5) * 6, 40, 20, Component.literal("c team"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(23, recruit.getUUID(), textField.getValue()));
        }));
    }

    private void disband(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos + 80, zeroTopPos + (20 + 5) * 7, 40, 20, Component.literal("c owner"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(26, recruit.getUUID(), textField.getValue()));
        }));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        int health = Mth.ceil(recruit.getHealth());
        int maxHealth = Mth.ceil(recruit.getMaxHealth());
        int moral = Mth.ceil(recruit.getMorale());

        double attackReach = recruit.getAttributeValue(ForgeMod.ENTITY_REACH.get());
        double attackSpeed = recruit.getAttributeValue(Attributes.ATTACK_SPEED);
        double attackDamage = recruit.getAttackDamage();
        DecimalFormat decimalformat = new DecimalFormat("##.#");
        double armor = recruit.getArmorValue();
        int costs = recruit.getCost();
        double hunger = recruit.getHunger();
        int group = recruit.getGroup();
        String team = recruit.getTeam() != null ? recruit.getTeam().getName() : "null";

        int k = 30;//rechst links
        int l = 15;//höhe
        String follow = switch (this.recruit.getFollowState()) {
            case 0 -> "wander";
            case 1 -> "follow";
            case 2 -> "hold my pos";
            case 3 -> "back to pos";
            case 4 -> "hold your pos";
            case 5 -> "protect";
            default -> "{}";
        };

        String aggro = switch (this.recruit.getState()) {
            case 0 -> "neutral";
            case 1 -> "aggro";
            case 2 -> "raid";
            case 3 -> "passive";
            default -> "{}";
        };

        String biome = switch (this.recruit.getBiome()) {
            case 0 -> "desert";
            case 1 -> "jungle";
            case 2 -> "plains";
            case 3 -> "savanna";
            case 4 -> "snowy";
            case 5 -> "swamp";
            case 6 -> "taiga";
            default -> "{}";
        };

        String tcolor = switch (this.recruit.getColor()) {
            case 0 -> "white";
            case 1 -> "black";
            case 2 -> "light_gray";
            case 3 -> "gray";
            case 4 -> "dark_gray";
            case 5 -> "light_blue";
            case 6 -> "blue";
            case 7 -> "dark_blue";
            case 8 -> "light_green";
            case 9 -> "green";
            case 10 -> "dark_green";
            case 11 -> "light_red";
            case 12 -> "red";
            case 13 -> "dark_red";
            case 14 -> "light_brown";
            case 15 -> "brown";
            case 16 -> "dark_brown";
            case 17 -> "light_cyan";
            case 18 -> "cyan";
            case 19 -> "dark_cyan";
            case 20 -> "yellow";
            case 21 -> "orange";
            case 22 -> "magenta";
            case 23 -> "purple";
            case 24 -> "gold";
            default -> "{}";
        };

        guiGraphics.pose();
        guiGraphics.pose().scale(0.7F, 0.7F, 1F);

        //Titles
        guiGraphics.drawString(font, recruit.getDisplayName().getVisualOrderText(), 8, 5, fontColor, false);

        //Info
        guiGraphics.drawString(font, "Hp:", k, l, fontColor, false);
        guiGraphics.drawString(font, "" + health + "/" + maxHealth, k + 25, l, fontColor, false);

        guiGraphics.drawString(font, "Lvl:", k, l + 10, fontColor, false);
        guiGraphics.drawString(font, "" + recruit.getXpLevel(), k + 25, l + 10, fontColor, false);

        guiGraphics.drawString(font, "Exp:", k, l + 20, fontColor, false);
        guiGraphics.drawString(font, "" + recruit.getXp(), k + 25, l + 20, fontColor, false);

        guiGraphics.drawString(font, "Kills:", k, l + 30, fontColor, false);
        guiGraphics.drawString(font, "" + recruit.getKills(), k + 25, l + 30, fontColor, false);

        guiGraphics.drawString(font, "Morale:", k, l + 40, fontColor, false);
        guiGraphics.drawString(font, "" + moral, k + 40, l + 40, fontColor, false);

        guiGraphics.drawString(font, "Hunger:", k, l + 50, fontColor, false);
        guiGraphics.drawString(font, "" + decimalformat.format(hunger), k + 40, l + 50, fontColor, false);

        guiGraphics.drawString(font, "Upkeep Pos:", k, l + 60, fontColor, false);
        guiGraphics.drawString(font, "" + recruit.getUpkeepPos(), k + 43 + 20, l + 60, fontColor, false);

        guiGraphics.drawString(font, "MaxHp:", k + 43 + 20, l, fontColor, false);
        guiGraphics.drawString(font, "" + maxHealth, k + 77 + 20, l, fontColor, false);
/*
        guiGraphics.drawString(font, "Attack:", k + 43 + 20, l + 10, fontColor, false);
        guiGraphics.drawString(font, "" + A_damage, k + 77 + 20, l + 10, fontColor, false);

        guiGraphics.drawString(font, "Speed:", k + 43 + 20, l + 20, fontColor, false);
        guiGraphics.drawString(font, "" + decimalformat.format(speed), k + 77 + 20, l + 20, fontColor, false);
*/
        guiGraphics.drawString(font, "Armor:", k + 43 + 20, l + 30, fontColor, false);
        guiGraphics.drawString(font, "" + armor, k + 77 + 20, l + 30, fontColor, false);

        guiGraphics.drawString(font, "Cost:", k + 43 + 20, l + 40, fontColor, false);
        guiGraphics.drawString(font, "" + costs, k + 77 + 20, l + 40, fontColor, false);

        guiGraphics.drawString(font, "Team:", k, l + 70, fontColor, false);
        guiGraphics.drawString(font, ""+ team, k + 40, l + 70, fontColor, false);


        guiGraphics.drawString(font, "A Dmg:", k + 43 + 80, l, fontColor, false);
        guiGraphics.drawString(font, ""+ decimalformat.format(attackDamage), k + 90 + 80, l, fontColor, false);

        guiGraphics.drawString(font, "A reach:", k + 43 + 80, l + 10, fontColor, false);
        guiGraphics.drawString(font, ""+ decimalformat.format(attackReach), k + 90 + 80, l + 10, fontColor, false);

        guiGraphics.drawString(font, "A speed:", k + 43 + 80, l + 20, fontColor, false);
        guiGraphics.drawString(font, ""+ decimalformat.format(attackSpeed), k + 90 + 80, l + 20, fontColor, false);

        guiGraphics.drawString(font, "Armor:", k + 43 + 80, l + 30, fontColor, false);
        guiGraphics.drawString(font, ""+ armor, k + 90 + 80, l + 30, fontColor, false);

        guiGraphics.drawString(font, "Cost:", k + 43 + 80, l + 40, fontColor, false);
        guiGraphics.drawString(font, ""+ costs, k + 90 + 80, l + 40, fontColor, false);

        guiGraphics.drawString(font, "Group:", k + 43 + 80, l + 50, fontColor, false);
        guiGraphics.drawString(font, ""+ group, k + 90 + 80, l + 50, fontColor, false);

        guiGraphics.drawString(font, "Follow:", k + 43 + 80, l + 60, fontColor, false);
        guiGraphics.drawString(font, ""+ follow, k + 90 + 80, l + 60, fontColor, false);

        guiGraphics.drawString(font, "Aggro:", k + 43 + 80, l + 70, fontColor, false);
        guiGraphics.drawString(font, ""+ aggro, k + 90 + 80, l + 70, fontColor, false);

        guiGraphics.drawString(font, "Variant:", k + 43 + 80, l + 80, fontColor, false);
        guiGraphics.drawString(font, ""+ recruit.getVariant(), k + 90 + 80, l + 80, fontColor, false);

        guiGraphics.drawString(font, "Biome:", k + 43 + 80, l + 90, fontColor, false);
        guiGraphics.drawString(font, ""+ biome, k + 90 + 80, l + 90, fontColor, false);

        guiGraphics.drawString(font, "tColor:", k + 43 + 80, l + 100, fontColor, false);
        guiGraphics.drawString(font, ""+ tcolor, k + 90 + 80, l + 100, fontColor, false);
        guiGraphics.pose().popPose();
    }

    private int calculateADamage() {
        int damage = Math.round(recruit.getAttackDamage());
        Main.LOGGER.debug("damage: " + damage);
        ItemStack handItem = recruit.getItemInHand(InteractionHand.MAIN_HAND);
        if (handItem.getItem() instanceof SwordItem || handItem.getItem() instanceof AxeItem){

            damage += handItem.getDamageValue();
            Main.LOGGER.debug("Sword damage: " + handItem.getDamageValue());
            Main.LOGGER.debug("Sword damage: " + damage);
        }
        if (handItem.getItem() instanceof BowItem bow){
            damage += 8; // according to wiki
            Main.LOGGER.debug("Bow damage: " + damage);
        }
        if (handItem.getItem() instanceof CrossbowItem bow){
            damage += 11; // according to wiki
            Main.LOGGER.debug("Cross damage: " + damage);
        }
        return damage;
    }

}
