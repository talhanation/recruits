package com.talhanation.recruits.client.gui.commandscreen;

import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.governor.GovernorClientHandler;
import com.talhanation.recruits.client.governor.GovernorSnapshot;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.group.RecruitsCommandButton;
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
import net.minecraft.core.particles.DustParticleOptions;
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
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Command screen tab "Governor": the posts of the current claim on the left (delete with x),
 * the buttons to set new posts by looking at a block on the right. Only available in an own claim
 * with a ruling Governor. While the tab is open, the posts are shown with particles.
 */
@OnlyIn(Dist.CLIENT)
public class GovernorCategory implements ICommandCategory {

    private static final MutableComponent TOOLTIP_GOVERNOR = Component.translatable("gui.recruits.command.tooltip.governor");
    private static final int VISIBLE_ROWS = 13;
    private static final int ROW_HEIGHT = 12;
    private static int scroll;

    @Override
    public Component getToolTipName() {
        return TOOLTIP_GOVERNOR;
    }

    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.BELL);
    }

    @Nullable
    private static GovernorSnapshot getSnapshot(Player player) {
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
        if (snapshot == null) return;

        // ---------------- post list (left)
        List<GovernorPost> posts = new ArrayList<>(snapshot.posts);
        posts.sort(Comparator.comparingInt((GovernorPost p) -> p.getType().getIndex()).thenComparingInt(GovernorPost::getNumber));
        scroll = Math.max(0, Math.min(scroll, Math.max(0, posts.size() - VISIBLE_ROWS)));

        int listX = x - 225;
        int listY = y - 75;
        int listW = 170;
        screen.addRenderableWidget(new TextRow(listX, listY, listW, Component.translatable("gui.recruits.governor.posts_of", snapshot.claimName), 0xFFFFD700));

        for (int i = 0; i < VISIBLE_ROWS && scroll + i < posts.size(); i++) {
            GovernorPost post = posts.get(scroll + i);
            int rowY = listY + ROW_HEIGHT + 2 + i * ROW_HEIGHT;
            Component text = Component.literal(post.getLabel().getString() + " " + post.getCoordinateString());
            screen.addRenderableWidget(new TextRow(listX, rowY, listW - 12, text, colorOf(post.getType())));

            ExtendedButton delete = new ExtendedButton(listX + listW - 11, rowY, 11, 11, Component.literal("x"),
                    button -> {
                        Main.SIMPLE_CHANNEL.sendToServer(new MessageGovernorRemovePost(snapshot.claim, post.getId()));
                    });
            delete.setTooltip(Tooltip.create(Component.translatable("gui.recruits.governor.tooltip.delete_post")));
            screen.addRenderableWidget(delete);
        }

        if (posts.size() > VISIBLE_ROWS) {
            ExtendedButton up = new ExtendedButton(listX + listW + 2, listY + ROW_HEIGHT + 2, 12, 12, Component.literal("^"),
                    button -> {
                        scroll = Math.max(0, scroll - 1);
                        screen.refreshButtons();
                    });
            ExtendedButton down = new ExtendedButton(listX + listW + 2, listY + ROW_HEIGHT + 2 + (VISIBLE_ROWS - 1) * ROW_HEIGHT, 12, 12, Component.literal("v"),
                    button -> {
                        scroll = scroll + 1;
                        screen.refreshButtons();
                    });
            screen.addRenderableWidget(up);
            screen.addRenderableWidget(down);
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
        if (player.tickCount % 5 != 0) return;
        GovernorSnapshot snapshot = getSnapshot(player);
        if (snapshot == null) return;
        Level level = player.getCommandSenderWorld();
        for (GovernorPost post : snapshot.posts) {
            BlockPos pos = post.getPos();
            if (pos.distSqr(player.blockPosition()) > 64 * 64) continue;
            int color = colorOf(post.getType());
            Vector3f rgb = new Vector3f(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F);
            double yOffset = post.getType().isContainer() ? 1.2D : 0.2D;
            for (int i = 0; i < 3; i++) {
                level.addParticle(new DustParticleOptions(rgb, 1.2F),
                        pos.getX() + 0.5D, pos.getY() + yOffset + i * 0.5D, pos.getZ() + 0.5D, 0, 0, 0);
            }
        }
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
