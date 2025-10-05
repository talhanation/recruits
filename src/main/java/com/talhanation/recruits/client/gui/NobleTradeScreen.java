package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.config.RecruitsServerConfig;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.init.ModEntityTypes;
import com.talhanation.recruits.network.MessageHireFromNobleVillager;
import com.talhanation.recruits.world.RecruitsHireTrade;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;

import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.talhanation.recruits.client.ClientManager.currency;

public class NobleTradeScreen extends RecruitsScreenBase {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/noble_villager.png");
    private static final Component TITLE = Component.translatable("gui.recruits.villager_noble");
    private static final Component HIRE_BUTTON = Component.translatable("gui.recruits.villager_noble.hire");
    private static final Component ERROR_NO_VILLAGERS = Component.translatable("gui.recruits.villager_noble.error_no_villagers");
    private static final Component ERROR_NO_USES = Component.translatable("gui.recruits.villager_noble.error_no_uses");
    private static final Component ERROR_NOT_ENOUGH_CURRENCY = Component.translatable("gui.recruits.villager_noble.error_not_enough_currency");
    private final Player player;
    private final VillagerNobleEntity villagerNoble;

    private TradeList tradeList;
    private RecruitsHireTrade selection;
    private Button hireButton;
    private List<Villager> villagerList;

    // layout constants (fits your base size 256x166)
    private static final int LIST_X = 5;
    private static final int LIST_Y = 18;
    private static final int LIST_W = 85;
    private static final int LIST_H = 170; // fits inside ySize (160) minus title & margins
    private static final int DETAIL_X = 90;
    private static final int DETAIL_Y = 64;
    private static final int DETAIL_W = 100;
    private static final int DETAIL_H = 112;

    public NobleTradeScreen(VillagerNobleEntity villagerNoble, Player player) {
        super(TITLE, 256,200);
        this.player = player;
        this.villagerNoble = villagerNoble;
    }

