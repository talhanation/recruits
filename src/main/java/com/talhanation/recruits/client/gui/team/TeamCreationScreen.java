package com.talhanation.recruits.client.gui.team;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.TeamCreationContainer;
import com.talhanation.recruits.network.MessageCreateTeam;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;


@OnlyIn(Dist.CLIENT)
public class TeamCreationScreen extends ScreenBase<TeamCreationContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/team/team_create_gui.png");

    private static final int fontColor = 4210752;
    private final TeamCreationContainer container;
    private final Inventory playerInventory;
    private EditBox textField;
    private ItemStack banner;
    private int color_index;
    private int colorId;
    private String color;
    private final ArrayList<String> COLORS = new ArrayList<>(
            Arrays.asList("white", "aqua", "black", "blue", "dark_aqua", "dark_blue", "dark_gray", "dark_green", "dark_purple", "dark_red", "gold", "green", "light_purple", "red", "yellow"));
    //ChatFormatting
    private final ArrayList<Integer> ColorID = new ArrayList<>(
            Arrays.asList(16777215, 5636095, 0, 5592575, 43690, 170, 5592405, 43520, 11141290, 11141120, 16755200, 5635925, 16733695, 16733525, 16777045));


    public TeamCreationScreen(TeamCreationContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
        this.playerInventory = playerInventory;
        this.container = container;
        this.banner = null;
        imageWidth = 176;
        imageHeight = 225;
    }

    @Override
    protected void init() {
        super.init();

        this.refreshSelectedColor();

        //Main.LOGGER.debug("Hello from Screen");
        String create = "create";
        if(playerInventory.player.getTeam() == null) {
            addRenderableWidget(new ExtendedButton(leftPos + 18, topPos + 80, 140, 20, Component.literal(create),
                button -> {
                    this.banner = container.getBanner();
                    if (!banner.equals(ItemStack.EMPTY)) {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageCreateTeam(this.getCorrectFormat(textField.getValue().strip()), banner, color));
                        this.onClose();
                    }
            }));

            cycleButtonLeft(leftPos + 60, topPos + 110);
            cycleButtonRight(leftPos + 60 + 80, topPos + 110);
        }

        textField = new EditBox(font, leftPos + 18, topPos + 50, 140, 20, Component.literal(""));
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setMaxLength(24);

        addRenderableOnly(textField);
        setInitialFocus(textField);
    }

    private Button cycleButtonLeft(int x, int y){
        return addRenderableWidget(new ExtendedButton(x, y, 13, 13, Component.literal("<"),
                button -> {
                    if(this.color_index > 0){
                        this.color_index--;
                        this.refreshSelectedColor();
                    }
                }
        ));
    }

    private Button cycleButtonRight(int x, int y){
        return addRenderableWidget(new ExtendedButton(x, y, 13, 13, Component.literal(">"),
                button -> {
                    if(this.color_index + 1 != COLORS.size()){
                        this.color_index++;
                        this.refreshSelectedColor();
                    }
                }
        ));
    }
    private void refreshSelectedColor() {
        this.color = COLORS.get(color_index);
        this.colorId = ColorID.get(color_index);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.drawString(font, "Create a Team:", 18, 11, fontColor, false);
        guiGraphics.drawString(font, playerInventory.getDisplayName().getVisualOrderText(), 8, this.imageHeight - 96 + 2, fontColor, false);
        guiGraphics.drawString(font, color, 75, this.imageHeight - 112, colorId, false);
        guiGraphics.drawString(font, "Color:", 18, this.imageHeight - 112, fontColor, false);

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
    
    private String getCorrectFormat(String input) {
        input = input.replaceAll(" ", "");
        input = input.replaceAll("[^a-zA-Z0-9\\s]+", "");

        return input;
    }

}
