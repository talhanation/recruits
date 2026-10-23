package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.network.MessageNobleQuestAction;
import com.talhanation.recruits.world.NobleQuest;
import com.talhanation.recruits.world.NobleQuestRegistry;
import com.talhanation.recruits.world.NobleReputation;
import com.talhanation.recruits.world.NobleVillageInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;

/**
 * Die 3 Angebote des Tages und der eigene aktive Auftrag.
 */
@OnlyIn(Dist.CLIENT)
public class NobleQuestScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = Component.translatable("gui.recruits.noble.quests.title");
    private static final Component BUTTON_ACCEPT = Component.translatable("gui.recruits.noble.quest.accept");
    private static final Component BUTTON_TAKEN = Component.literal("✔");
    private static final Component BUTTON_TURN_IN = Component.translatable("gui.recruits.noble.quest.turn_in");
    private static final Component BUTTON_ABANDON = Component.translatable("gui.recruits.noble.quest.abandon");
    private static final Component BUTTON_BACK = Component.translatable("gui.recruits.noble.button.back");
    private static final Component TEXT_ACTIVE = Component.translatable("gui.recruits.noble.quest.active");
    private static final Component TEXT_NONE = Component.translatable("gui.recruits.noble.quest.none");
    private static final Component TEXT_NO_OFFERS = Component.translatable("gui.recruits.noble.quest.no_offers");
    private static final Component TEXT_OTHER_VILLAGE = Component.translatable("gui.recruits.noble.quest.other_village");
    private static final Component TEXT_DONE = Component.translatable("gui.recruits.noble.quest.done");

    private static final int LEFT = 8;
    private static final int ROW_Y = 20;
    private static final int ROW_H = 18;
    private static final int ACCEPT_W = 52;

    @Nullable
    private NobleVillageInfo shownInfo;
    private Button turnInButton;
    private Button abandonButton;

    public NobleQuestScreen() {
        super(TITLE, 195, 160);
    }

    @Override
    protected void init() {
        super.init();
        NobleVillageInfo info = ClientManager.nobleInfo;
        this.shownInfo = info;
        if (info == null) {
            this.onClose();
            return;
        }

        boolean canAccept = !info.isHostile() && info.activeQuest == null;
        for (int i = 0; i < info.offers.size() && i < NobleQuestRegistry.OFFERS_PER_DAY; i++) {
            NobleQuest offer = info.offers.get(i);
            boolean taken = i < info.offerTaken.size() && info.offerTaken.get(i);

            Button accept = this.addRenderableWidget(new ExtendedButton(
                    guiLeft + xSize - LEFT - ACCEPT_W, guiTop + ROW_Y + i * ROW_H - 4, ACCEPT_W, 16,
                    taken ? BUTTON_TAKEN : BUTTON_ACCEPT,
                    button -> Main.SIMPLE_CHANNEL.sendToServer(MessageNobleQuestAction.accept(info.nobleId, offer.id))));
            accept.active = canAccept && !taken;
            accept.setTooltip(Tooltip.create(Component.translatable("gui.recruits.noble.quest.reward",
                    NobleReputation.format(offer.rewardReputation), NobleQuestRegistry.REWARD_CURRENCY)));
        }

        this.addRenderableWidget(new ExtendedButton(guiLeft + LEFT, guiTop + 134, 55, 18, BUTTON_BACK,
                button -> {
                    if (this.minecraft != null) this.minecraft.setScreen(new NobleScreen());
                }));

        turnInButton = this.addRenderableWidget(new ExtendedButton(guiLeft + 68, guiTop + 134, 60, 18, BUTTON_TURN_IN,
                button -> Main.SIMPLE_CHANNEL.sendToServer(MessageNobleQuestAction.turnIn(info.nobleId))));

        abandonButton = this.addRenderableWidget(new ExtendedButton(guiLeft + 132, guiTop + 134, 55, 18, BUTTON_ABANDON,
                button -> Main.SIMPLE_CHANNEL.sendToServer(MessageNobleQuestAction.abandon(info.nobleId))));

        updateActiveButtons(info);
    }

    @Override
    public void tick() {
        super.tick();
        // Neue Info vom Server (nach Annehmen/Abgeben) → Knöpfe neu aufbauen.
        if (ClientManager.nobleInfo != shownInfo && this.minecraft != null) {
            this.init(this.minecraft, this.width, this.height);
            return;
        }
        if (shownInfo != null) updateActiveButtons(shownInfo);
    }

    private void updateActiveButtons(NobleVillageInfo info) {
        if (turnInButton == null || abandonButton == null) return;
        NobleQuest active = info.activeQuest;
        abandonButton.active = active != null;
        turnInButton.active = active != null && info.activeQuestHere && !info.isHostile() && isReadyToTurnIn(info);
    }

    private boolean isReadyToTurnIn(NobleVillageInfo info) {
        NobleQuest active = info.activeQuest;
        if (active == null) return false;
        if (info.activeCompleted) return true;
        if (active.type != NobleQuest.Type.DELIVERY) return false;
        Player player = Minecraft.getInstance().player;
        return player != null && active.countDeliveryItems(player) >= active.count;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        guiGraphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.drawString(font, TITLE, guiLeft + xSize / 2 - font.width(TITLE) / 2, guiTop + 7, FONT_COLOR, false);

        NobleVillageInfo info = shownInfo;
        if (info == null) return;

        int textWidth = xSize - 2 * LEFT - ACCEPT_W - 4;
        if (info.offers.isEmpty()) {
            guiGraphics.drawString(font, TEXT_NO_OFFERS, guiLeft + LEFT, guiTop + ROW_Y, FONT_COLOR, false);
        }
        for (int i = 0; i < info.offers.size() && i < NobleQuestRegistry.OFFERS_PER_DAY; i++) {
            String text = font.plainSubstrByWidth(info.offers.get(i).getDescription().getString(), textWidth);
            guiGraphics.drawString(font, text, guiLeft + LEFT, guiTop + ROW_Y + i * ROW_H, FONT_COLOR, false);
        }

        int y = guiTop + 78;
        guiGraphics.fill(guiLeft + LEFT, y, guiLeft + xSize - LEFT, y + 1, 0xFF8B8B8B);

        guiGraphics.drawString(font, TEXT_ACTIVE, guiLeft + LEFT, y + 6, FONT_COLOR, false);

        NobleQuest active = info.activeQuest;
        if (active == null) {
            guiGraphics.drawString(font, TEXT_NONE, guiLeft + LEFT, y + 20, FONT_COLOR, false);
            return;
        }

        int fullWidth = xSize - 2 * LEFT;
        String description = font.plainSubstrByWidth(active.getDescription().getString(), fullWidth);
        guiGraphics.drawString(font, description, guiLeft + LEFT, y + 20, FONT_COLOR, false);

        Component status;
        if (!info.activeQuestHere) {
            status = TEXT_OTHER_VILLAGE.copy().withStyle(ChatFormatting.DARK_RED);
        } else if (info.activeCompleted) {
            status = TEXT_DONE.copy().withStyle(ChatFormatting.DARK_GREEN);
        } else if (active.type == NobleQuest.Type.DELIVERY) {
            Player player = Minecraft.getInstance().player;
            int have = player != null ? active.countDeliveryItems(player) : 0;
            status = Component.translatable("gui.recruits.noble.quest.inventory", Math.min(have, active.count), active.count);
        } else {
            status = Component.translatable("gui.recruits.noble.quest.progress", info.activeProgress, active.count);
        }
        guiGraphics.drawString(font, status, guiLeft + LEFT, y + 32, FONT_COLOR, false);

        long minutes = Math.max(0L, info.activeTicksLeft) / 1200L;
        Component timeLeft = Component.translatable("gui.recruits.noble.quest.time_left", minutes);
        guiGraphics.drawString(font, timeLeft, guiLeft + xSize - LEFT - font.width(timeLeft), guiTop + 78 + 6, FONT_COLOR, false);
    }
}