    @Override
    protected void init() {
        super.init();
        this.findVillagers();

        if (this.tradeList != null) {
            this.removeWidget(this.tradeList);
        }

        int listLeft = guiLeft + LIST_X;
        int listTop = guiTop + LIST_Y;
        int listBottom = listTop + LIST_H;
        int listWidth = LIST_W;
        int listHeight = LIST_H; // 'height' param der Konstruktion
        int itemHeight = 40;

        this.tradeList = new TradeList(Minecraft.getInstance(), listWidth, listHeight, listTop, listBottom, itemHeight);
        this.tradeList.setRenderBackground(false);
        this.tradeList.setRenderTopAndBottom(false);

        this.tradeList.setLeftPos(listLeft);
        this.tradeList.setRenderSelection(false);


        this.tradeList.clearEntries();
        List<RecruitsHireTrade> trades = villagerNoble.getTrades();
        for (RecruitsHireTrade trade : trades) {
            this.tradeList.addEntry(this.tradeList.new TradeEntry(trade));
        }

        this.addRenderableWidget(this.tradeList);


        this.hireButton = this.addRenderableWidget(new ExtendedButton(guiLeft + 123, guiTop + ySize - 28, 100, 20, HIRE_BUTTON,
                btn -> {
                    if (selection != null && villagerList != null && !villagerList.isEmpty()) {
                        Villager villager = villagerList.get(0);
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageHireFromNobleVillager(villagerNoble.getUUID(), villager.getUUID(), selection));

                        this.selection = null;
                        this.tradeList.setSelected(null);
                        this.updateHireButtonState();
                    }
                }
        ));
        this.hireButton.active = false;
        updateHireButtonState();
    }


    @Override
    public void tick() {
        super.tick();
        if(this.player.tickCount % 20 == 0) this.findVillagers();
    }

    private void findVillagers() {
        this.villagerList = this.player.getCommandSenderWorld()
                .getEntitiesOfClass(Villager.class, this.player.getBoundingBox().inflate(32))
                .stream()
                .filter(v -> v.getVillagerData().getProfession().equals(VillagerProfession.NONE))
                .sorted(Comparator.comparing(v -> v.distanceTo(this.player)))
                .collect(Collectors.toList());

        this.updateHireButtonState();
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
        guiGraphics.drawString(font, TITLE, guiLeft + 10, guiTop + 7, 0x404040, false);
        // draw selected trade details
        if (selection != null) {
            int x = guiLeft + DETAIL_X + 4;
            int y = guiTop + DETAIL_Y + 4;
            guiGraphics.drawString(font, Component.literal("Recruit: " + selection.recruitType.toString()), x, y, 0x000000, false);
            y += 12;
            guiGraphics.drawString(font, Component.literal("Cost: " + selection.cost), x, y, 0x000000, false);
            y += 12;
            guiGraphics.drawString(font, Component.literal("MinLevel: " + selection.minLevel), x, y, 0x000000, false);

            // optionally draw extra info like uses/maxUses when you add those fields
        } else {
            int x = guiLeft + DETAIL_X + 4;
            int y = guiTop + DETAIL_Y + 4;
            guiGraphics.drawString(font, Component.literal("No selection"), x, y, 0x666666, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void updateHireButtonState() {
        if (this.hireButton == null) return;

        this.hireButton.setTooltip(getErrorMessage());

        if (this.selection == null) {
            this.hireButton.active = false;
            return;
        }

        int playerMoney = getPlayerCurrencyAmount();
        this.hireButton.active = (playerMoney >= this.selection.cost || player.isCreative()) && selection.uses > 0 && !villagerList.isEmpty();
    }

    private int getPlayerCurrencyAmount(){
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(currency.getItem())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /* ===================== List Widget ===================== */
    private class TradeList extends ObjectSelectionList<TradeList.TradeEntry> {
        public TradeList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
            super(mc, width, height, top, bottom, itemHeight);
        }

        @Override
        protected int addEntry(TradeEntry p_93487_) {
            return super.addEntry(p_93487_);
        }

        @Override
        protected void clearEntries() {
            super.clearEntries();
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getRowLeft() + this.getRowWidth() - 6;
        }

        @Override
        public int getRowLeft() {
            return super.getRowLeft();
        }

        @Override
        public int getRowWidth() {
            return LIST_W;
        }

        public class TradeEntry extends ObjectSelectionList.Entry<TradeEntry> {
            private final RecruitsHireTrade trade;
            public TradeEntry(RecruitsHireTrade trade) { this.trade = trade; }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
                int rowLeft = TradeList.this.getRowLeft();
                int rowWidth = TradeList.this.getRowWidth();
                int x = rowLeft + 4;
                int y = top + 3;

                boolean selected = (TradeList.this.getSelected() == this);
                int textureY = getButtonTextureY(hovered, selected);

                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                guiGraphics.blitNineSliced(AbstractButton.WIDGETS_LOCATION, rowLeft, top, rowWidth, entryHeight, 20, 4, 200, 20, 0, textureY);
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

                // Text mit Schatten, damit er immer sichtbar ist
                guiGraphics.drawString(font, Component.literal(trade.recruitType.getPath()), x, y, 0x101010, false);
                guiGraphics.drawString(font, Component.literal("Trades " + trade.uses), x, y + 10, 0x505050, false);
                guiGraphics.drawString(font, Component.literal(String.valueOf(trade.cost)), rowLeft + rowWidth - 20, y, 0x101010, false);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                TradeList.this.setSelected(this);
                NobleTradeScreen.this.selection = this.trade;
                NobleTradeScreen.this.updateHireButtonState();
                return true;
            }

            @Override
            public Component getNarration() { return Component.empty(); }
            private int getButtonTextureY(boolean hovered, boolean selected) {
                final int BUTTON_Y_NORMAL = 66;
                final int BUTTON_Y_HOVER = 86;
                final int BUTTON_Y_PRESSED = 86;

                if (selected) return BUTTON_Y_PRESSED;
                if (hovered) return BUTTON_Y_HOVER;
                return BUTTON_Y_NORMAL;
            }
        }
    }

    private Tooltip getErrorMessage() {
        List<HireError> errors = this.getErrors();

        if (errors.isEmpty()) {
            return Tooltip.create(Component.empty());
        }

        MutableComponent tooltip = Component.literal("");
        for (int i = 0; i < errors.size(); i++) {
            tooltip.append(Component.literal("- ").append(errors.get(i).getMessage()));
            if (i < errors.size() - 1) {
                tooltip.append(Component.literal("\n"));
            }
        }
        return Tooltip.create(tooltip);
    }

    private List<HireError> getErrors() {
        List<HireError> errorList = new ArrayList<>();

        if(villagerList != null && villagerList.isEmpty()){
            errorList.add(HireError.NO_VILLAGER_NEARBY);
        }
        else if(selection != null){
            int playerMoney = getPlayerCurrencyAmount();
            if(selection.cost > playerMoney) errorList.add(HireError.NOT_ENOUGH_CURRENCY);
            if(selection.uses <= 0) errorList.add(HireError.NO_USES);
        }

        return errorList;
    }

    public enum HireError{
        NOT_ENOUGH_CURRENCY(ERROR_NOT_ENOUGH_CURRENCY),
        NO_VILLAGER_NEARBY(ERROR_NO_VILLAGERS),
        NO_USES(ERROR_NO_USES);
        private final Component message;

        HireError(Component message) {
            this.message = message;
        }

        public Component getMessage() {
            return message;
        }
    }
}

