package com.talhanation.recruits.client.gui.widgets;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collection;
import java.util.List;

/**
 * Scrollable list inside a panel of a screen, in the design of the faction list (dark entries, lighter when
 * hovered). Unlike the screen wide lists it is placed by its row area: rows start at {@code left} and are
 * {@code rowWidth} wide, the scrollbar sits right next to them. Entries can hold widgets (buttons), they are
 * placed in {@link Entry#renderContent}.
 */
@OnlyIn(Dist.CLIENT)
public class PanelList<E extends PanelList.Entry<E>> extends ContainerObjectSelectionList<E> {

    public static final int BG_FILL = FastColor.ARGB32.color(255, 60, 60, 60);
    public static final int BG_FILL_HOVERED = FastColor.ARGB32.color(255, 100, 100, 100);
    public static final int BG_FILL_SELECTED = FastColor.ARGB32.color(255, 10, 10, 10);
    public static final int TEXT_COLOR = FastColor.ARGB32.color(255, 255, 255, 255);
    public static final int TEXT_GRAY = FastColor.ARGB32.color(255, 170, 170, 170);
    public static final int TEXT_RED = FastColor.ARGB32.color(255, 255, 85, 85);
    public static final int TEXT_GREEN = FastColor.ARGB32.color(255, 85, 255, 85);

    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 2;

    private final int rowWidth;
    /** Alpha (0-255) of the whole list: background, entries. 255 = opaque. */
    private int alpha = 255;
    /** Background behind the rows, 0 = none. */
    private int backgroundColor;

    /**
     * @param left      x of the rows
     * @param top       y of the list
     * @param rowWidth  width of the rows (the scrollbar comes on top of it, right of the rows)
     * @param height    height of the list
     * @param itemHeight height of one entry including 4 px gap
     */
    public PanelList(int left, int top, int rowWidth, int height, int itemHeight) {
        // AbstractSelectionList centres the rows in its width: width = rows + scrollbar, x0 so the rows start at left
        // (getRowLeft = x0 + width / 2 - rowWidth / 2 + 2), so x0 = left - 12 and the scrollbar lies inside x1
        super(Minecraft.getInstance(), rowWidth + 2 * (SCROLLBAR_WIDTH + SCROLLBAR_GAP + 2), Minecraft.getInstance().getWindow().getGuiScaledHeight(),
                top, top + height, itemHeight);
        this.rowWidth = rowWidth;
        this.setLeftPos(left - (SCROLLBAR_WIDTH + SCROLLBAR_GAP + 2) - 2);
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
        this.setRenderSelection(false);
    }

    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
    }

    public int getAlpha() {
        return alpha;
    }

    public void setBackgroundColor(int argb) {
        this.backgroundColor = argb;
    }

    /** Applies the list alpha to an ARGB colour. */
    public int withAlpha(int argb) {
        int a = (argb >>> 24) * alpha / 255;
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public void setEntries(Collection<E> entries) {
        double scroll = this.getScrollAmount();
        this.replaceEntries(entries);
        this.setScrollAmount(scroll);
    }

    public void addListEntry(E entry) {
        this.addEntry(entry);
    }

    public void clearListEntries() {
        this.clearEntries();
    }

    public int getListLeft() {
        return this.getRowLeft();
    }

    public int getListTop() {
        return this.y0;
    }

    public int getListBottom() {
        return this.y1;
    }

    public boolean isEmpty() {
        return this.children().isEmpty();
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRowLeft() + rowWidth + SCROLLBAR_GAP;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (backgroundColor != 0) {
            guiGraphics.fill(getRowLeft() - 2, y0, getScrollbarPosition() + SCROLLBAR_WIDTH, y1, withAlpha(backgroundColor));
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /** One row. Children (buttons etc.) are positioned every frame in {@link #renderContent}. */
    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry<E extends Entry<E>> extends ContainerObjectSelectionList.Entry<E> {

        protected final List<AbstractWidget> children = Lists.newArrayList();

        protected abstract PanelList<E> getList();

        /** Highlighted like the selected entry of the faction list. */
        protected boolean isSelected() {
            return false;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            PanelList<E> list = getList();
            int color = isSelected() ? BG_FILL_SELECTED : hovered ? BG_FILL_HOVERED : BG_FILL;
            guiGraphics.fill(left, top, left + width, top + height, list.withAlpha(color));
            renderContent(guiGraphics, index, top, left, width, height, mouseX, mouseY, hovered, partialTick);
            for (AbstractWidget widget : children) {
                widget.setAlpha(list.getAlpha() / 255F);
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        /** Draws the entry and sets the positions of its widgets. */
        protected abstract void renderContent(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick);

        @Override
        public List<? extends NarratableEntry> narratables() {
            return children;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }
    }
}
