package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsModConfig;
import com.talhanation.recruits.entities.AssassinLeaderEntity;
import com.talhanation.recruits.inventory.AssassinLeaderContainer;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;


@OnlyIn(Dist.CLIENT)
public class AssassinLeaderScreen extends ScreenBase<AssassinLeaderContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/assassin_gui.png");

    private static final ITextComponent TEXT_HEALTH = new TranslationTextComponent("gui.recruits.health");
    private static final ITextComponent TEXT_LEVEL = new TranslationTextComponent("gui.recruits.level");
    private static final ITextComponent TEXT_GROUP = new TranslationTextComponent("gui.recruits.group");
    private static final ITextComponent TEXT_KILLS = new TranslationTextComponent("gui.recruits.kills");

    private static final int fontColor = 4210752;

    private final PlayerInventory playerInventory;
    private final AssassinLeaderEntity assassinLeaderEntity;
    private TextFieldWidget textField;

    private int count;

    public AssassinLeaderScreen(AssassinLeaderContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(RESOURCE_LOCATION, container, playerInventory, title);
        this.playerInventory = playerInventory;
        this.assassinLeaderEntity = container.getEntity();
        this.count = assassinLeaderEntity.getCount();
        imageWidth = 176;
        imageHeight = 218;
    }

    @Override
    protected void init() {
        super.init();
        minecraft.keyboardHandler.setSendRepeatsToGui(true);

        addButton(new Button(leftPos + 10, topPos + 60, 8, 12, new StringTextComponent("<"), button -> {
            this.count = assassinLeaderEntity.getCount();
            if (this.count != 0) {
                this.count--;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAssassinCount(this.count, assassinLeaderEntity.getUUID()));
            }
        }));

        addButton(new Button(leftPos + 10 + 30, topPos + 60, 8, 12, new StringTextComponent(">"), button -> {
            this.count = assassinLeaderEntity.getCount();
            if (this.count != assassinLeaderEntity.getMaxAssassinCount()) {
                this.count++;
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAssassinCount(this.count, assassinLeaderEntity.getUUID()));
            }
        }));

        //HUNT
        addButton(new Button(leftPos + 77 + 25, topPos + 4, 50, 12, new StringTextComponent("Assassinate"), button -> {
            int assassinateCost = assassinLeaderEntity.calculateAssassinateCosts(assassinLeaderEntity.getAssassinCosts(), this.count);
            if(assassinLeaderEntity.playerHasEnoughEmeralds(playerInventory.player, assassinateCost))
                Main.SIMPLE_CHANNEL.sendToServer(new MessageAssassinate(textField.getValue(), this.count, assassinateCost));
            else
                playerInventory.player.sendMessage(new StringTextComponent(assassinLeaderEntity.getName() + ": You dont have enough Emeralds"), playerInventory.player.getUUID());
        onClose();
        }));

        textField = new TextFieldWidget(font, leftPos + 30, topPos + 30, 116, 16, new StringTextComponent(""));
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);

        addButton(textField);
        setInitialFocus(textField);
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);

        int k = 79;//rechst links
        int l = 19;//höhe
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor);
        String count = String.valueOf(assassinLeaderEntity.getCount());
        font.draw(matrixStack, "Assassin Count:", k - 70, l + 35, fontColor);
        font.draw(matrixStack, count, k - 55, l + 45, fontColor);
    }

    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        //InventoryScreen.renderEntityInInventory(i + 50, j + 82, 30, (float)(i + 50) - mouseX, (float)(j + 75 - 50) - mouseY, this.assassinLeaderEntity);
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
        minecraft.keyboardHandler.setSendRepeatsToGui(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
