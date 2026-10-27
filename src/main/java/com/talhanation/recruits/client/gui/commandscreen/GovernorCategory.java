package com.talhanation.recruits.client.gui.commandscreen;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.governor.GovernorClientHandler;
import com.talhanation.recruits.client.governor.GovernorSnapshot;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
import com.talhanation.recruits.client.gui.widgets.PanelList;
import com.talhanation.recruits.governor.GovernorPost;
import com.talhanation.recruits.network.MessageGovernorAddPost;
import com.talhanation.recruits.network.MessageGovernorRemovePost;
import com.talhanation.recruits.network.MessageGovernorRequestData;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ExtendedButton;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Command screen tab "Governor": the posts of the current claim on the left (delete with x),
 * the buttons to set new posts by looking at a block on the right. Only available in an own claim
 * with a ruling Governor. While the tab is open, the posts are shown as block outlines with their names
 * ({@link com.talhanation.recruits.client.events.GovernorPostRenderer}).
 */
@OnlyIn(Dist.CLIENT)
public class GovernorCategory implements ICommandCategory {

    private static final MutableComponent TOOLTIP_GOVERNOR = Component.translatable("gui.recruits.command.tooltip.governor");
    private static final int ROW_HEIGHT = 12;
    private static final int ENTRY_HEIGHT = 17;
    private static final int LIST_HEIGHT = 13 * 12;
    /** The post list is slightly transparent. */
    private static final int LIST_ALPHA = 200;
    private static double scroll;
    @Nullable
    private PanelList<PostEntry> postList;

