package com.talhanation.recruits.client.api;

import com.talhanation.recruits.client.gui.overlay.ClaimOverlayManager;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;


@OnlyIn(Dist.CLIENT)
public abstract class ClientOverlayEvent extends Event {

    @Nullable
    private final RecruitsClaim claim;
    private final ClaimOverlayManager.OverlayState state;
    private final float alpha;

    protected ClientOverlayEvent(@Nullable RecruitsClaim claim, ClaimOverlayManager.OverlayState state, float alpha) {
        this.claim = claim;
        this.state = state;
        this.alpha = alpha;
    }

    @Nullable
    public RecruitsClaim getClaim() { return claim; }

    public ClaimOverlayManager.OverlayState getState() { return state; }

    public float getAlpha() { return alpha; }


    @Cancelable
    public static class RenderPre extends ClientOverlayEvent {
        private final GuiGraphics guiGraphics;

        public RenderPre(GuiGraphics guiGraphics,
                         @Nullable RecruitsClaim claim,
                         ClaimOverlayManager.OverlayState state,
                         float alpha) {
            super(claim, state, alpha);
            this.guiGraphics = guiGraphics;
        }

        public GuiGraphics getGuiGraphics() { return guiGraphics; }
    }


    public static class RenderPost extends ClientOverlayEvent {
        private final GuiGraphics guiGraphics;

        public RenderPost(GuiGraphics guiGraphics, @Nullable RecruitsClaim claim, ClaimOverlayManager.OverlayState state, float alpha) {
            super(claim, state, alpha);
            this.guiGraphics = guiGraphics;
        }

        public GuiGraphics getGuiGraphics(){
            return guiGraphics;
        }
    }

    @Cancelable
    public static class StateChanged extends ClientOverlayEvent {
        private final ClaimOverlayManager.OverlayState previousState;

        public StateChanged(@Nullable RecruitsClaim claim, ClaimOverlayManager.OverlayState previousState, ClaimOverlayManager.OverlayState newState, float alpha) {
            super(claim, newState, alpha);
            this.previousState = previousState;
        }

        public ClaimOverlayManager.OverlayState getPreviousState(){
            return previousState;
        }

        public ClaimOverlayManager.OverlayState getNewState(){
            return getState();
        }
    }
}
