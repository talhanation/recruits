package com.talhanation.recruits.client.gui.governor;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.governor.GovernorSnapshot;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.commandscreen.GovernorCategory;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.recruits.client.gui.widgets.SelectedClaimWidget;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorPost;
import com.talhanation.recruits.governor.GovernorUnitType;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Governor screen with the tabs Info, Garrison, Order and Village (only if a village lies in the claim).
 * All changes are sent to the server, which answers with a new snapshot.
 */
@OnlyIn(Dist.CLIENT)
public class GovernorScreen extends RecruitsScreenBase {

    private enum Tab {INFO, GARRISON, ORDER, VILLAGE}

    private static final int WIDTH = 310;
    private static final int HEIGHT = 274;
    /** Height of the header row with the Governor name and the claim widget. */
    private static final int HEADER = 24;
    private static final int TEXT = 0x404040;
    private static final int RED = 0xB02020;
    private static final int GREEN = 0x207020;
    private static final int ORDER_ROWS = 12;
    private static final String[] SLOT_KEYS = {"mainhand", "offhand", "head", "chest", "legs", "feet"};

    private static Tab lastTab = Tab.INFO;

    private GovernorSnapshot snapshot;
    private Tab tab;
    private int orderScroll;
    @Nullable
    private RecruitsGroup selectedGroup;
    @Nullable
    private ScrollDropDownMenu<RecruitsGroup> groupDropDown;

    @Nullable
    private final UUID governorId;
    @Nullable
    private SelectedClaimWidget claimWidget;

    public GovernorScreen(GovernorSnapshot snapshot) {
        super(Component.translatable("gui.recruits.governor.title"), WIDTH, HEIGHT);
        this.snapshot = snapshot;
        this.governorId = snapshot.self;
        this.tab = lastTab;
    }

    @Nullable
    public UUID getClaimId() {
        return snapshot.claim;
    }

    /** Snapshots of this Governor, or of his claim. */
    public boolean matches(GovernorSnapshot other) {
        if (governorId != null && governorId.equals(other.self)) return true;
        return other.self == null && snapshot.claim != null && snapshot.claim.equals(other.claim);
    }

    public void update(GovernorSnapshot snapshot) {
        this.snapshot = snapshot;
        this.rebuild();
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    private void setTab(Tab tab) {
        this.tab = tab;
        lastTab = tab;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        groupDropDown = null;
        buildClaimWidget();
        if (!snapshot.hasClaim()) return;
        if (tab == Tab.VILLAGE && !snapshot.village) tab = Tab.INFO;

        int x = guiLeft + 6;
        int tabWidth = 70;
        addTabButton(x, Tab.INFO, "info");
        addTabButton(x + (tabWidth + 4), Tab.GARRISON, "garrison");
        addTabButton(x + (tabWidth + 4) * 2, Tab.ORDER, "order");
        if (snapshot.village) addTabButton(x + (tabWidth + 4) * 3, Tab.VILLAGE, "village");

        switch (tab) {
            case GARRISON -> buildGarrison();
            case ORDER -> buildOrder();
            case VILLAGE -> buildVillage();
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ claim widget (office)

    /**
     * Top right: the claim the Governor governs, like the selected player in other screens.
     * Click on it opens the claim list, "x" removes him from office. Without office a button opens the list.
     */
    private void buildClaimWidget() {
        int w = 170;
        int x = guiLeft + WIDTH - 8 - w;
        int y = guiTop + 4;
        claimWidget = null;
        if (governorId == null) return;

        if (snapshot.hasClaim()) {
            claimWidget = new SelectedClaimWidget(font, x, y, w, 20, Component.literal("x"),
                    () -> Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorSetClaim(governorId, null)),
                    this::openClaimList);
            Component info = snapshot.selfInClaim
                    ? Component.translatable("gui.recruits.governor.office.in_office", snapshot.chunkCount)
                    : Component.translatable("gui.recruits.governor.office.travelling");
            claimWidget.setClaim(snapshot.claimName, info.getString(), findFaction());
            claimWidget.setButtonTooltip(Component.translatable("gui.recruits.governor.office.tooltip_remove"));
            claimWidget.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.office.tooltip_change")));
            addRenderableWidget(claimWidget);
        } else {
            ExtendedButton select = new ExtendedButton(x, y, w, 20, Component.translatable("gui.recruits.governor.office.select"), b -> openClaimList());
            select.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.office.tooltip_select")));
            addRenderableWidget(select);
        }
    }

    @Nullable
    private RecruitsFaction findFaction() {
        if (snapshot.claim == null) return null;
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim != null && snapshot.claim.equals(claim.getUUID())) return claim.getOwnerFaction();
        }
        return null;
    }

