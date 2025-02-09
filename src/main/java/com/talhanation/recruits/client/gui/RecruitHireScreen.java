package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.RecruitHireMenu;
import com.talhanation.recruits.network.MessageHire;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import java.text.DecimalFormat;

@OnlyIn(Dist.CLIENT)
public class RecruitHireScreen extends ScreenBase<RecruitHireMenu> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/hire_gui.png" );

    private static final MutableComponent TEXT_HIRE = Component.translatable("gui.recruits.hire_gui.text.hire");

    private static final int fontColor = 4210752;

    private final AbstractRecruitEntity recruit;
    private final Player player;
    private ExtendedButton hireButton;
    public static ItemStack currency;
    public static boolean canHire;

    public RecruitHireScreen(RecruitHireMenu recruitContainer, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, recruitContainer, playerInventory, Component.literal(""));
        this.recruit = recruitContainer.getRecruitEntity();
        this.player = playerInventory.player;
        imageWidth = 176;
        imageHeight = 223;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if(hireButton != null){
            hireButton.active = canHire;
        }
    }

    @Override
    protected void init() {
        super.init();

        if(currency != null) currency.setCount(recruit.getCost());
        hireButton = createHireButton();
    }

    private ExtendedButton createHireButton() {
        int zeroLeftPos = leftPos + 180;
        int zeroTopPos = topPos + 10;
        int mirror = 240 - 60;

        return addRenderableWidget(new ExtendedButton(zeroLeftPos - mirror + 40, zeroTopPos + 85, 100, 20, TEXT_HIRE,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageHire(player.getUUID(), recruit.getUUID()));
                    this.onClose();
        }));
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        int health = Mth.ceil(recruit.getHealth());
        int maxHealth = Mth.ceil(recruit.getMaxHealth());
        int moral = Mth.ceil(recruit.getMorale());

        double A_damage = Mth.ceil(recruit.getAttackDamage());
        double speed = recruit.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) / 0.3;
        DecimalFormat decimalformat = new DecimalFormat("##.##");
        double armor = recruit.getArmorValue();

        int k = 60;//rechst links
        int l = 19;//höhe

        guiGraphics.drawString(font, recruit.getDisplayName().getVisualOrderText(), 8, 5, fontColor, false);
        guiGraphics.drawString(font, player.getInventory().getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor, false);

        guiGraphics.drawString(font, "Hp:", k, l, fontColor, false);
        guiGraphics.drawString(font, "" + health, k + 25, l , fontColor, false);

        guiGraphics.drawString(font, "Lvl:", k , l  + 10, fontColor, false);
        guiGraphics.drawString(font, "" + recruit.getXpLevel(), k + 25 , l + 10, fontColor, false);

        guiGraphics.drawString(font, "Exp:", k, l + 20, fontColor, false);
        guiGraphics.drawString(font, "" + recruit.getXp(), k + 25, l + 20, fontColor, false);

        guiGraphics.drawString(font, "Kills:", k, l + 30, fontColor, false);
        guiGraphics.drawString(font, ""+ recruit.getKills(), k + 25, l + 30, fontColor, false);

        guiGraphics.drawString(font, "Morale:", k, l + 40, fontColor, false);
        guiGraphics.drawString(font, ""+ moral, k + 37, l + 40, fontColor, false);

        guiGraphics.drawString(font, "MaxHp:", k + 55, l, fontColor, false);
        guiGraphics.drawString(font, ""+ maxHealth, k + 90, l, fontColor, false);

        guiGraphics.drawString(font, "Attack:", k + 55, l + 10, fontColor, false);
        guiGraphics.drawString(font, ""+ A_damage, k + 90, l + 10, fontColor, false);

        guiGraphics.drawString(font, "Speed:", k + 55, l + 20, fontColor, false);
        guiGraphics.drawString(font, ""+ decimalformat.format(speed), k + 90, l + 20, fontColor, false);

        guiGraphics.drawString(font, "Armor:", k + 55, l + 30, fontColor, false);
        guiGraphics.drawString(font, ""+ armor, k + 90, l + 30, fontColor, false);

        if(currency != null){
            guiGraphics.renderFakeItem(currency, 120, this.imageHeight - 125);
            guiGraphics.renderItemDecorations(font, currency, 120, this.imageHeight - 125);
        }
    }

    protected void renderBg(GuiGraphics poseStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(poseStack, partialTicks, mouseX, mouseY);

        RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        InventoryScreen.renderEntityInInventoryFollowsMouse(poseStack,i + 30, j + 60, 15, (float)(i + 50) - mouseX, (float)(j + 75 - 50) - mouseY, this.recruit);
        if(recruit.getVehicle() instanceof AbstractHorse horse) InventoryScreen.renderEntityInInventoryFollowsMouse(poseStack, i + 30, j + 72, 15, (float)(i + 50) - mouseX, (float)(j + 75 - 50) - mouseY, horse);
    }

}
