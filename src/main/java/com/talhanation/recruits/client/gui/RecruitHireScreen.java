package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.RecruitHireMenu;
import com.talhanation.recruits.network.MessageHire;
import com.talhanation.recruits.world.RecruitsGroup;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
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
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/hire_gui.png");

    private static final MutableComponent TEXT_HIRE = Component.translatable("gui.recruits.hire_gui.text.hire");

    private static final int fontColor = 4210752;

    private final AbstractRecruitEntity recruit;
    private final Player player;
    private ExtendedButton hireButton;
    public static ItemStack currency;
    public static boolean canHire;

    private ScrollDropDownMenu<RecruitsGroup> groupSelectionDropDownMenu;
    public RecruitsGroup group;
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
        group = ClientManager.groups.get(ClientManager.groupSelection);
        groupSelectionDropDownMenu = new ScrollDropDownMenu<>(group, leftPos + 80 + 7 + 5,topPos + 100,  80, 20, ClientManager.groups,
                RecruitsGroup::getName,
                (selected) ->{
                    this.group = selected;
                    ClientManager.groupSelection = ClientManager.groups.indexOf(group);
                }
        );
        groupSelectionDropDownMenu.setBgFillSelected(FastColor.ARGB32.color(255, 139, 139, 139));
        addRenderableWidget(groupSelectionDropDownMenu);

        if(currency != null) currency.setCount(recruit.getCost());
        hireButton = createHireButton();
        if(group == null || ClientManager.groups.isEmpty()) hireButton.active = false;
    }

    private ExtendedButton createHireButton() {
        return addRenderableWidget(new ExtendedButton(leftPos + 7, topPos + 100, 80, 20, TEXT_HIRE,
                button -> {
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageHire(player.getUUID(), recruit.getUUID(), group.getUUID()));
                    this.onClose();
        }));
    }

    @Override
    public void mouseMoved(double x, double y) {
        if(groupSelectionDropDownMenu != null){
            groupSelectionDropDownMenu.onMouseMove(x,y);
        }
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (groupSelectionDropDownMenu != null && groupSelectionDropDownMenu.isMouseOver(mouseX, mouseY)) {
            groupSelectionDropDownMenu.onMouseClick(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseScrolled(double x, double y, double d) {
        if(groupSelectionDropDownMenu != null) groupSelectionDropDownMenu.mouseScrolled(x,y,d);
        return super.mouseScrolled(x, y, d);
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
            guiGraphics.renderFakeItem(currency, 70, this.imageHeight - 122);
            guiGraphics.renderItemDecorations(font, currency, 70, this.imageHeight - 122);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (groupSelectionDropDownMenu != null) {
            groupSelectionDropDownMenu.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);

        RenderSystem.clearColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics,i + 30, j + 60, 15, (float)(i + 50) - mouseX, (float)(j + 75 - 50) - mouseY, this.recruit);
        if(recruit.getVehicle() instanceof AbstractHorse horse) InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, i + 30, j + 72, 15, (float)(i + 50) - mouseX, (float)(j + 75 - 50) - mouseY, horse);
    }

}
