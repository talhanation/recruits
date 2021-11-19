package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.CommandEvents;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.inventory.CommandContainer;
import com.talhanation.recruits.network.MessageAttack;
import com.talhanation.recruits.network.MessageFollow;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
public class CommandScreen extends ScreenBase<CommandContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID,"textures/gui/command_gui.png");
    private static final int fontColor = 4210752;
    private PlayerEntity player;
    private int group;

    public CommandScreen(CommandContainer commandContainer, PlayerInventory playerInventory, ITextComponent title) {
        super(RESOURCE_LOCATION, commandContainer, playerInventory, title);
        imageWidth = 201;
        imageHeight = 170;
        group = 0;
        player = playerInventory.player;
    }

    @Override
    public boolean keyReleased(int x, int y, int z) {
        super.keyReleased(x,y,z);
        this.onClose();
        return true;
    }

    @Override
    protected void init() {
        super.init();

        //FOLLOW
        addButton(new Button(leftPos - 40 + imageWidth / 2, topPos + 10, 81, 20, new StringTextComponent("Follow me!"), button -> {
            CommandEvents.sendFollowCommandInChat(1, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 1, 0));

        }));

        //HOLDPOS
        addButton(new Button(leftPos + 30 + imageWidth / 2, topPos + 20 + 30, 81, 20, new StringTextComponent("Hold your Pos.!"), button -> {
            CommandEvents.sendFollowCommandInChat(2, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 2, 0));
        }));

        //WANDER
        addButton(new Button(leftPos - 70 - 40 + imageWidth / 2, topPos + 20 + 30, 81, 20, new StringTextComponent("Release!"), button -> {
            CommandEvents.sendFollowCommandInChat(0, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageFollow(player.getUUID(), 0, 0));
        }));



        //NEUTRAl
        addButton(new Button(leftPos - 40 + imageWidth / 2, topPos + 120, 81, 20, new StringTextComponent("Stay Neutral!"), button -> {
            CommandEvents.sendAggroCommandInChat(0, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(player.getUUID(), 0, 0));
        }));

        //AGGRESSIVE
        addButton(new Button(leftPos - 40 - 70 + imageWidth / 2, topPos + 20 + 30 + 30, 81, 20, new StringTextComponent("Stray Aggressive!"), button -> {
            CommandEvents.sendAggroCommandInChat(1, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(player.getUUID(), 1, 0));
        }));

        //RAID
        addButton(new Button(leftPos + 30 + imageWidth / 2, topPos + 20 + 30 + 30, 81, 20, new StringTextComponent("Raid!"), button -> {
            CommandEvents.sendAggroCommandInChat(2, player);
            Main.SIMPLE_CHANNEL.sendToServer(new MessageAttack(player.getUUID(), 2, 0));
        }));



        addButton(new Button(leftPos - 5 + imageWidth / 2, topPos - 40 + imageHeight / 2, 11, 20, new StringTextComponent("+"), button -> {
            if (this.group != 9) {
                this.group ++;
            }
        }));

        addButton(new Button(leftPos - 5 + imageWidth / 2, topPos + imageHeight / 2, 11, 20, new StringTextComponent("-"), button -> {
            if (this.group != 0) {
                this.group --;
            }
        }));
    }

    @Override
    protected void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
        font.draw(matrixStack, "" +  handleGroupText(this.group), leftPos, topPos, fontColor);
    }

    protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(matrixStack, partialTicks, mouseX, mouseY);

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }


    private String handleGroupText(int group){
        if (group == 0){
            return "Everyone";
        }
        else
            return String.valueOf(group);
    }

}