    private void openClaimList() {
        if (governorId == null || minecraft == null || minecraft.player == null) return;
        minecraft.setScreen(new SelectClaimScreen(this, minecraft.player, governorId, snapshot));
    }

    private void addTabButton(int x, Tab target, String key) {
        ExtendedButton button = new ExtendedButton(x, guiTop + HEADER + 6, 70, 18, Component.translatable("gui.recruits.governor.tab." + key), b -> setTab(target));
        button.active = tab != target;
        addRenderableWidget(button);
    }

    // ------------------------------------------------------------------ garrison tab

    private void buildGarrison() {
        int top = guiTop + HEADER + 50;
        int left = guiLeft + 8;
        int maxQuota = Math.max(0, snapshot.cap);

        for (GovernorUnitType type : GovernorUnitType.values()) {
            int row = type.getIndex();
            int y = top + row * 21;
            int quota = snapshot.quota[type.getIndex()];

            ExtendedButton minus = new ExtendedButton(left + 112, y + 2, 14, 14, Component.literal("-"), b -> {
                int step = Screen.hasShiftDown() ? 10 : 1;
                sendQuota(type, Math.max(0, quota - step));
            });
            minus.active = quota > 0;
            addRenderableWidget(minus);

            ExtendedButton plus = new ExtendedButton(left + 128, y + 2, 14, 14, Component.literal("+"), b -> {
                int step = Screen.hasShiftDown() ? 10 : 1;
                sendQuota(type, quota + step);
            });
            plus.active = snapshot.getTotalQuota() < maxQuota;
            plus.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.quota")));
            addRenderableWidget(plus);

            for (int slot = 0; slot < ClaimGovernorData.LOADOUT_SLOTS; slot++) {
                addRenderableWidget(new LoadoutSlotButton(left + 150 + slot * 20, y, type, slot));
            }
        }

        int budgetY = top + GovernorUnitType.COUNT * 21 + 4;
        int bx = guiLeft + 110;
        addRenderableWidget(new ExtendedButton(bx, budgetY, 24, 14, Component.literal("-10"), b -> sendBudget(snapshot.budget - 10)));
        addRenderableWidget(new ExtendedButton(bx + 26, budgetY, 18, 14, Component.literal("-1"), b -> sendBudget(snapshot.budget - 1)));
        addRenderableWidget(new ExtendedButton(bx + 82, budgetY, 18, 14, Component.literal("+1"), b -> sendBudget(snapshot.budget + 1)));
        addRenderableWidget(new ExtendedButton(bx + 102, budgetY, 24, 14, Component.literal("+10"), b -> sendBudget(snapshot.budget + 10)));

        int groupY = budgetY + 20;
        List<RecruitsGroup> groups = new ArrayList<>();
        for (RecruitsGroup group : ClientManager.groups) {
            if (!group.removed) groups.add(group);
        }
        if (selectedGroup != null && !groups.contains(selectedGroup)) selectedGroup = null;
        groupDropDown = new ScrollDropDownMenu<>(selectedGroup, guiLeft + 110, groupY, 110, 14, groups,
                RecruitsGroup::getName,
                group -> {
                    selectedGroup = group;
                    rebuild();
                });
        // rendered and clicked manually so the open list is drawn above everything else

        ExtendedButton assign = new ExtendedButton(guiLeft + 224, groupY, 78, 14, Component.translatable("gui.recruits.governor.assign_group"), b -> {
            if (selectedGroup != null) {
                Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorAssignGroup(snapshot.claim, selectedGroup.getUUID()));
            }
        });
        assign.active = selectedGroup != null;
        assign.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.assign_group")));
        addRenderableWidget(assign);
    }

    private void sendQuota(GovernorUnitType type, int amount) {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorSetQuota(snapshot.claim, type, amount));
    }

    private void sendBudget(int budget) {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorSetBudget(snapshot.claim, Math.max(0, budget)));
    }

    /** Ghost slot: click sets the item in your main hand as template (not consumed), empty hand clears it. */
    private class LoadoutSlotButton extends ExtendedButton {
        private final GovernorUnitType type;
        private final int slot;

        LoadoutSlotButton(int x, int y, GovernorUnitType type, int slot) {
            super(x, y, 18, 18, Component.empty(), b -> {
            });
            this.type = type;
            this.slot = slot;
            ItemStack stack = getStack();
            Component tooltip = stack.isEmpty()
                    ? Component.translatable("gui.recruits.governor.tooltip.slot_empty", Component.translatable("gui.recruits.governor.slot." + SLOT_KEYS[slot]))
                    : Component.translatable("gui.recruits.governor.tooltip.slot_set", Component.translatable("gui.recruits.governor.slot." + SLOT_KEYS[slot]), stack.getHoverName());
            this.setTooltip(Tooltip.create(tooltip));
        }

        private ItemStack getStack() {
            String id = snapshot.loadout[type.getIndex()][slot];
            if (id == null || id.isEmpty()) return ItemStack.EMPTY;
            ResourceLocation location = ResourceLocation.tryParse(id);
            if (location == null) return ItemStack.EMPTY;
            Item item = ForgeRegistries.ITEMS.getValue(location);
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }

        @Override
        public void onPress() {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            ItemStack hand = player.getMainHandItem();
            String id = "";
            if (!hand.isEmpty()) {
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(hand.getItem());
                id = key == null ? "" : key.toString();
            }
            Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorSetLoadout(snapshot.claim, type, slot, id));
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int color = isHovered() ? 0xFF9A9A9A : 0xFF8B8B8B;
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xFF373737);
            guiGraphics.fill(getX() + 1, getY() + 1, getX() + width, getY() + height, 0xFFFFFFFF);
            guiGraphics.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, color);
            ItemStack stack = getStack();
            if (!stack.isEmpty()) guiGraphics.renderItem(stack, getX() + 1, getY() + 1);
        }
    }

    // ------------------------------------------------------------------ order tab

    private List<GovernorPost> sortedPosts() {
        List<GovernorPost> posts = new ArrayList<>(snapshot.posts);
        posts.sort(Comparator.comparingInt((GovernorPost p) -> p.getType().getIndex()).thenComparingInt(GovernorPost::getNumber));
        return posts;
    }

    private void buildOrder() {
        List<GovernorPost> posts = sortedPosts();
        orderScroll = Math.max(0, Math.min(orderScroll, Math.max(0, posts.size() - ORDER_ROWS)));
        int top = guiTop + HEADER + 46;
        for (int i = 0; i < ORDER_ROWS && orderScroll + i < posts.size(); i++) {
            GovernorPost post = posts.get(orderScroll + i);
            ExtendedButton delete = new ExtendedButton(guiLeft + WIDTH - 36, top + i * 15, 14, 13, Component.literal("x"),
                    b -> Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorRemovePost(snapshot.claim, post.getId())));
            delete.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.delete_post")));
            addRenderableWidget(delete);
        }
        if (posts.size() > ORDER_ROWS) {
            addRenderableWidget(new ExtendedButton(guiLeft + WIDTH - 20, top, 14, 13, Component.literal("^"), b -> {
                orderScroll = Math.max(0, orderScroll - 1);
                rebuild();
            }));
            addRenderableWidget(new ExtendedButton(guiLeft + WIDTH - 20, top + (ORDER_ROWS - 1) * 15, 14, 13, Component.literal("v"), b -> {
                orderScroll++;
                rebuild();
            }));
        }
    }

    // ------------------------------------------------------------------ village tab

    private void buildVillage() {
        int top = guiTop + HEADER + 100;
        for (int level = 0; level <= 3; level++) {
            int taxLevel = level;
            ExtendedButton button = new ExtendedButton(guiLeft + 10 + level * 74, top, 70, 18,
                    Component.translatable("gui.recruits.governor.tax." + level), b ->
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorSetTax(snapshot.claim, taxLevel)));
            button.active = snapshot.taxLevel != level;
            button.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.tax",
                    level * snapshot.taxBase, snapshot.taxPenalty[level])));
            addRenderableWidget(button);
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (groupDropDown != null && groupDropDown.isMouseOver(mouseX, mouseY)) {
            groupDropDown.onMouseClick(mouseX, mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (groupDropDown != null) groupDropDown.onMouseMove(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (groupDropDown != null && groupDropDown.mouseScrolled(mouseX, mouseY, delta)) return true;
        if (tab == Tab.ORDER) {
            orderScroll = Math.max(0, orderScroll + (delta > 0 ? -1 : 1));
            rebuild();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        int x = guiLeft;
        int y = guiTop;
        guiGraphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF000000);
        guiGraphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, 0xFFFFFFFF);
        guiGraphics.fill(x + 3, y + 3, x + WIDTH - 1, y + HEIGHT - 1, 0xFF555555);
        guiGraphics.fill(x + 3, y + 3, x + WIDTH - 3, y + HEIGHT - 3, 0xFFC6C6C6);
        guiGraphics.fill(x + 4, y + HEADER + 27, x + WIDTH - 4, y + HEADER + 28, 0xFF8B8B8B);
        guiGraphics.fill(x + 4, y + HEADER + 1, x + WIDTH - 4, y + HEADER + 2, 0xFF8B8B8B);
    }

    @Override
    public void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        String name = !snapshot.selfName.isEmpty() ? snapshot.selfName : snapshot.governorName;
        guiGraphics.drawString(font, font.plainSubstrByWidth(name, WIDTH - 16 - 178), guiLeft + 8, guiTop + 10, TEXT, false);

        if (!snapshot.hasClaim()) {
            renderNoOffice(guiGraphics);
            return;
        }

        switch (tab) {
            case INFO -> renderInfo(guiGraphics);
            case GARRISON -> renderGarrison(guiGraphics);
            case ORDER -> renderOrder(guiGraphics);
            case VILLAGE -> renderVillage(guiGraphics);
        }

        if (groupDropDown != null) groupDropDown.renderWidget(guiGraphics, mouseX, mouseY, delta);
    }

    private void renderNoOffice(GuiGraphics g) {
        int x = guiLeft + 8;
        int y = guiTop + HEADER + 14;
        line(g, Component.translatable("gui.recruits.governor.office.none"), x, y, RED);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.translatable("gui.recruits.governor.office.hint"), WIDTH - 16);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x, y + 16 + i * 10, TEXT, false);
        }
    }

    private void line(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    private void renderInfo(GuiGraphics g) {
        int x = guiLeft + 8;
        int y = guiTop + HEADER + 48;
        int step = 12;

        line(g, Component.translatable("gui.recruits.governor.info.claim", snapshot.claimName, snapshot.chunkCount), x, y, TEXT);
        line(g, Component.translatable("gui.recruits.governor.info.owner", snapshot.ownerName), x, y += step, TEXT);
        line(g, Component.translatable("gui.recruits.governor.info.faction", snapshot.factionName), x, y += step, TEXT);
        Component status;
        if (snapshot.ruling && snapshot.self != null && !snapshot.selfInClaim) status = Component.translatable("gui.recruits.governor.status.travelling");
        else if (snapshot.ruling) status = Component.translatable("gui.recruits.governor.status.ruling");
        else status = Component.translatable("gui.recruits.governor.status." + snapshot.status);
        line(g, Component.translatable("gui.recruits.governor.info.governor", snapshot.governorName, status), x, y += step, snapshot.ruling ? GREEN : RED);
        line(g, Component.translatable("gui.recruits.governor.info.garrison", snapshot.garrisonTotal, snapshot.getTotalQuota(), snapshot.cap), x, y += step, TEXT);

        Component coins;
        if (!snapshot.hasTreasury) coins = Component.translatable("gui.recruits.governor.info.no_treasury");
        else if (snapshot.treasuryMissing) coins = Component.translatable("gui.recruits.governor.info.treasury_missing");
        else if (snapshot.coins < 0) coins = Component.literal("?");
        else coins = Component.literal(String.valueOf(snapshot.coins));
        line(g, Component.translatable("gui.recruits.governor.info.treasury", coins), x, y += step, TEXT);
        line(g, Component.translatable("gui.recruits.governor.info.spent", snapshot.spentToday, snapshot.budget), x, y += step, TEXT);
        if (snapshot.purse > 0) line(g, Component.translatable("gui.recruits.governor.info.purse", snapshot.purse), x, y += step, TEXT);

        y += step + 4;
        if (!snapshot.hasTreasury || snapshot.treasuryMissing) line(g, Component.translatable("gui.recruits.governor.warn.treasury"), x, y += step, RED);
        if (snapshot.upkeepMissing) line(g, Component.translatable("gui.recruits.governor.warn.upkeep"), x, y += step, RED);
        if (snapshot.underSiege) line(g, Component.translatable("gui.recruits.governor.warn.siege"), x, y += step, RED);
        if (!snapshot.ruling) line(g, Component.translatable("gui.recruits.governor.warn.not_ruling"), x, y += step, RED);

        renderMap(g, guiLeft + WIDTH - 128, guiTop + HEADER + 48, 118);
    }

    /** Small cut-out of the claim: chunks as squares, posts as coloured dots. */
    private void renderMap(GuiGraphics g, int x, int y, int size) {
        if (snapshot.chunks.isEmpty()) return;
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (ChunkPos chunk : snapshot.chunks) {
            minX = Math.min(minX, chunk.x);
            minZ = Math.min(minZ, chunk.z);
            maxX = Math.max(maxX, chunk.x);
            maxZ = Math.max(maxZ, chunk.z);
        }
        int chunksWide = Math.max(maxX - minX + 1, maxZ - minZ + 1);
        int cell = Math.max(2, size / chunksWide);

        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF373737);
        g.fill(x, y, x + size, y + size, 0xFF6F8F5F);
        for (ChunkPos chunk : snapshot.chunks) {
            int cx = x + (chunk.x - minX) * cell;
            int cz = y + (chunk.z - minZ) * cell;
            g.fill(cx, cz, cx + cell - 1, cz + cell - 1, 0xFF9DC48A);
        }
        double scale = cell / 16D;
        for (GovernorPost post : snapshot.posts) {
            int px = x + (int) ((post.getPos().getX() - minX * 16) * scale);
            int pz = y + (int) ((post.getPos().getZ() - minZ * 16) * scale);
            g.fill(px - 1, pz - 1, px + 2, pz + 2, GovernorCategory.colorOf(post.getType()));
        }
    }

    private void renderGarrison(GuiGraphics g) {
        int top = guiTop + HEADER + 50;
        int left = guiLeft + 8;
        line(g, Component.translatable("gui.recruits.governor.garrison.header", snapshot.garrisonTotal, snapshot.getTotalQuota(), snapshot.cap), left, guiTop + HEADER + 40, TEXT);
        for (GovernorUnitType type : GovernorUnitType.values()) {
            int y = top + type.getIndex() * 21;
            g.drawString(font, type.getDisplayName(), left, y + 5, TEXT, false);
            String count = snapshot.counts[type.getIndex()] + "/" + snapshot.quota[type.getIndex()];
            g.drawString(font, count, left + 80, y + 5, snapshot.counts[type.getIndex()] < snapshot.quota[type.getIndex()] ? RED : TEXT, false);
        }
        int budgetY = top + GovernorUnitType.COUNT * 21 + 4;
        line(g, Component.translatable("gui.recruits.governor.budget"), left, budgetY + 3, TEXT);
        g.drawCenteredString(font, String.valueOf(snapshot.budget), guiLeft + 110 + 63, budgetY + 3, 0xFFFFFF);
        line(g, Component.translatable("gui.recruits.governor.assign"), left, budgetY + 23, TEXT);
    }

    private void renderOrder(GuiGraphics g) {
        List<GovernorPost> posts = sortedPosts();
        int top = guiTop + HEADER + 46;
        int left = guiLeft + 8;
        if (posts.isEmpty()) {
            line(g, Component.translatable("gui.recruits.governor.order.empty"), left, top + 2, TEXT);
            return;
        }
        for (int i = 0; i < ORDER_ROWS && orderScroll + i < posts.size(); i++) {
            GovernorPost post = posts.get(orderScroll + i);
            int y = top + i * 15 + 3;
            g.fill(left, y - 1, left + 6, y + 6, GovernorCategory.colorOf(post.getType()));
            g.drawString(font, post.getLabel().getString() + " " + post.getCoordinateString(), left + 10, y, TEXT, false);
            String assigned = snapshot.assignedNames.get(post.getId());
            if (post.getType().isGuardPost()) {
                String text = assigned != null ? assigned : Component.translatable("gui.recruits.governor.order.unassigned").getString();
                g.drawString(font, text, left + 175, y, assigned != null ? TEXT : RED, false);
            }
        }
    }

    private void renderVillage(GuiGraphics g) {
        int x = guiLeft + 8;
        int y = guiTop + HEADER + 48;
        line(g, Component.translatable("gui.recruits.governor.village.inhabitants", snapshot.inhabitants), x, y, TEXT);
        line(g, Component.translatable("gui.recruits.governor.village.tax_level",
                Component.translatable("gui.recruits.governor.tax." + snapshot.taxLevel)), x, y + 12, TEXT);
        line(g, Component.translatable("gui.recruits.governor.village.tax_today", snapshot.taxToday), x, y + 24, TEXT);
        line(g, Component.translatable("gui.recruits.governor.village.expected",
                (snapshot.inhabitants / 10) * snapshot.taxLevel * snapshot.taxBase), x, y + 36, TEXT);
        line(g, Component.translatable("gui.recruits.governor.village.note").withStyle(ChatFormatting.ITALIC), x, guiTop + HEADER + 130, TEXT);
        line(g, Component.translatable("gui.recruits.governor.village.workers").withStyle(ChatFormatting.GRAY), x, guiTop + HEADER + 150, 0x707070);
    }
}
