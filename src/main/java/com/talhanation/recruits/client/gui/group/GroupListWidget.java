package com.talhanation.recruits.client.gui.group;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.List;
public class GroupListWidget extends ObjectSelectionList<GroupListWidget.GroupEntry> {
    private final int listWidth;
    private final GroupManageScreen parent;
    private List<RecruitsGroup> groups;

    public GroupListWidget(GroupManageScreen parent, int listWidth, int top, int bottom, List<RecruitsGroup> groups) {
        super(parent.getMinecraft(), listWidth, parent.height, top, bottom, parent.getFont().lineHeight * 2 + 12);
        this.parent = parent;
        this.listWidth = listWidth;
        this.groups = groups;
        this.refreshList();
    }

    public void removeGroup(GroupEntry entry){
        this.removeEntry(entry);
    }
    @Override
    protected int getScrollbarPosition() {
        return this.x0 + this.listWidth - 6; // Adjust scrollbar position
    }

    @Override
    public int getRowWidth() {
        return this.listWidth;
    }

    public void refreshList() {
        this.clearEntries();
        for (RecruitsGroup groupInfo : groups) {
            this.addEntry(new GroupEntry(groupInfo, this.parent));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0.0) {
            this.setScrollAmount(this.getScrollAmount() - delta * this.itemHeight / 2.0);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isMouseOver(mouseX, mouseY)) {
            this.setScrollAmount(this.getScrollAmount() - dragY);
            return true;
        }
        return false;
    }

    public class GroupEntry extends ObjectSelectionList.Entry<GroupEntry> {
        private final RecruitsGroup group;
        private final GroupManageScreen parent;

        GroupEntry(RecruitsGroup group, GroupManageScreen parent) {
            this.group = group;
            this.parent = parent;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", group.getName());
        }

        public void renderBack(GuiGraphics guiGraphics, int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            // Grauen Hintergrund rendern
            guiGraphics.fill(left, top, left + entryWidth, top + entryHeight, 0xFF404040);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int entryIdx, int top, int left, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Font font = this.parent.getFont();
            Component name = Component.literal(group.getName());
            //Component id = Component.literal("ID: " + group.getId());
            Component count = Component.literal("Count: " + group.getCount());

            guiGraphics.drawString(font, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(name, listWidth))), left + 3, top + 2, 0xFFFFFF, false);
            //guiGraphics.drawString(font, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(id, listWidth))), left + 3, top + 2 + font.lineHeight, 0xCCCCCC, false);
            guiGraphics.drawString(font, Language.getInstance().getVisualOrder(FormattedText.composite(font.substrByWidth(count, listWidth))), left + 3, top + 2 + font.lineHeight, 0xCCCCCC, false);

        }

        private int calculateTotalCount() {
            return groups.stream().mapToInt(RecruitsGroup::getCount).sum();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                parent.setSelected(this);
                GroupListWidget.this.setSelected(this);
                return true;
            }
            return false;
        }

        public RecruitsGroup getGroup() {
            return group;
        }
    }
}
