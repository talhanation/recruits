package com.talhanation.recruits.client.gui;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.component.UnitIcons;
import com.talhanation.recruits.client.gui.widgets.PanelList;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.inventory.PromoteContainer;
import com.talhanation.recruits.network.*;
import de.maxhenkel.corelib.inventory.ScreenBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Promotion of a recruit to a companion: name field at the top, the professions as a list below
 * (icon, name, required level). Click on an unlocked entry promotes the recruit.
 */
public class PromoteScreen extends ScreenBase<PromoteContainer> {

    private static final ResourceLocation RESOURCE_LOCATION = new ResourceLocation(Main.MOD_ID, "textures/gui/professions/professions_main_gui.png");
    private static final int WIDTH = 197;
    private static final int HEIGHT = 250;
    private static final int LIST_TOP = 34;
    private static final int ENTRY_HEIGHT = 30;

    private final Player player;
    private final AbstractRecruitEntity recruit;
    private EditBox textField;
    private PanelList<ProfessionEntry> list;

    private static final MutableComponent BUTTON_MESSENGER = Component.translatable("gui.recruits.inv.text.messenger");
    private static final MutableComponent TOOLTIP_MESSENGER = Component.translatable("gui.recruits.inv.tooltip.messenger");

    private static final MutableComponent BUTTON_PATROL_LEADER = Component.translatable("gui.recruits.inv.text.patrol_leader");
    private static final MutableComponent TOOLTIP_PATROL_LEADER = Component.translatable("gui.recruits.inv.tooltip.patrol_leader");
    private static final MutableComponent BUTTON_CAPTAIN = Component.translatable("gui.recruits.inv.text.captain");
    private static final MutableComponent TOOLTIP_CAPTAIN = Component.translatable("gui.recruits.inv.tooltip.captain");
    private static final MutableComponent TOOLTIP_CAPTAIN_DISABLED = Component.translatable("gui.recruits.inv.tooltip.captain_disabled");
    private static final MutableComponent BUTTON_SCOUT = Component.translatable("gui.recruits.inv.text.scout");
    private static final MutableComponent TOOLTIP_SCOUT = Component.translatable("gui.recruits.inv.tooltip.scout");

    private static final MutableComponent BUTTON_GOVERNOR = Component.translatable("gui.recruits.inv.text.governor");
    private static final MutableComponent TOOLTIP_GOVERNOR = Component.translatable("gui.recruits.inv.tooltip.governor");

    private static final MutableComponent BUTTON_ASSASSIN = Component.translatable("gui.recruits.inv.text.assassin");
    private static final MutableComponent TOOLTIP_ASSASSIN = Component.translatable("gui.recruits.inv.tooltip.assassin");

    private static final MutableComponent BUTTON_SPY = Component.translatable("gui.recruits.inv.text.spy");
    private static final MutableComponent TOOLTIP_SPY = Component.translatable("gui.recruits.inv.tooltip.spy");

    private static final MutableComponent BUTTON_SIEGE_ENGINEER = Component.translatable("gui.recruits.inv.text.siege_engineer");
    private static final MutableComponent TOOLTIP_SIEGE_ENGINEER = Component.translatable("gui.recruits.inv.tooltip.siege_engineer");

    private static final MutableComponent BUTTON_ROGUE = Component.translatable("gui.recruits.inv.text.rogue");
    private static final MutableComponent TOOLTIP_ROGUE = Component.translatable("gui.recruits.inv.tooltip.rogue");
    private static final MutableComponent TOOLTIP_SIEGE_ENGINEER_DISABLED = Component.translatable("gui.recruits.inv.tooltip.siege_engineer_disabled");

    public PromoteScreen(PromoteContainer container, Inventory playerInventory, Component title) {
        super(RESOURCE_LOCATION, container, playerInventory, Component.literal(""));
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
        this.player = container.getPlayerEntity();
        this.recruit = container.getRecruit();
    }

    @Override
    protected void init() {
        super.init();
        setWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (textField != null) textField.tick();
    }

    private void setEditBox() {
        // the recruit's current name (custom name or type name), never a placeholder
        String name = recruit.getName().getString();

        textField = new EditBox(font, leftPos + 13, topPos + 8, 171, 20, recruit.getName());
        textField.setValue(name);
        textField.setTextColor(-1);
        textField.setTextColorUneditable(-1);
        textField.setBordered(true);
        textField.setFocused(true);
        textField.setMaxLength(13);

        addRenderableWidget(textField);
        setInitialFocus(textField);
    }

