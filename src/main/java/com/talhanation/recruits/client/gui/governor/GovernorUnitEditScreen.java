package com.talhanation.recruits.client.gui.governor;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.governor.GovernorSnapshot;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.component.UnitIcons;
import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorUnitType;
import com.talhanation.recruits.network.MessageGovernorSetUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Edit one unit type of the garrison: wanted amount at the top, the equipment template as ghost slots, the
 * player inventory below. Click an item of your inventory to pick it up as template (it stays in your
 * inventory), then click a slot to set it; click a slot with nothing picked (or right-click) to clear it
 * ("keep what he has"). Save sends everything at once, Cancel discards. Both go back to the Governor screen.
 */
@OnlyIn(Dist.CLIENT)
public class GovernorUnitEditScreen extends RecruitsScreenBase {

    private static final int WIDTH = 196;
    private static final int HEIGHT = 238;
    private static final int TEXT = 0x404040;
    private static final int SLOT = 18;
    /** Same order as {@link com.talhanation.recruits.governor.GovernorGarrison#LOADOUT_SLOTS}. */
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
            EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final String[] SLOT_KEYS = {"mainhand", "offhand", "head", "chest", "legs", "feet"};

    private final GovernorScreen parent;
    private final GovernorSnapshot snapshot;
    private final GovernorUnitType type;
    private final String[] items = new String[ClaimGovernorData.LOADOUT_SLOTS];
    private int amount;
    private final int maxAmount;
    @Nullable
    private ItemStack picked;

    public GovernorUnitEditScreen(GovernorScreen parent, GovernorSnapshot snapshot, GovernorUnitType type) {
        super(Component.translatable("gui.recruits.governor.unit.title", type.getDisplayName()), WIDTH, HEIGHT);
        this.parent = parent;
        this.snapshot = snapshot;
        this.type = type;
        for (int i = 0; i < items.length; i++) {
            String id = snapshot.loadout[type.getIndex()][i];
            items[i] = id == null ? "" : id;
        }
        this.amount = snapshot.quota[type.getIndex()];
        int others = snapshot.getTotalQuota() - this.amount;
        this.maxAmount = Math.max(0, snapshot.cap - others);
    }

    public GovernorScreen getParent() {
        return parent;
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int y = guiTop + 30;
        int center = guiLeft + WIDTH / 2;
        addAmountButton(center - 62, y, 22, "-10", -10);
        addAmountButton(center - 38, y, 16, "-1", -1);
        addAmountButton(center + 22, y, 16, "+1", 1);
        addAmountButton(center + 40, y, 22, "+10", 10);

        int buttonY = guiTop + HEIGHT - 26;
        addRenderableWidget(new ExtendedButton(guiLeft + 10, buttonY, 84, 20, Component.translatable("gui.recruits.governor.unit.save"), b -> save()));
        addRenderableWidget(new ExtendedButton(guiLeft + WIDTH - 94, buttonY, 84, 20, Component.translatable("gui.recruits.governor.unit.cancel"), b -> back()));
    }

    private void addAmountButton(int x, int y, int width, String label, int delta) {
        ExtendedButton button = new ExtendedButton(x, y, width, 16, Component.literal(label), b -> {
            amount = Math.max(0, Math.min(maxAmount, amount + delta));
            rebuild();
        });
        button.active = delta < 0 ? amount > 0 : amount < maxAmount;
        if (delta > 0) button.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.quota")));
        addRenderableWidget(button);
    }

