package com.talhanation.recruits.client.gui.governor;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.governor.GovernorSnapshot;
import com.talhanation.recruits.client.gui.RecruitsScreenBase;
import com.talhanation.recruits.client.gui.commandscreen.GovernorCategory;
import com.talhanation.recruits.client.gui.component.UnitIcons;
import com.talhanation.recruits.client.gui.widgets.PanelList;
import com.talhanation.recruits.client.gui.widgets.ScrollDropDownMenu;
import com.talhanation.recruits.client.gui.widgets.SelectedClaimWidget;
import com.talhanation.recruits.governor.ClaimGovernorData;
import com.talhanation.recruits.governor.GovernorPost;
import com.talhanation.recruits.governor.GovernorUnitType;
import com.talhanation.recruits.network.*;
import com.talhanation.recruits.world.NobleReputation;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsFaction;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
    /** Top of the tab content. */
    private static final int CONTENT = HEADER + 34;
    private static final int TEXT = 0x404040;
    private static final int RED = 0xB02020;
    private static final int GREEN = 0x207020;
    private static final int MAP_SIZE = 140;
    private static final int LIST_WIDTH = WIDTH - 16 - 10;

    private static Tab lastTab = Tab.INFO;

    private GovernorSnapshot snapshot;
    private Tab tab;
    @Nullable
    private RecruitsGroup selectedGroup;
    @Nullable
    private ScrollDropDownMenu<RecruitsGroup> groupDropDown;
    @Nullable
    private PanelList<UnitEntry> unitList;
    @Nullable
    private PanelList<PostEntry> postList;
    /** Scroll positions survive the rebuild after every snapshot. */
    private double unitScroll;
    private double postScroll;
    @Nullable
    private ClaimMapView mapView;

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

    public GovernorSnapshot getSnapshot() {
        return snapshot;
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

    @Override
    public void removed() {
        super.removed();
        if (mapView != null) {
            mapView.close();
            mapView = null;
        }
    }

    private void setTab(Tab tab) {
        this.tab = tab;
        lastTab = tab;
        rebuild();
    }

    private void rebuild() {
        if (unitList != null) unitScroll = unitList.getScrollAmount();
        if (postList != null) postScroll = postList.getScrollAmount();
        clearWidgets();
        groupDropDown = null;
        unitList = null;
        postList = null;
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

    private int budgetY() {
        return guiTop + CONTENT;
    }

    private int unitListTop() {
        return guiTop + CONTENT + 54;
    }

    private int unitListBottom() {
        return guiTop + HEIGHT - 28;
    }

    private void buildGarrison() {
        // 1. budget first
        int budgetY = budgetY();
        int bx = guiLeft + 110;
        addRenderableWidget(new ExtendedButton(bx, budgetY, 24, 14, Component.literal("-10"), b -> sendBudget(snapshot.budget - 10)));
        addRenderableWidget(new ExtendedButton(bx + 26, budgetY, 18, 14, Component.literal("-1"), b -> sendBudget(snapshot.budget - 1)));
        addRenderableWidget(new ExtendedButton(bx + 82, budgetY, 18, 14, Component.literal("+1"), b -> sendBudget(snapshot.budget + 1)));
        addRenderableWidget(new ExtendedButton(bx + 102, budgetY, 24, 14, Component.literal("+10"), b -> sendBudget(snapshot.budget + 10)));

        // 2. the unit types as a list
        int top = unitListTop();
        unitList = new PanelList<>(guiLeft + 8, top, LIST_WIDTH, unitListBottom() - top, 34);
        List<UnitEntry> entries = new ArrayList<>();
        for (GovernorUnitType type : GovernorUnitType.values()) entries.add(new UnitEntry(type));
        unitList.setEntries(entries);
        unitList.setScrollAmount(unitScroll);
        addRenderableWidget(unitList);

        // 3. hand over a group
        int groupY = guiTop + HEIGHT - 22;
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

    private void sendBudget(int budget) {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorSetBudget(snapshot.claim, Math.max(0, budget)));
    }

    private void openUnitEdit(GovernorUnitType type) {
        if (minecraft == null) return;
        minecraft.setScreen(new GovernorUnitEditScreen(this, snapshot, type));
    }

    private static ItemStack stackOf(@Nullable String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(location);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /** One unit type: icon, name, equipment template, present / wanted, Edit. */
    private class UnitEntry extends PanelList.Entry<UnitEntry> {
        private final GovernorUnitType type;
        private final ExtendedButton edit;

        UnitEntry(GovernorUnitType type) {
            this.type = type;
            this.edit = new ExtendedButton(0, 0, 56, 16, Component.translatable("gui.recruits.governor.unit.edit"), b -> openUnitEdit(type));
            this.edit.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.unit_edit", type.getDisplayName())));
            this.children.add(edit);
        }

        @Override
        protected PanelList<UnitEntry> getList() {
            return unitList;
        }

        @Override
        protected void renderContent(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            UnitIcons.render(g, UnitIcons.of(type), left + 4, top + (height - UnitIcons.SIZE) / 2);
            g.drawString(font, type.getDisplayName(), left + 30, top + 3, PanelList.TEXT_COLOR, false);

            boolean any = false;
            for (int slot = 0; slot < ClaimGovernorData.LOADOUT_SLOTS; slot++) {
                ItemStack stack = stackOf(snapshot.loadout[type.getIndex()][slot]);
                int ix = left + 30 + slot * 17;
                int iy = top + 13;
                if (stack.isEmpty()) {
                    g.fill(ix + 3, iy + 3, ix + 13, iy + 13, 0x40FFFFFF);
                } else {
                    g.renderItem(stack, ix, iy);
                    any = true;
                }
            }
            if (!any) {
                g.drawString(font, Component.translatable("gui.recruits.governor.unit.no_template"), left + 30 + 6 * 17 + 4, top + 17, PanelList.TEXT_GRAY, false);
            }

            int present = snapshot.counts[type.getIndex()];
            int wanted = snapshot.quota[type.getIndex()];
            String count = present + " / " + wanted;
            int countX = left + width - 66 - font.width(count);
            g.drawString(font, count, countX, top + (height - 8) / 2, present < wanted ? PanelList.TEXT_RED : PanelList.TEXT_COLOR, false);

            edit.setX(left + width - 60);
            edit.setY(top + (height - 16) / 2);
        }
    }

    // ------------------------------------------------------------------ order tab

    private List<GovernorPost> sortedPosts() {
        List<GovernorPost> posts = new ArrayList<>(snapshot.posts);
        posts.sort(Comparator.comparingInt((GovernorPost p) -> p.getType().getIndex()).thenComparingInt(GovernorPost::getNumber));
        return posts;
    }

    private void buildOrder() {
        int top = guiTop + CONTENT + 2;
        postList = new PanelList<>(guiLeft + 8, top, LIST_WIDTH, guiTop + HEIGHT - 8 - top, 24);
        List<PostEntry> entries = new ArrayList<>();
        for (GovernorPost post : sortedPosts()) entries.add(new PostEntry(post));
        postList.setEntries(entries);
        postList.setScrollAmount(postScroll);
        addRenderableWidget(postList);
    }

    /** One post: colour of its type, name and position, who holds it, delete. */
    private class PostEntry extends PanelList.Entry<PostEntry> {
        private final GovernorPost post;
        private final ExtendedButton delete;

        PostEntry(GovernorPost post) {
            this.post = post;
            this.delete = new ExtendedButton(0, 0, 14, 14, Component.literal("x"),
                    b -> Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorRemovePost(snapshot.claim, post.getId())));
            this.delete.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.delete_post")));
            this.children.add(delete);
        }

        @Override
        protected PanelList<PostEntry> getList() {
            return postList;
        }

        @Override
        protected void renderContent(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int color = GovernorCategory.colorOf(post.getType());
            g.fill(left + 4, top + 4, left + 16, top + height - 4, 0xFF000000);
            g.fill(left + 5, top + 5, left + 15, top + height - 5, 0xFF000000 | color);

            int textY = top + (height - 8) / 2;
            g.drawString(font, post.getLabel(), left + 22, textY, PanelList.TEXT_COLOR, false);
            g.drawString(font, post.getCoordinateString(), left + 100, textY, PanelList.TEXT_GRAY, false);

            if (post.getType().isGuardPost()) {
                String assigned = snapshot.assignedNames.get(post.getId());
                String text = assigned != null ? assigned : Component.translatable("gui.recruits.governor.order.unassigned").getString();
                text = font.plainSubstrByWidth(text, width - 190);
                g.drawString(font, text, left + 180, textY, assigned != null ? PanelList.TEXT_GREEN : PanelList.TEXT_RED, false);
            }

            delete.setX(left + width - 18);
            delete.setY(top + (height - 14) / 2);
        }
    }

    // ------------------------------------------------------------------ village tab

    private int villageTaxY() {
        return guiTop + CONTENT + 62;
    }

    private int villageQuestY() {
        return guiTop + CONTENT + 132;
    }

    private void buildVillage() {
        int top = villageTaxY();
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

        int questTop = villageQuestY();
        for (int mode = 0; mode < ClaimGovernorData.QUEST_MODES; mode++) {
            int questMode = mode;
            ExtendedButton button = new ExtendedButton(guiLeft + 10 + mode * 74, questTop, 70, 18,
                    Component.translatable("gui.recruits.governor.quest_mode." + mode), b ->
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorSetQuestMode(snapshot.claim, questMode)));
            button.active = snapshot.questMode != mode;
            button.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.quest_mode." + mode)));
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
            case INFO -> renderInfo(guiGraphics, mouseX, mouseY);
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
        List<FormattedCharSequence> lines = font.split(Component.translatable("gui.recruits.governor.office.hint"), WIDTH - 16);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x, y + 16 + i * 10, TEXT, false);
        }
    }

    private void line(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }

    /** Draws the text cut to the width; returns the next y. */
    private int fitLine(GuiGraphics g, Component text, int x, int y, int maxWidth, int color) {
        String s = text.getString();
        if (font.width(s) > maxWidth) s = font.plainSubstrByWidth(s, maxWidth - font.width("…")) + "…";
        g.drawString(font, s, x, y, color, false);
        return y + 12;
    }

    /** Draws the text wrapped to the width; returns the next y. */
    private int wrapLines(GuiGraphics g, Component text, int x, int y, int maxWidth, int color, int maxLines) {
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            g.drawString(font, lines.get(i), x, y, color, false);
            y += 10;
        }
        return y + 2;
    }

    private Component garrisonLimitText() {
        int bonus = snapshot.cap - snapshot.capBase;
        if (bonus > 0) {
            return Component.translatable("gui.recruits.governor.info.cap_bonus", snapshot.cap, snapshot.capBase, bonus, snapshot.governorLevel);
        }
        return Component.translatable("gui.recruits.governor.info.cap_next", snapshot.cap, snapshot.bonusFromLevel, snapshot.bonusPerLevel);
    }

    private Component recruitStatusText() {
        String key = snapshot.recruitStatus == null || snapshot.recruitStatus.isEmpty() ? "idle" : snapshot.recruitStatus;
        Object arg = snapshot.recruitStatusArg;
        if (key.equals("hiring") || key.equals("hired")) {
            // the server sends the unit type index, the name is translated here
            try {
                arg = GovernorUnitType.fromIndex(Integer.parseInt(snapshot.recruitStatusArg)).getDisplayName();
            } catch (NumberFormatException ignored) {
            }
        }
        return Component.translatable("gui.recruits.governor.recruit_status." + key, arg);
    }

    private int recruitStatusColor() {
        return switch (snapshot.recruitStatus == null ? "" : snapshot.recruitStatus) {
            case "hiring", "hired", "quota_met" -> GREEN;
            case "idle", "no_quota" -> TEXT;
            default -> RED;
        };
    }

    private void renderInfo(GuiGraphics g, int mouseX, int mouseY) {
        int x = guiLeft + 8;
        int y = guiTop + CONTENT + 2;
        int textWidth = WIDTH - 16 - MAP_SIZE - 8;

        y = fitLine(g, Component.translatable("gui.recruits.governor.info.claim", snapshot.claimName, snapshot.chunkCount), x, y, textWidth, TEXT);
        y = fitLine(g, Component.translatable("gui.recruits.governor.info.owner", snapshot.ownerName), x, y, textWidth, TEXT);
        y = fitLine(g, Component.translatable("gui.recruits.governor.info.faction", snapshot.factionName), x, y, textWidth, TEXT);
        Component status;
        if (snapshot.ruling && snapshot.self != null && !snapshot.selfInClaim) status = Component.translatable("gui.recruits.governor.status.travelling");
        else if (snapshot.ruling) status = Component.translatable("gui.recruits.governor.status.ruling");
        else status = Component.translatable("gui.recruits.governor.status." + snapshot.status);
        y = fitLine(g, Component.translatable("gui.recruits.governor.info.governor", snapshot.governorName, status), x, y, textWidth, snapshot.ruling ? GREEN : RED);
        if (snapshot.governorLevel > 0) y = fitLine(g, Component.translatable("gui.recruits.governor.info.level", snapshot.governorLevel), x, y, textWidth, TEXT);
        y = fitLine(g, Component.translatable("gui.recruits.governor.info.garrison", snapshot.garrisonTotal, snapshot.getTotalQuota(), snapshot.cap), x, y, textWidth, TEXT);
        y = wrapLines(g, garrisonLimitText(), x, y, textWidth, 0x606060, 2);

        Component coins;
        if (!snapshot.hasTreasury) coins = Component.translatable("gui.recruits.governor.info.no_treasury");
        else if (snapshot.treasuryMissing) coins = Component.translatable("gui.recruits.governor.info.treasury_missing");
        else if (snapshot.coins < 0) coins = Component.literal("?");
        else coins = Component.literal(String.valueOf(snapshot.coins));
        y = fitLine(g, Component.translatable("gui.recruits.governor.info.treasury", coins), x, y, textWidth, TEXT);
        y = fitLine(g, Component.translatable("gui.recruits.governor.info.spent", snapshot.spentToday, snapshot.budget), x, y, textWidth, TEXT);
        if (snapshot.purse > 0) y = fitLine(g, Component.translatable("gui.recruits.governor.info.purse", snapshot.purse), x, y, textWidth, TEXT);
        y = wrapLines(g, recruitStatusText(), x, y + 2, textWidth, recruitStatusColor(), 3);

        y += 2;
        if (!snapshot.hasTreasury || snapshot.treasuryMissing) y = wrapLines(g, Component.translatable("gui.recruits.governor.warn.treasury"), x, y, textWidth, RED, 2);
        if (snapshot.upkeepMissing) y = wrapLines(g, Component.translatable("gui.recruits.governor.warn.upkeep"), x, y, textWidth, RED, 2);
        if (snapshot.underSiege) y = wrapLines(g, Component.translatable("gui.recruits.governor.warn.siege"), x, y, textWidth, RED, 2);
        if (!snapshot.ruling) wrapLines(g, Component.translatable("gui.recruits.governor.warn.not_ruling"), x, y, textWidth, RED, 2);

        if (mapView == null) mapView = new ClaimMapView();
        int mapX = guiLeft + WIDTH - 8 - MAP_SIZE;
        int mapY = guiTop + CONTENT + 2;
        GovernorPost hovered = mapView.render(g, font, mapX, mapY, MAP_SIZE, MAP_SIZE, snapshot.chunks, snapshot.posts, mouseX, mouseY);
        renderMapLegend(g, mapX, mapY + MAP_SIZE + 4);
        if (hovered != null) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(hovered.getLabel());
            tooltip.add(Component.literal(hovered.getCoordinateString()).withStyle(ChatFormatting.GRAY));
            String assigned = snapshot.assignedNames.get(hovered.getId());
            if (assigned != null) tooltip.add(Component.literal(assigned).withStyle(ChatFormatting.GREEN));
            g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    /** Colours of the post types under the map, two columns. */
    private void renderMapLegend(GuiGraphics g, int x, int y) {
        GovernorPost.Type[] types = GovernorPost.Type.values();
        for (int i = 0; i < types.length; i++) {
            int lx = x + (i % 2) * (MAP_SIZE / 2);
            int ly = y + (i / 2) * 10;
            g.fill(lx, ly + 1, lx + 6, ly + 7, 0xFF000000 | GovernorCategory.colorOf(types[i]));
            Component name = Component.translatable("gui.recruits.governor.post." + types[i].name().toLowerCase(java.util.Locale.ROOT));
            g.drawString(font, font.plainSubstrByWidth(name.getString(), MAP_SIZE / 2 - 10), lx + 9, ly, TEXT, false);
        }
    }

    private void renderGarrison(GuiGraphics g) {
        int left = guiLeft + 8;
        int budgetY = budgetY();
        line(g, Component.translatable("gui.recruits.governor.budget"), left, budgetY + 3, TEXT);
        g.drawCenteredString(font, String.valueOf(snapshot.budget), guiLeft + 110 + 63, budgetY + 3, 0xFFFFFF);
        line(g, Component.translatable("gui.recruits.governor.garrison.spent", snapshot.spentToday), guiLeft + 242, budgetY + 3, TEXT);

        int y = budgetY + 20;
        line(g, Component.translatable("gui.recruits.governor.garrison.header", snapshot.garrisonTotal, snapshot.getTotalQuota(), snapshot.cap), left, y, TEXT);
        fitLine(g, recruitStatusText(), left, y + 12, WIDTH - 16, recruitStatusColor());
        fitLine(g, garrisonLimitText(), left, y + 24, WIDTH - 16, 0x606060);

        line(g, Component.translatable("gui.recruits.governor.assign"), left, guiTop + HEIGHT - 19, TEXT);
    }

    private void renderOrder(GuiGraphics g) {
        if (snapshot.posts.isEmpty()) {
            List<FormattedCharSequence> lines = font.split(Component.translatable("gui.recruits.governor.order.empty"), WIDTH - 32);
            int y = guiTop + CONTENT + 20;
            for (FormattedCharSequence lineText : lines) {
                g.drawCenteredString(font, lineText, guiLeft + WIDTH / 2, y, 0xFFFFFF);
                y += 10;
            }
        }
    }

    private void renderVillage(GuiGraphics g) {
        int x = guiLeft + 8;
        int y = guiTop + CONTENT + 2;
        Component villages = snapshot.villages > 1
                ? Component.translatable("gui.recruits.governor.village.inhabitants_villages", snapshot.inhabitants, snapshot.villages)
                : Component.translatable("gui.recruits.governor.village.inhabitants", snapshot.inhabitants);
        line(g, villages, x, y, TEXT);
        line(g, Component.translatable("gui.recruits.governor.village.tax_level",
                Component.translatable("gui.recruits.governor.tax." + snapshot.taxLevel)), x, y + 12, TEXT);
        line(g, Component.translatable("gui.recruits.governor.village.tax_today", snapshot.taxToday), x, y + 24, TEXT);
        line(g, Component.translatable("gui.recruits.governor.village.expected",
                (snapshot.inhabitants / 10) * snapshot.taxLevel * snapshot.taxBase), x, y + 36, TEXT);
        line(g, Component.translatable("gui.recruits.governor.village.taxes"), x, villageTaxY() - 12, TEXT);
        line(g, Component.translatable("gui.recruits.governor.village.note").withStyle(ChatFormatting.ITALIC), x, villageTaxY() + 22, TEXT);

        // reputation and quests
        int ry = villageTaxY() + 38;
        if (snapshot.repActive) {
            NobleReputation.Tier tier = NobleReputation.Tier.values()[Math.max(0, Math.min(NobleReputation.Tier.values().length - 1, snapshot.repTier))];
            line(g, Component.translatable("gui.recruits.governor.village.reputation", NobleReputation.format(snapshot.reputation),
                    tier.getName().copy().withStyle(tier.getColor())), x, ry, TEXT);
        }
        line(g, Component.translatable("gui.recruits.governor.village.quests"), x, villageQuestY() - 12, TEXT);

        int qy = villageQuestY() + 24;
        if (snapshot.questText != null) {
            Component progress = snapshot.questDone
                    ? Component.translatable("gui.recruits.governor.village.quest_done")
                    : Component.translatable("gui.recruits.governor.village.quest_progress", snapshot.questProgress, snapshot.questCount);
            qy = wrapLines(g, Component.translatable("gui.recruits.governor.village.quest_active", snapshot.questText, progress), x, qy, WIDTH - 16, TEXT, 2);
            if (!snapshot.questHere) wrapLines(g, Component.translatable("gui.recruits.governor.village.quest_elsewhere"), x, qy, WIDTH - 16, 0x707070, 2);
        } else {
            wrapLines(g, Component.translatable("gui.recruits.governor.village.quest_none"), x, qy, WIDTH - 16, 0x707070, 2);
        }
    }
}
