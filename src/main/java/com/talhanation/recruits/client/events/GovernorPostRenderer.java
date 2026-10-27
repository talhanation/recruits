package com.talhanation.recruits.client.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.talhanation.recruits.client.governor.GovernorSnapshot;
import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.commandscreen.GovernorCategory;
import com.talhanation.recruits.governor.GovernorPost;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

/**
 * While the Governor tab of the command screen is open, every post of the claim is shown as a coloured
 * outline of the block the recruit stands in (for treasury and upkeep: the block above the chest),
 * with the post name floating above it. Replaces the particles.
 */
@OnlyIn(Dist.CLIENT)
public class GovernorPostRenderer {

    private static final double MAX_DISTANCE = 64D;
    private static final double LABEL_DISTANCE = 32D;

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !(minecraft.screen instanceof CommandScreen screen)
                || !(screen.getCurrentCategory() instanceof GovernorCategory)) return;
        GovernorSnapshot snapshot = GovernorCategory.getSnapshot(player);
        if (snapshot == null || snapshot.posts.isEmpty()) return;

        PoseStack pose = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (GovernorPost post : snapshot.posts) {
            BlockPos pos = getMarkedBlock(post);
            if (Vec3.atCenterOf(pos).distanceToSqr(cam) > MAX_DISTANCE * MAX_DISTANCE) continue;
            int color = GovernorCategory.colorOf(post.getType());
            float r = ((color >> 16) & 0xFF) / 255F;
            float g = ((color >> 8) & 0xFF) / 255F;
            float b = (color & 0xFF) / 255F;
            AABB box = new AABB(pos).deflate(0.02D).move(-cam.x, -cam.y, -cam.z);
            LevelRenderer.renderLineBox(pose, lines, box, r, g, b, 1.0F);
            // a smaller second box makes the outline easier to see from afar
            LevelRenderer.renderLineBox(pose, lines, box.deflate(0.06D), r, g, b, 0.6F);
        }
        buffers.endBatch(RenderType.lines());

        Font font = minecraft.font;
        for (GovernorPost post : snapshot.posts) {
            BlockPos pos = getMarkedBlock(post);
            Vec3 labelPos = Vec3.atBottomCenterOf(pos).add(0, 1.25D, 0);
            if (labelPos.distanceToSqr(cam) > LABEL_DISTANCE * LABEL_DISTANCE) continue;
            Component text = post.getLabel();
            String assigned = snapshot.assignedNames.get(post.getId());
            renderLabel(pose, buffers, font, minecraft, text, labelPos, cam, GovernorCategory.colorOf(post.getType()), 0);
            if (assigned != null) renderLabel(pose, buffers, font, minecraft, Component.literal(assigned), labelPos, cam, 0xFFFFFFFF, 10);
        }
        buffers.endBatch();
    }

    /** The air block above the position the post was set on: where the recruit stands, or above the chest. */
    public static BlockPos getMarkedBlock(GovernorPost post) {
        return post.getType().isContainer() ? post.getPos().above() : post.getPos();
    }

    private static void renderLabel(PoseStack pose, MultiBufferSource buffers, Font font, Minecraft minecraft, Component text,
                                    Vec3 pos, Vec3 cam, int color, int lineOffset) {
        pose.pushPose();
        pose.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix = pose.last().pose();
        float x = -font.width(text) / 2F;
        int background = (int) (minecraft.options.getBackgroundOpacity(0.25F) * 255F) << 24;
        font.drawInBatch(text, x, lineOffset, 0x40FFFFFF, false, matrix, buffers, Font.DisplayMode.SEE_THROUGH, background, 0xF000F0);
        font.drawInBatch(text, x, lineOffset, 0xFF000000 | color, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        pose.popPose();
    }
}
