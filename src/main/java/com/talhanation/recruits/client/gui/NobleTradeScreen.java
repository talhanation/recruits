package com.talhanation.recruits.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.RecruitsHireTradesRegistry;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.component.RecruitsMultiLineEditBox;
import com.talhanation.recruits.entities.VillagerNobleEntity;
import com.talhanation.recruits.network.MessageHireFromNobleVillager;
import com.talhanation.recruits.util.DelayedExecutor;
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

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.talhanation.recruits.client.ClientManager.currency;
import static com.talhanation.recruits.client.ClientManager.factionCreationPrice;

public class NobleTradeScreen extends RecruitsScreenBase {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Main.MOD_ID, "textures/gui/noble_villager.png");
    private static final Component TITLE = Component.translatable("gui.recruits.villager_noble");
    private static final Component HIRE_BUTTON = Component.translatable("gui.recruits.villager_noble.hire");
    private static final Component ERROR_NOT_ENOUGH_VILLAGERS = Component.translatable("gui.recruits.villager_noble.error_no_villagers");
    private static final Component ERROR_NO_USES = Component.translatable("gui.recruits.villager_noble.error_no_uses");
    private static final Component ERROR_NOT_ENOUGH_CURRENCY = Component.translatable("gui.recruits.villager_noble.error_not_enough_currency");
    private final Player player;
    private final VillagerNobleEntity villagerNoble;

    private TradeList tradeList;
    private RecruitsHireTrade selection;
    private Button hireButton;
    private List<Villager> villagerList;
    private RecruitsMultiLineEditBox descriptionBox;
    private Component description = Component.empty();
    private Component tradeTitle = Component.empty();

    private static final int LIST_X = 5;
    private static final int LIST_Y = 18;
    private static final int LIST_W = 85;
    private static final int LIST_H = 170;
    private static final int TRADE_TITLE_X = 98;
    private static final int TRADE_TITLE_Y = 58;
    private static final int LEVEL_BAR_X = 98;
    private static final int LEVEL_BAR_Y = 18;
    private static final int BAR_W = 150;
    private static final int BAR_H = 8;
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
        int listHeight = LIST_H;
        int itemHeight = 40;
        int itemWidth = LIST_W - 10;

        this.tradeList = new TradeList(Minecraft.getInstance(), listWidth, listHeight, listTop, listBottom, itemHeight, itemWidth);
        this.tradeList.setRenderBackground(false);
        this.tradeList.setRenderTopAndBottom(false);

        this.tradeList.setLeftPos(listLeft);
        this.tradeList.setRenderSelection(false);

        this.loadTrades();

        this.addRenderableWidget(this.tradeList);

        this.hireButton = this.addRenderableWidget(new ExtendedButton(guiLeft + 123, guiTop + ySize - 28, 100, 20, HIRE_BUTTON,
                btn -> {
                    if(ClientManager.configValueNobleNeedsVillagers){
                        if (selection != null && villagerList != null && !villagerList.isEmpty()) {
                            Villager villager = villagerList.get(0);
                            Main.SIMPLE_CHANNEL.sendToServer(new MessageHireFromNobleVillager(villagerNoble.getUUID(), villager.getUUID(), selection, true, false));

                            this.selection.uses -= 1;
                            this.updateHireButtonState();
                            this.loadTrades();
                        }
                    }
                    else {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageHireFromNobleVillager(villagerNoble.getUUID(), UUID.randomUUID(), selection, false, false));
                        this.selection.uses -= 1;
                        this.updateHireButtonState();
                        this.loadTrades();
                    }
                }
        ));
        this.hireButton.active = false;
        updateHireButtonState();
        this.descriptionBox = new RecruitsMultiLineEditBox(font, guiLeft + 98, guiTop + 73, 150, 97, Component.empty(), Component.empty());
        this.descriptionBox.setValue(description.getString());
        this.descriptionBox.setEnableEditing(false);
        this.descriptionBox.setScrollAmount(0);

        this.addRenderableWidget(this.descriptionBox);
    }


    @Override
    public void tick() {
        super.tick();
        if(this.player.tickCount % 20 == 0){
            this.findVillagers();
        }
        if(this.descriptionBox != null) descriptionBox.tick();
        this.loadTrades();
    }

    private void loadTrades(){
        this.tradeList.clearEntries();
        List<RecruitsHireTrade> trades = copyTrades(villagerNoble.getTrades());
        for (RecruitsHireTrade serverSideTrade : trades) {
            RecruitsHireTrade clientSideTrade = RecruitsHireTradesRegistry.getByResourceLocation(serverSideTrade.resourceLocation);
            if(clientSideTrade == null) continue;

            serverSideTrade.title = clientSideTrade.title;
            serverSideTrade.description = clientSideTrade.description;

            this.tradeList.addEntry(this.tradeList.new TradeEntry(serverSideTrade));
        }
    }

    private List<RecruitsHireTrade> copyTrades(List<RecruitsHireTrade> original) {
        if (original == null) return new ArrayList<>();
        List<RecruitsHireTrade> copy = new ArrayList<>();
        for (RecruitsHireTrade t : original) {
            RecruitsHireTrade clone = new RecruitsHireTrade(
                    t.resourceLocation,
                    t.cost,
                    t.minLevel,
                    t.chance,
                    t.title,
                    t.description,
                    t.tradeTagList == null ? null : new ArrayList<>(t.tradeTagList)
            );
            clone.uses = t.uses;
            clone.maxUses = t.maxUses;
            copy.add(clone);
        }
        return copy;
    }
    private void findVillagers() {
        this.villagerList = this.player.getCommandSenderWorld()
                .getEntitiesOfClass(Villager.class, this.player.getBoundingBox().inflate(32))
                .stream()
                .filter(v -> v.getVillagerData().getProfession().equals(VillagerProfession.NONE) && !v.isBaby())
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
        guiGraphics.drawString(font, TITLE, guiLeft + 10, guiTop + 7, FONT_COLOR, false);
        guiGraphics.drawString(font, Component.literal("Lvl: " + villagerNoble.getTraderLevel()), guiLeft + TRADE_TITLE_X, guiTop + 7, FONT_COLOR, false);

        if (selection != null) {
            int x = guiLeft + TRADE_TITLE_X ;
            int y = guiTop + TRADE_TITLE_Y;
            guiGraphics.drawString(font, tradeTitle, x, y, FONT_COLOR, false);
        }
        int progress = (int) (villagerNoble.getTraderProgress() * 1.5);
        guiGraphics.fill(guiLeft + LEVEL_BAR_X, guiTop + LEVEL_BAR_Y, guiLeft + LEVEL_BAR_X + BAR_W,  guiTop + LEVEL_BAR_Y + BAR_H, 0xFF555555);
        guiGraphics.fill(guiLeft + LEVEL_BAR_X, guiTop + LEVEL_BAR_Y, guiLeft + LEVEL_BAR_X + progress,  guiTop + LEVEL_BAR_Y + BAR_H, 0xFF00FF00);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        super.onClose();
        Main.SIMPLE_CHANNEL.sendToServer(new MessageHireFromNobleVillager(villagerNoble.getUUID(), UUID.randomUUID(), selection, false, true));
    }

    private void updateHireButtonState() {
        if (this.hireButton == null) return;

        this.hireButton.setTooltip(getErrorMessage());

        if (this.selection == null) {
            this.hireButton.active = false;
            return;
        }

        int playerMoney = getPlayerCurrencyAmount();

        this.hireButton.active = (playerMoney >= selection.cost || player.isCreative())
                && selection.uses > 0
                && (!ClientManager.configValueNobleNeedsVillagers || !villagerList.isEmpty());
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

    int xOffset = 50;
    int yOffset = 15;

    /* ===================== List Widget ===================== */
    private class TradeList extends ObjectSelectionList<TradeList.TradeEntry> {
        public int itemWidth;
        public TradeList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight, int itemWidth) {
            super(mc, width, height, top, bottom, itemHeight);
            this.itemWidth = itemWidth;
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
            return this.getRowLeft() + this.getRowWidth() + 5;
        }

        @Override
        public int getRowLeft() {
            return super.getRowLeft() - 7;
        }

        @Override
        public int getRowWidth() {
            return LIST_W - 12;
        }

        public void setSelected(@Nullable TradeEntry entry) {
            super.setSelected(entry);
            if(entry == null) return;
            NobleTradeScreen.this.onSelected(entry);
        }

        public class TradeEntry extends ObjectSelectionList.Entry<TradeEntry> {
            private final RecruitsHireTrade trade;
            public TradeEntry(RecruitsHireTrade trade) {
                this.trade = trade;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
                int rowLeft = TradeList.this.getRowLeft();
                int rowWidth = TradeList.this.itemWidth;
                int x = rowLeft + 4;
                int y = top + 3;


                boolean selected = (TradeList.this.getSelected() == this);
                boolean out = trade.uses <= 0;
                int textureY = getButtonTextureY(hovered, selected, out);

                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableBlend();
                RenderSystem.enableDepthTest();
                guiGraphics.blitNineSliced(AbstractButton.WIDGETS_LOCATION, rowLeft, top, rowWidth, entryHeight, 20, 4, 200, 20, 0, textureY);
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

                guiGraphics.drawString(font, trade.title, x, y, -2039584, false);
                guiGraphics.drawString(font, Component.literal("" + trade.uses), x, y + yOffset + 5, -2039584, false);

                if(currency != null){
                    currency.setCount(trade.cost);
                    guiGraphics.renderFakeItem(currency, x + xOffset, y + yOffset);
                    guiGraphics.renderItemDecorations(font, currency, x + xOffset, y + yOffset);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                TradeList.this.setSelected(this);
                NobleTradeScreen.this.selection = this.trade;
                NobleTradeScreen.this.updateHireButtonState();
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.empty();
            }
            private int getButtonTextureY(boolean hovered, boolean selected, boolean out) {
                final int BUTTON_Y_OUT = 46;
                final int BUTTON_Y_NORMAL = 66;
                final int BUTTON_Y_HOVER = 86;
                final int BUTTON_Y_PRESSED = 86;

                if (out) return BUTTON_Y_OUT;
                if (selected) return BUTTON_Y_PRESSED;
                if (hovered) return BUTTON_Y_HOVER;

                return BUTTON_Y_NORMAL;
            }
        }
    }

    private void onSelected(TradeList.TradeEntry entry) {
        this.tradeTitle = entry.trade.title;
        Component desc = entry.trade.description;
        this.descriptionBox.setValue(desc.getString());
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

        if(villagerList != null && villagerList.size() > 2 && ClientManager.configValueNobleNeedsVillagers){
            errorList.add(HireError.NOT_ENOUGH_VILLAGERS_NEARBY);
        }
        else if(selection != null){
            int playerMoney = getPlayerCurrencyAmount();
            if(!(player.isCreative() || playerMoney >= selection.cost)) errorList.add(HireError.NOT_ENOUGH_CURRENCY);
            if(selection.uses <= 0) errorList.add(HireError.NO_USES);
        }

        return errorList;
    }

    public enum HireError{
        NOT_ENOUGH_CURRENCY(ERROR_NOT_ENOUGH_CURRENCY),
        NOT_ENOUGH_VILLAGERS_NEARBY(ERROR_NOT_ENOUGH_VILLAGERS),
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

