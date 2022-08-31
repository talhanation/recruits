package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.TeamCreationContainer;
import com.talhanation.recruits.network.MessageOpenTeamCreationScreen;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BannerItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;


@OnlyIn(Dist.CLIENT)
public class TeamCreationScreen extends ScreenBase<TeamCreationContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/assassin_gui.png");

    //private static final Component TEXT_NOT_ENOUGH_EMERALDS = new TranslatableComponent("gui.recruits.teamcreation.not_enough");


    private static final int fontColor = 4210752;

    private final Inventory playerInventory;
    private EditBox textField;
    private BannerItem banner;

    public TeamCreationScreen(TeamCreationContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, new TextComponent(""));
        this.playerInventory = playerInventory;
        this.banner = null;
        imageWidth = 176;
        imageHeight = 218;
    }

    @Override
    protected void init() {
        super.init();
        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        Main.LOGGER.debug("Hello from Screen");
        addRenderableOnly(new Button(leftPos + 77 + 25, topPos + 4, 50, 12, new TextComponent("Create"),
                button -> {
                    Main.LOGGER.debug("Hello from Button");
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenTeamCreationScreen(playerInventory.player));
                    this.onClose();
                }, (a, b, c, d) -> {

        }));


        textField = new EditBox(font, leftPos + 30, topPos + 30, 116, 16, new TextComponent(""));
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);

        addRenderableOnly(textField);
        setInitialFocus(textField);
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);

        int k = 79;//rechst links
        int l = 19;//höhe
        font.draw(matrixStack, playerInventory.getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor);
    }

    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);

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
