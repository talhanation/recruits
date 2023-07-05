package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.DebugInvMenu;
import com.talhanation.recruits.network.MessageDebugGui;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.text.DecimalFormat;

@OnlyIn(Dist.CLIENT)
public class DebugInvScreen extends ScreenBase<DebugInvMenu> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/debug_gui.png" );

    private static final int fontColor = 4210752;

    private final AbstractRecruitEntity recruit;
    private final Player player;
    private final Inventory playerInventory;

    private int group;
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

        int zeroLeftPos = leftPos + 140;
        int zeroTopPos = topPos + 10;

        int topPosGab = 5;


        xpButton(zeroLeftPos, zeroTopPos);
        lvlButton(zeroLeftPos, zeroTopPos);
        costButton(zeroLeftPos, zeroTopPos);
        hungerButton(zeroLeftPos, zeroTopPos);
        moralButton(zeroLeftPos, zeroTopPos);
        healthButton(zeroLeftPos, zeroTopPos);
    }

    private void xpButton(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 0, 23, 20, Component.literal("+xp"), button -> {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(0, recruit.getUUID()));
        }));
    }

    private void lvlButton(int zeroLeftPos, int zeroTopPos){
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 1, 23, 20, Component.literal("+lvl"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(1, recruit.getUUID()));
        }));
    }

    private void costButton(int zeroLeftPos, int zeroTopPos){
        //increase cost
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 2, 23, 20, Component.literal("+cost"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(2, recruit.getUUID()));
        }));
        //decrease cost
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 170, zeroTopPos + (20 + 5) * 2, 23, 20, Component.literal("-cost"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(3, recruit.getUUID()));
        }));
    }

    private void hungerButton(int zeroLeftPos, int zeroTopPos){
        //increase hunger
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 3, 23, 20, Component.literal("+hunger"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(4, recruit.getUUID()));
        }));

        //decrease hunger
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 170, zeroTopPos + (20 + 5) * 3, 23, 20, Component.literal("-hunger"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(5, recruit.getUUID()));
        }));
    }

    private void moralButton(int zeroLeftPos, int zeroTopPos){
        //increase moral
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 4, 23, 20, Component.literal("+moral"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(6, recruit.getUUID()));
        }));

        //decrease moral
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 170, zeroTopPos + (20 + 5) * 4, 23, 20, Component.literal("-moral"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(7, recruit.getUUID()));
        }));
    }

    private void healthButton(int zeroLeftPos, int zeroTopPos){
        //increase health
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 210, zeroTopPos + (20 + 5) * 5, 23, 20, Component.literal("+health"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(8, recruit.getUUID()));
        }));

        //decrease health
        addRenderableWidget(new ExtendedButton(zeroLeftPos - 170, zeroTopPos + (20 + 5) * 5, 23, 20, Component.literal("-health"), button -> {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageDebugGui(9, recruit.getUUID()));
        }));
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        int health = Mth.ceil(recruit.getHealth());
        int maxHealth = Mth.ceil(recruit.getMaxHealth());
        int moral = Mth.ceil(recruit.getMoral());

        double A_damage = this.calculateADamage();
        double speed = recruit.getMovementSpeed();
        DecimalFormat decimalformat = new DecimalFormat("##.#");
        double armor = recruit.getArmorValue();
        int costs = recruit.getCost();
        double hunger = recruit.getHunger();

        int k = 30;//rechst links
        int l = 15;//höhe

        //Titles
        font.draw(matrixStack, recruit.getDisplayName().getVisualOrderText(), 8, 5, fontColor);
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, imageHeight - 148 + 18, FONT_COLOR);
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

        font.draw(matrixStack, "Hunger:", k, l + 50, fontColor);
        font.draw(matrixStack, ""+ decimalformat.format(hunger), k + 40, l + 50, fontColor);

        font.draw(matrixStack, "Upkeep Pos:", k, l + 60, fontColor);
        font.draw(matrixStack, ""+ recruit.getUpkeepPos(), k + 43 + 20, l + 60, fontColor);


        font.draw(matrixStack, "MaxHp:", k + 43 + 20, l, fontColor);
        font.draw(matrixStack, ""+ maxHealth, k + 77 + 20, l, fontColor);

        font.draw(matrixStack, "Attack:", k + 43 + 20, l + 10, fontColor);
        font.draw(matrixStack, ""+ A_damage, k + 77 + 20, l + 10, fontColor);

        font.draw(matrixStack, "Speed:", k +43 + 20, l + 20, fontColor);
        font.draw(matrixStack, ""+ decimalformat.format(speed), k + 77 + 20, l + 20, fontColor);

        font.draw(matrixStack, "Armor:", k + 43 + 20, l + 30, fontColor);
        font.draw(matrixStack, ""+ armor, k + 77 + 20, l + 30, fontColor);

        font.draw(matrixStack, "Cost:", k + 43 + 20, l + 40, fontColor);
        font.draw(matrixStack, ""+ costs, k + 77 + 20, l + 40, fontColor);
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
