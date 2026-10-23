package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.network.MessageOpenNobleTrade;
import com.talhanation.recruits.world.NobleReputation;
import com.talhanation.recruits.world.NobleVillageInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;

/**
 * Hauptscreen des Nobles: Ruf, Dorf, „Anheuern“ und „Aufträge“.
 */
@OnlyIn(Dist.CLIENT)
public class NobleScreen extends RecruitsScreenBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/gui_big.png");
    private static final Component TITLE = Component.translatable("gui.recruits.villager_noble");
    private static final Component BUTTON_HIRE = Component.translatable("gui.recruits.noble.button.hire");
    private static final Component BUTTON_QUESTS = Component.translatable("gui.recruits.noble.button.quests");
    private static final Component TEXT_PLAYER = Component.translatable("gui.recruits.noble.reputation.player");
    private static final Component TEXT_FACTION = Component.translatable("gui.recruits.noble.reputation.faction");
    private static final Component TEXT_EFFECTIVE = Component.translatable("gui.recruits.noble.reputation.effective");
    private static final Component TEXT_NO_VILLAGE = Component.translatable("gui.recruits.noble.no_village");
    private static final Component TEXT_HOSTILE = Component.translatable("gui.recruits.noble.hostile");
    private static final Component TEXT_REPUTATION_DISABLED = Component.translatable("gui.recruits.noble.reputation.disabled");

    private static final int LEFT = 12;
    private static final int RIGHT = 183;
    private static final int BAR_Y = 80;
    private static final int BAR_W = RIGHT - LEFT;
    private static final int BAR_H = 8;

    private Button hireButton;
    private Button questButton;

    public NobleScreen() {
        super(TITLE, 195, 160);
    }

    @Nullable
    private NobleVillageInfo info() {
        return ClientManager.nobleInfo;
    }

    @Override
    protected void init() {
        super.init();
        NobleVillageInfo info = info();
        if (info == null) {
            this.onClose();
            return;
        }

        hireButton = this.addRenderableWidget(new ExtendedButton(guiLeft + LEFT, guiTop + 130, 82, 20, BUTTON_HIRE,
                button -> Main.SIMPLE_CHANNEL.sendToServer(new MessageOpenNobleTrade(info.nobleId))));

        questButton = this.addRenderableWidget(new ExtendedButton(guiLeft + RIGHT - 82, guiTop + 130, 82, 20, BUTTON_QUESTS,
                button -> {
                    if (this.minecraft != null) this.minecraft.setScreen(new NobleQuestScreen());
                }));

        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
    }

    private void updateButtons() {
        NobleVillageInfo info = info();
        if (info == null || hireButton == null || questButton == null) return;
        boolean hostile = info.isHostile();
        hireButton.active = !hostile;
        questButton.active = !hostile && info.hasVillage && info.questsActive;
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
        NobleVillageInfo info = info();
        if (info == null) return;

        Component title = getNobleName(info);
        guiGraphics.drawString(font, title, guiLeft + xSize / 2 - font.width(title) / 2, guiTop + 7, FONT_COLOR, false);

        if (!info.hasVillage) {
            drawLeft(guiGraphics, TEXT_NO_VILLAGE, 24);
            return;
        }

        drawLeft(guiGraphics, Component.translatable("gui.recruits.noble.inhabitants", info.inhabitants), 22);

        if (!info.reputationActive) {
            drawLeft(guiGraphics, TEXT_REPUTATION_DISABLED, 40);
            return;
        }

        // Ruf-Zeilen
        drawRow(guiGraphics, TEXT_PLAYER, Component.literal(NobleReputation.format(info.playerReputation)), 40);
        if (info.hasFaction) {
            String weights = info.isLeader ? "(50 / 50)" : "(75 / 25)";
            drawRow(guiGraphics, TEXT_FACTION, Component.literal(NobleReputation.format(info.factionReputation) + "  " + weights), 52);
        }

        NobleReputation.Tier tier = info.getTier();
        Component effective = Component.literal(NobleReputation.format(info.effectiveReputation) + "  ").append(tier.getName());
        drawRow(guiGraphics, TEXT_EFFECTIVE, effective, 64);

        renderBar(guiGraphics, info.effectiveReputation, tier);

        // Wirkung
        if (tier == NobleReputation.Tier.HOSTILE) {
            drawLeft(guiGraphics, TEXT_HOSTILE.copy().withStyle(ChatFormatting.DARK_RED), 96);
            return;
        }

        int percent = NobleReputation.getPricePercent(info.effectiveReputation);
        String percentText = (percent > 0 ? "+" : "") + percent + " %";
        drawLeft(guiGraphics, Component.translatable("gui.recruits.noble.prices", percentText, tier.getMaxTradeLevel()), 96);

        Component next = getNextTierHint(info.effectiveReputation);
        if (next != null) drawLeft(guiGraphics, next, 108);
    }

    @Nullable
    private Component getNextTierHint(int reputation) {
        NobleReputation.Tier tier = NobleReputation.getTier(reputation);
        return switch (tier) {
            case NEUTRAL -> Component.translatable("gui.recruits.noble.next_tier",
                    NobleReputation.RESPECTED_MIN - reputation, NobleReputation.Tier.RESPECTED.getName());
            case RESPECTED -> Component.translatable("gui.recruits.noble.next_tier",
                    NobleReputation.HONORED_MIN - reputation, NobleReputation.Tier.HONORED.getName());
            default -> null;
        };
    }

    private void renderBar(GuiGraphics guiGraphics, int reputation, NobleReputation.Tier tier) {
        int x0 = guiLeft + LEFT;
        int y0 = guiTop + BAR_Y;
        guiGraphics.fill(x0, y0, x0 + BAR_W, y0 + BAR_H, 0xFF555555);

        int filled = toBarX(reputation);
        guiGraphics.fill(x0, y0, x0 + filled, y0 + BAR_H, getTierColor(tier));

        // Stufenmarken bei −50, 25, 75
        for (int mark : new int[]{NobleReputation.HOSTILE_MAX, NobleReputation.RESPECTED_MIN, NobleReputation.HONORED_MIN}) {
            int mx = x0 + toBarX(mark);
            guiGraphics.fill(mx, y0 - 1, mx + 1, y0 + BAR_H + 1, 0xFF000000);
        }
    }

    private static int toBarX(int reputation) {
        int clamped = NobleReputation.clamp(reputation);
        return Math.round((clamped - NobleReputation.MIN) / (float) (NobleReputation.MAX - NobleReputation.MIN) * BAR_W);
    }

    private static int getTierColor(NobleReputation.Tier tier) {
        return switch (tier) {
            case HOSTILE -> 0xFFAA0000;
            case NEUTRAL -> 0xFFAAAAAA;
            case RESPECTED -> 0xFF55CC55;
            case HONORED -> 0xFFFFAA00;
        };
    }

    private void drawLeft(GuiGraphics guiGraphics, Component text, int y) {
        guiGraphics.drawString(font, text, guiLeft + LEFT, guiTop + y, FONT_COLOR, false);
    }

    private void drawRow(GuiGraphics guiGraphics, Component label, Component value, int y) {
        guiGraphics.drawString(font, label, guiLeft + LEFT, guiTop + y, FONT_COLOR, false);
        guiGraphics.drawString(font, value, guiLeft + RIGHT - font.width(value), guiTop + y, FONT_COLOR, false);
    }

    private Component getNobleName(NobleVillageInfo info) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return TITLE;
        return player.getCommandSenderWorld()
                .getEntitiesOfClass(VillagerNobleEntity.class, player.getBoundingBox().inflate(16.0D),
                        noble -> noble.getUUID().equals(info.nobleId))
                .stream()
                .findAny()
                .<Component>map(VillagerNobleEntity::getDisplayName)
                .orElse(TITLE);
    }
}