    private void setWidgets() {
        clearWidgets();
        setEditBox();

        list = new PanelList<>(leftPos + 9, topPos + LIST_TOP, WIDTH - 18 - 10, HEIGHT - LIST_TOP - 9, ENTRY_HEIGHT);
        List<ProfessionEntry> entries = new ArrayList<>();
        int level = recruit.getXpLevel();
        entries.add(new ProfessionEntry(BUTTON_MESSENGER, TOOLTIP_MESSENGER, 0, 2, level >= 2, true));
        entries.add(new ProfessionEntry(BUTTON_SCOUT, TOOLTIP_SCOUT, 1, 2, level >= 2, true));
        boolean siege = Main.isSiegeWeaponsLoaded && Main.isSiegeWeaponsCompatible;
        entries.add(new ProfessionEntry(BUTTON_SIEGE_ENGINEER, Main.isSiegeWeaponsCompatible ? TOOLTIP_SIEGE_ENGINEER : TOOLTIP_SIEGE_ENGINEER_DISABLED, 2, 3, level >= 3 && siege, siege));
        boolean ships = Main.isSmallShipsLoaded && Main.isSmallShipsCompatible;
        entries.add(new ProfessionEntry(BUTTON_CAPTAIN, Main.isSmallShipsCompatible ? TOOLTIP_CAPTAIN : TOOLTIP_CAPTAIN_DISABLED, 3, 5, level >= 5 && ships, ships));
        entries.add(new ProfessionEntry(BUTTON_PATROL_LEADER, TOOLTIP_PATROL_LEADER, 4, 5, level >= 5, true));
        entries.add(new ProfessionEntry(BUTTON_GOVERNOR, TOOLTIP_GOVERNOR, 6, 7, level >= 7, true));
        // not available yet
        entries.add(new ProfessionEntry(BUTTON_ASSASSIN, TOOLTIP_ASSASSIN, 5, 5, false, false));
        entries.add(new ProfessionEntry(BUTTON_SPY, TOOLTIP_SPY, 7, 7, false, false));
        entries.add(new ProfessionEntry(BUTTON_ROGUE, TOOLTIP_ROGUE, 8, 7, false, false));
        list.setEntries(entries);
        addRenderableWidget(list);
    }

    private void promote(int professionID) {
        if (recruit == null) return;
        String name = this.textField.getValue();
        if (name.isBlank()) name = recruit.getName().getString();
        Main.SIMPLE_CHANNEL.sendToServer(new MessagePromoteRecruit(this.recruit.getUUID(), professionID, name));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (textField != null) textField.setFocused(true);
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // the container screen does not pass dragging on to the list (scrollbar)
        if (list != null && list.isMouseOver(mouseX, mouseY) && list.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int key, int a, int b) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        setFocused(textField);

        return textField.keyPressed(key, a, b) || textField.canConsumeInput() || super.keyPressed(key, a, b);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF000000);
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + WIDTH - 1, y + HEIGHT - 1, 0xFF555555);
        g.fill(x + 3, y + 3, x + WIDTH - 3, y + HEIGHT - 3, 0xFFC6C6C6);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // no container title / inventory label
    }

    /** One profession: icon, name, required level; click promotes if unlocked. */
    private class ProfessionEntry extends PanelList.Entry<ProfessionEntry> {
        private final Component name;
        private final Component tooltip;
        private final int profession;
        private final int requiredLevel;
        private final boolean active;
        private final boolean available;

        ProfessionEntry(Component name, Component tooltip, int profession, int requiredLevel, boolean active, boolean available) {
            this.name = name;
            this.tooltip = tooltip;
            this.profession = profession;
            this.requiredLevel = requiredLevel;
            this.active = active;
            this.available = available;
        }

        @Override
        protected PanelList<ProfessionEntry> getList() {
            return PromoteScreen.this.list;
        }

        @Override
        protected void renderContent(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!active) g.setColor(0.55F, 0.55F, 0.55F, 1.0F);
            UnitIcons.render(g, UnitIcons.ofProfession(profession), left + 4, top + (height - UnitIcons.SIZE) / 2);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            g.drawString(font, name, left + 30, top + 4, active ? PanelList.TEXT_COLOR : PanelList.TEXT_GRAY, false);
            Component requirement;
            int color;
            if (!available) {
                requirement = Component.translatable("gui.recruits.promote.unavailable");
                color = PanelList.TEXT_GRAY;
            } else if (recruit.getXpLevel() < requiredLevel) {
                requirement = Component.translatable("gui.recruits.promote.requires_level", requiredLevel);
                color = PanelList.TEXT_RED;
            } else {
                requirement = Component.translatable("gui.recruits.promote.click");
                color = PanelList.TEXT_GREEN;
            }
            g.drawString(font, requirement, left + 30, top + 14, color, false);

            if (hovered && Minecraft.getInstance().screen != null) {
                //Minecraft.getInstance().screen.setTooltipForNextRenderPass(tooltip);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && active) {
                promote(profession);
                return true;
            }
            return false;
        }
    }
}
