package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.entities.AssassinLeaderEntity;
import com.talhanation.recruits.inventory.AssassinLeaderMenu;
import com.talhanation.recruits.network.MessageAssassinCount;
import com.talhanation.recruits.network.MessageAssassinate;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.awt.*;


@OnlyIn(Dist.CLIENT)
public class AssassinLeaderScreen extends ScreenBase<AssassinLeaderMenu> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/assassin_gui.png");

    private static final MutableComponent TEXT_HEALTH = Component.literal("gui.recruits.inv.health");
    private static final MutableComponent TEXT_LEVEL = Component.literal("gui.recruits.inv.level");
    private static final MutableComponent TEXT_GROUP = Component.literal("gui.recruits.inv.group");
    private static final MutableComponent TEXT_KILLS = Component.literal("gui.recruits.inv.kills");

    private static final int fontColor = 4210752;

    private final Inventory playerInventory;
    private final AssassinLeaderEntity assassinLeaderEntity;
    private EditBox textField;

    private int count;

    public AssassinLeaderScreen(AssassinLeaderMenu container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
        this.playerInventory = playerInventory;
        this.assassinLeaderEntity = container.getEntity();
        this.count = assassinLeaderEntity.getCount();
        imageWidth = 176;
        imageHeight = 218;
    }

    @Override
    protected void init() {
        super.init();
        /*
        addRenderableWidget(new Button(leftPos + 10, topPos + 60, 8, 12, Component.literal("<"), button -> {
            this.count = assassinLeaderEntity.getCount();
            if (this.count != 0) {
                this.count--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAssassinCount(this.count, assassinLeaderEntity.getUUID()));
            }
        }));

        addRenderableWidget(new Button(leftPos + 10 + 30, topPos + 60, 8, 12, Component.literal(">"), button -> {
            this.count = assassinLeaderEntity.getCount();
            if (this.count != assassinLeaderEntity.getMaxAssassinCount()) {
                this.count++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAssassinCount(this.count, assassinLeaderEntity.getUUID()));
            }
        }));

        //HUNT
        addRenderableWidget(new Button(leftPos + 77 + 25, topPos + 4, 50, 12, Component.literal("Assassinate"), button -> {
            int assassinateCost = assassinLeaderEntity.calculateAssassinateCosts(assassinLeaderEntity.getAssassinCosts(), this.count);
            //if(AssassinEvents.playerHasEnoughEmeralds(playerInventory.player, assassinateCost))
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAssassinate(textField.getValue(), this.count, assassinateCost));
            //else
                playerInventory.player.sendSystemMessage(Component.literal(assassinLeaderEntity.getName() + ": You dont have enough Emeralds"));
        onClose();
        }));

        textField = new EditBox(font, leftPos + 30, topPos + 30, 116, 16, Component.literal(("")));
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);

        addRenderableOnly(textField);
        setInitialFocus(textField);

         */
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        /*
        int k = 79;//rechst links
        int l = 19;//höhe
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor);
        String count = String.valueOf(assassinLeaderEntity.getCount());
        font.draw(matrixStack, "Assassin Count:", k - 70, l + 35, fontColor);
        font.draw(matrixStack, count, k - 55, l + 45, fontColor);
     */
    }


    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTicks, mouseX, mouseY);

    }


    @Override
    public boolean keyPressed(int key, int a, int b) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.player.closeContainer();
            return true;
        }

        return textField.keyPressed(key, a, b) || textField.canConsumeInput() || super.keyPressed(key, a, b);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;

    }
}