    @Override
    public Component getToolTipName() {
        return TOOLTIP_GOVERNOR;
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.BELL);
    }

    @Nullable
    public static GovernorSnapshot getSnapshot(Player player) {
        RecruitsClaim claim = ClientManager.currentClaim;
        if (claim == null || claim.getPlayerInfo() == null || !player.getUUID().equals(claim.getPlayerInfo().getUUID())) return null;
        return GovernorClientHandler.get(claim.getUUID());
    }

    @Override
    public boolean isAvailable(CommandScreen screen, Player player) {
        GovernorSnapshot snapshot = getSnapshot(player);
        return snapshot != null && snapshot.ruling && player.getCommandSenderWorld().dimension() == Level.OVERWORLD;
    }

    @Override
    public void onOpen(CommandScreen screen, Player player) {
        RecruitsClaim claim = ClientManager.currentClaim;
        if (claim != null && claim.getPlayerInfo() != null && player.getUUID().equals(claim.getPlayerInfo().getUUID())) {
            Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorRequestData(claim.getUUID()));
        }
    }

    @Override
    public void createButtons(CommandScreen screen, int x, int y, List<RecruitsGroup> groups, Player player) {
        GovernorSnapshot snapshot = getSnapshot(player);
        postList = null;
        if (snapshot == null) return;

        // ---------------- post list (left), slightly transparent so the world stays visible
        List<GovernorPost> posts = new ArrayList<>(snapshot.posts);
        posts.sort(Comparator.comparingInt((GovernorPost p) -> p.getType().getIndex()).thenComparingInt(GovernorPost::getNumber));

        int listX = x - 225;
        int listY = y - 75;
        int listW = 170;
        screen.addRenderableWidget(new TextRow(listX, listY, listW + 8, Component.translatable("gui.recruits.governor.posts_of", snapshot.claimName), 0xFFFFD700));

        int top = listY + ROW_HEIGHT + 2;
        postList = new PanelList<>(listX, top, listW, LIST_HEIGHT, ENTRY_HEIGHT);
        postList.setAlpha(LIST_ALPHA);
        postList.setBackgroundColor(0x60000000);
        List<PostEntry> entries = new ArrayList<>();
        for (GovernorPost post : posts) entries.add(new PostEntry(postList, snapshot, post));
        postList.setEntries(entries);
        postList.setScrollAmount(scroll);
        screen.addRenderableWidget(postList);
        if (posts.isEmpty()) {
            screen.addRenderableWidget(new TextRow(listX, top, listW + 8, Component.translatable("gui.recruits.governor.order.none"), 0xFFAAAAAA));
        }

        // ---------------- post buttons (right)
        BlockPos target = screen.rayBlockPos;
        boolean inClaim = target != null && snapshot.chunks.contains(new ChunkPos(target));
        boolean isContainer = inClaim && isContainer(player.getCommandSenderWorld(), target);

        int colA = x + 90;
        int colB = x + 175;
        addPostButton(screen, snapshot, colA, y - 40, GovernorPost.Type.MELEE, target, inClaim);
        addPostButton(screen, snapshot, colB, y - 40, GovernorPost.Type.RANGED, target, inClaim);
        addPostButton(screen, snapshot, colA, y - 10, GovernorPost.Type.RESERVE, target, inClaim);
        addPostButton(screen, snapshot, colB, y - 10, GovernorPost.Type.PATROL, target, inClaim);
        addPostButton(screen, snapshot, colA, y + 20, GovernorPost.Type.TREASURY, target, isContainer);
        addPostButton(screen, snapshot, colB, y + 20, GovernorPost.Type.UPKEEP, target, isContainer);
    }

    private void addPostButton(CommandScreen screen, GovernorSnapshot snapshot, int x, int y, GovernorPost.Type type, @Nullable BlockPos target, boolean active) {
        RecruitsCommandButton button = new RecruitsCommandButton(x, y, type.getDisplayName(),
                press -> {
                    if (target == null) return;
                    BlockPos pos = type.isContainer() ? target : target.above();
                    Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorAddPost(snapshot.claim, type, pos));
                });
        button.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.post." + type.getKey())));
        button.active = active;
        screen.addRenderableWidget(button);
    }

    private static boolean isContainer(Level level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        return entity instanceof Container || level.getBlockState(pos).getBlock() instanceof ChestBlock;
    }

    @Override
    public void tick(CommandScreen screen, Player player) {
        // the posts are drawn as block outlines by GovernorPostRenderer while this tab is open
        if (postList != null) scroll = postList.getScrollAmount();
    }

    @Override
    public boolean mouseScrolled(CommandScreen screen, double mouseX, double mouseY, double delta) {
        if (postList == null || !postList.isMouseOver(mouseX, mouseY)) return false;
        postList.mouseScrolled(mouseX, mouseY, delta);
        scroll = postList.getScrollAmount();
        return true;
    }

    public static int colorOf(GovernorPost.Type type) {
        return switch (type) {
            case MELEE -> 0xFFE04040;
            case RANGED -> 0xFF40C040;
            case RESERVE -> 0xFF4080FF;
            case PATROL -> 0xFFFFFF40;
            case TREASURY -> 0xFFFFB000;
            case UPKEEP -> 0xFFB06030;
        };
    }

    /** One post of the list: colour of the type, name, position, who holds it, delete. */
    private static class PostEntry extends PanelList.Entry<PostEntry> {
        private final PanelList<PostEntry> list;
        private final GovernorPost post;
        private final GovernorSnapshot snapshot;
        private final ExtendedButton delete;

        PostEntry(PanelList<PostEntry> list, GovernorSnapshot snapshot, GovernorPost post) {
            this.list = list;
            this.post = post;
            this.snapshot = snapshot;
            this.delete = new ExtendedButton(0, 0, 11, 11, Component.literal("x"),
                    button -> Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorRemovePost(snapshot.claim, post.getId())));
            this.delete.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.delete_post")));
            this.children.add(delete);
        }

        @Override
        protected PanelList<PostEntry> getList() {
            return list;
        }

        @Override
        protected void renderContent(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            var font = Minecraft.getInstance().font;
            int textY = top + (height - 8) / 2;
            g.fill(left + 2, top + 2, left + 8, top + height - 2, list.withAlpha(0xFF000000 | colorOf(post.getType())));
            String text = post.getLabel().getString() + " " + post.getCoordinateString();
            g.drawString(font, font.plainSubstrByWidth(text, width - 26), left + 11, textY, list.withAlpha(PanelList.TEXT_COLOR), false);
            if (hovered && post.getType().isGuardPost()) {
                String assigned = snapshot.assignedNames.get(post.getId());
                Component holder = assigned != null ? Component.literal(assigned) : Component.translatable("gui.recruits.governor.order.unassigned");
                // deferred: the list clips everything drawn inside it
                //if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.set(holder.getSiblings());
            }
            delete.setX(left + width - 13);
            delete.setY(top + (height - 11) / 2);
        }
    }

    /** Non-interactive text line with a dark background, used for the post list. */
    private static class TextRow extends AbstractWidget {
        private final int color;

        TextRow(int x, int y, int width, Component text, int color) {
            super(x, y, width, ROW_HEIGHT - 1, text);
            this.color = color;
            this.active = false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0xA0000000);
            guiGraphics.drawString(Minecraft.getInstance().font, getMessage(), getX() + 3, getY() + 2, color, false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