    private void save() {
        if (snapshot.claim != null) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorSetUnit(snapshot.claim, type, amount, items));
        }
        back();
    }

    private void back() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void onClose() {
        back();
    }

    // ------------------------------------------------------------------ layout

    private int slotX(int slot) {
        // main hand, off hand on the left; armour in a row
        return guiLeft + 14 + slot * (SLOT + 10);
    }

    private int slotY() {
        return guiTop + 66;
    }

    private int inventoryX() {
        return guiLeft + (WIDTH - 9 * SLOT) / 2;
    }

    private int inventoryY() {
        return guiTop + 108;
    }

    /** Inventory index of the cell: rows 9-35, then the hotbar 0-8 (like the player inventory screen). */
    private static int inventoryIndex(int column, int row) {
        return row < 3 ? 9 + row * 9 + column : column;
    }

    private int cellY(int row) {
        return inventoryY() + row * SLOT + (row == 3 ? 4 : 0);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            for (int slot = 0; slot < SLOTS.length; slot++) {
                if (isIn(mouseX, mouseY, slotX(slot), slotY())) {
                    if (button == 1 || picked == null) {
                        items[slot] = "";
                    } else if (fits(picked, slot)) {
                        ResourceLocation key = ForgeRegistries.ITEMS.getKey(picked.getItem());
                        items[slot] = key == null ? "" : key.toString();
                        picked = null;
                    }
                    return true;
                }
            }
            Inventory inventory = player.getInventory();
            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 9; column++) {
                    if (!isIn(mouseX, mouseY, inventoryX() + column * SLOT, cellY(row))) continue;
                    ItemStack stack = inventory.getItem(inventoryIndex(column, row));
                    picked = stack.isEmpty() || button == 1 ? null : stack.copyWithCount(1);
                    return true;
                }
            }
        }
        if (picked != null && button == 1) {
            picked = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int step = Screen.hasShiftDown() ? 10 : 1;
        amount = Math.max(0, Math.min(maxAmount, amount + (delta > 0 ? step : -step)));
        rebuild();
        return true;
    }

    private static boolean isIn(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
    }

    /** Armour only into its slot; hands take any item (the server checks what the unit can hold). */
    private static boolean fits(ItemStack stack, int slot) {
        EquipmentSlot target = SLOTS[slot];
        if (target.getType() == EquipmentSlot.Type.ARMOR) return Mob.getEquipmentSlotForItem(stack) == target;
        return true;
    }

    private static ItemStack stackOf(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(location);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = guiLeft;
        int y = guiTop;
        g.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF000000);
        g.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xFFFFFFFF);
        g.fill(x + 3, y + 3, x + WIDTH - 1, y + HEIGHT - 1, 0xFF555555);
        g.fill(x + 3, y + 3, x + WIDTH - 3, y + HEIGHT - 3, 0xFFC6C6C6);
        g.fill(x + 4, y + 52, x + WIDTH - 4, y + 53, 0xFF8B8B8B);
        g.fill(x + 4, y + 96, x + WIDTH - 4, y + 97, 0xFF8B8B8B);
    }

    @Override
    public void renderForeground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        UnitIcons.render(g, UnitIcons.of(type), guiLeft + 8, guiTop + 6);
        g.drawString(font, type.getDisplayName(), guiLeft + 34, guiTop + 8, TEXT, false);
        int count = snapshot.counts[type.getIndex()];
        g.drawString(font, Component.translatable("gui.recruits.governor.unit.present", count), guiLeft + 34, guiTop + 18, TEXT, false);

        g.drawCenteredString(font, Component.translatable("gui.recruits.governor.unit.amount", amount, maxAmount), guiLeft + WIDTH / 2, guiTop + 34, 0xFFFFFF);
        g.drawString(font, Component.translatable("gui.recruits.governor.unit.equipment"), guiLeft + 8, guiTop + 56, TEXT, false);

        ItemStack hoveredStack = ItemStack.EMPTY;
        Component hoveredText = null;
        for (int slot = 0; slot < SLOTS.length; slot++) {
            int sx = slotX(slot);
            int sy = slotY();
            boolean hovered = isIn(mouseX, mouseY, sx, sy);
            boolean canDrop = picked != null && fits(picked, slot);
            drawSlot(g, sx, sy, hovered, picked != null && canDrop ? 0xFF7FB27F : 0);
            ItemStack stack = stackOf(items[slot]);
            if (!stack.isEmpty()) g.renderItem(stack, sx + 1, sy + 1);
            Component slotName = Component.translatable("gui.recruits.governor.slot." + SLOT_KEYS[slot]);
            if (hovered) {
                hoveredText = stack.isEmpty()
                        ? Component.translatable("gui.recruits.governor.tooltip.slot_keep", slotName)
                        : Component.translatable("gui.recruits.governor.tooltip.slot_template", slotName, stack.getHoverName());
            }
        }

        g.drawString(font, Component.translatable("gui.recruits.governor.unit.inventory"), inventoryX(), inventoryY() - 10, TEXT, false);
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Inventory inventory = player.getInventory();
            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 9; column++) {
                    int cx = inventoryX() + column * SLOT;
                    int cy = cellY(row);
                    boolean hovered = isIn(mouseX, mouseY, cx, cy);
                    drawSlot(g, cx, cy, hovered, 0);
                    ItemStack stack = inventory.getItem(inventoryIndex(column, row));
                    if (!stack.isEmpty()) {
                        g.renderItem(stack, cx + 1, cy + 1);
                        g.renderItemDecorations(font, stack, cx + 1, cy + 1);
                        if (hovered) hoveredStack = stack;
                    }
                }
            }
        }

        g.drawCenteredString(font, Component.translatable("gui.recruits.governor.unit.hint"), guiLeft + WIDTH / 2, guiTop + HEIGHT - 38, 0xFFFFFF);

        // the picked item follows the mouse
        if (picked != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 300);
            g.renderItem(picked, mouseX - 8, mouseY - 8);
            g.pose().popPose();
        } else if (!hoveredStack.isEmpty()) {
            g.renderTooltip(font, hoveredStack, mouseX, mouseY);
        }
        if (hoveredText != null) g.renderTooltip(font, hoveredText, mouseX, mouseY);
    }

    private static void drawSlot(GuiGraphics g, int x, int y, boolean hovered, int highlight) {
        g.fill(x, y, x + SLOT, y + SLOT, 0xFF373737);
        g.fill(x + 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, highlight != 0 ? highlight : hovered ? 0xFF9A9A9A : 0xFF8B8B8B);
    }
}
