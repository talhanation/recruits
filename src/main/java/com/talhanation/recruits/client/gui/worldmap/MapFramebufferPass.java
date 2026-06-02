package com.talhanation.recruits.client.gui.worldmap;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

final class MapFramebufferPass implements AutoCloseable {
    private TextureTarget framebuffer;
    private int framebufferWidth = -1;
    private int framebufferHeight = -1;

    Frame begin(GuiGraphics guiGraphics, double offsetX, double offsetZ, double scale, int screenWidth, int screenHeight) {
        guiGraphics.flush();

        Minecraft minecraft = Minecraft.getInstance();
        ensureFramebuffer(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());

        Frame frame = Frame.fromView(offsetX, offsetZ, scale, screenWidth, screenHeight);
        framebuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        framebuffer.bindWrite(true);
        framebuffer.clear(Minecraft.ON_OSX);
        framebuffer.bindWrite(true);
        return frame;
    }

    void endAndBlit(GuiGraphics guiGraphics, Frame frame) {
        guiGraphics.flush();

        Minecraft minecraft = Minecraft.getInstance();
        framebuffer.unbindWrite();
        minecraft.getMainRenderTarget().bindWrite(true);

        framebuffer.bindRead();
        int filter = frame.needsSecondaryFiltering() ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, framebuffer.getColorTextureId());

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale((float) frame.secondaryScale(), (float) frame.secondaryScale(), 1.0F);
        drawFramebuffer(guiGraphics, frame);
        guiGraphics.pose().popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        framebuffer.unbindRead();
    }

    private void ensureFramebuffer(int width, int height) {
        width = Math.max(1, width);
        height = Math.max(1, height);
        if (framebuffer != null && framebufferWidth == width && framebufferHeight == height) return;

        if (framebuffer != null) {
            framebuffer.destroyBuffers();
        }

        framebuffer = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        framebuffer.setFilterMode(GL11.GL_NEAREST);
        framebufferWidth = width;
        framebufferHeight = height;
    }

    private void drawFramebuffer(GuiGraphics guiGraphics, Frame frame) {
        Matrix4f matrix = guiGraphics.pose().last().pose();
        float left = (float) -frame.secondaryOffsetX();
        float top = (float) -frame.secondaryOffsetZ();
        float right = left + frame.screenWidth();
        float bottom = top + frame.screenHeight();

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, left, bottom, 0.0F).uv(0.0F, 0.0F).endVertex();
        buffer.vertex(matrix, right, bottom, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, right, top, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, left, top, 0.0F).uv(0.0F, 1.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    @Override
    public void close() {
        if (framebuffer != null) {
            framebuffer.destroyBuffers();
            framebuffer = null;
        }
        framebufferWidth = -1;
        framebufferHeight = -1;
    }

    record Frame(
            double fboScale,
            double secondaryScale,
            double renderOffsetX,
            double renderOffsetZ,
            double secondaryOffsetX,
            double secondaryOffsetZ,
            double leftWorld,
            double topWorld,
            double rightWorld,
            double bottomWorld,
            int screenWidth,
            int screenHeight
    ) {
        private boolean needsSecondaryFiltering() {
            return Math.abs(secondaryScale - 1.0) > 0.0001;
        }

        private static Frame fromView(double offsetX, double offsetZ, double scale, int screenWidth, int screenHeight) {
            double effectiveScale = Math.max(0.01, scale);
            double fboScale = effectiveScale >= 1.0 ? Math.max(1.0, Math.floor(effectiveScale)) : effectiveScale;
            double secondaryScale = effectiveScale / fboScale;

            double leftWorld = -offsetX / effectiveScale;
            double topWorld = -offsetZ / effectiveScale;
            double rightWorld = leftWorld + screenWidth / effectiveScale;
            double bottomWorld = topWorld + screenHeight / effectiveScale;

            double renderOffsetX;
            double renderOffsetZ;
            double secondaryOffsetX;
            double secondaryOffsetZ;

            if (fboScale < 1.0) {
                double pixelInBlocks = 1.0 / fboScale;
                double anchorWorldX = Math.floor(leftWorld / pixelInBlocks) * pixelInBlocks;
                double anchorWorldZ = Math.floor(topWorld / pixelInBlocks) * pixelInBlocks;
                renderOffsetX = -anchorWorldX * fboScale;
                renderOffsetZ = -anchorWorldZ * fboScale;
                secondaryOffsetX = (leftWorld - anchorWorldX) * fboScale;
                secondaryOffsetZ = (topWorld - anchorWorldZ) * fboScale;
            } else {
                double anchorWorldX = Math.floor(leftWorld);
                double anchorWorldZ = Math.floor(topWorld);
                secondaryOffsetX = (leftWorld - anchorWorldX) * fboScale;
                secondaryOffsetZ = (topWorld - anchorWorldZ) * fboScale;

                int wholeOffsetX = (int) Math.floor(secondaryOffsetX);
                int wholeOffsetZ = (int) Math.floor(secondaryOffsetZ);
                renderOffsetX = -anchorWorldX * fboScale - wholeOffsetX;
                renderOffsetZ = -anchorWorldZ * fboScale - wholeOffsetZ;
                secondaryOffsetX -= wholeOffsetX;
                secondaryOffsetZ -= wholeOffsetZ;
            }

            double padding = 2.0 / effectiveScale;
            return new Frame(
                    fboScale,
                    secondaryScale,
                    renderOffsetX,
                    renderOffsetZ,
                    secondaryOffsetX,
                    secondaryOffsetZ,
                    leftWorld - padding,
                    topWorld - padding,
                    rightWorld + padding,
                    bottomWorld + padding,
                    screenWidth,
                    screenHeight
            );
        }
    }
}
