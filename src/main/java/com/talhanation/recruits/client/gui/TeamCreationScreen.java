package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.talhanation.recruits.AssassinEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.TeamCreationContainer;
import com.talhanation.recruits.network.MessageCreateTeam;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;


@OnlyIn(Dist.CLIENT)
public class TeamCreationScreen extends ScreenBase<TeamCreationContainer> {
    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/assassin_gui.png");

    //private static final Component TEXT_NOT_ENOUGH_EMERALDS = new TranslatableComponent("gui.recruits.teamcreation.not_enough");


    private static final int fontColor = 4210752;
    private TeamCreationContainer container;
    private final Inventory playerInventory;
    private EditBox textField;
    private ItemStack banner;

    public TeamCreationScreen(TeamCreationContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, new TextComponent(""));
        this.playerInventory = playerInventory;
        this.container = container;
        this.banner = null;
        imageWidth = 176;
        imageHeight = 218;
    }

    @Override
    protected void init() {
        super.init();

        minecraft.keyboardHandler.setSendRepeatsToGui(true);
        Main.LOGGER.debug("Hello from Screen");
        String create = "create";
        if(playerInventory.player.getTeam() == null) {
            addRenderableWidget(new Button(leftPos + 30, topPos + 50, 80, 20, new TextComponent(create),
                button -> {
                    this.banner = container.getBanner();
                    if (!banner.equals(ItemStack.EMPTY)) {
                        CompoundTag compoundtag = banner.serializeNBT();
                        Main.LOGGER.debug(compoundtag);
                        if(compoundtag != null){
                            int creationCost = 10;
                            if (AssassinEvents.playerHasEnoughEmeralds(playerInventory.player, creationCost)) {
                                if (!textField.getValue().equals("")) {
                                    Main.SIMPLE_CHANNEL.sendToServer(new MessageCreateTeam(textField.getValue(), creationCost, banner));
                                } else
                                    playerInventory.player.sendMessage(new TranslatableComponent("chat.recruits.team_creation.noname"), playerInventory.player.getUUID());

                            } else
                                playerInventory.player.sendMessage(new TranslatableComponent("chat.recruits.team_creation.noenoughMoney"), playerInventory.player.getUUID());

                            this.onClose();
                        }
                        else
                            playerInventory.player.sendMessage(new TranslatableComponent("chat.recruits.team_creation.wrongbanner"), playerInventory.player.getUUID());
                    }
                    else
                        playerInventory.player.sendMessage(new TranslatableComponent("chat.recruits.team_creation.nobanner"), playerInventory.player.getUUID());
                }
            ));
        }

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
        container.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;

    }
}
