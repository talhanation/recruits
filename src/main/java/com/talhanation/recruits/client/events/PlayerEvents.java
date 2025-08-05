package com.talhanation.recruits.client.events;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.overlay.FactionClaimBannerOverlay;
import com.talhanation.recruits.client.gui.overlay.FactionClaimSiegeOverlay;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsClaimManager;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerEvents {

    private static ChunkPos lastPlayerChunk = null;
    private static RecruitsClaim currentClaim = null;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || event.phase != TickEvent.Phase.END) return;

        ChunkPos currentChunk = player.chunkPosition();

        if (!currentChunk.equals(lastPlayerChunk)) {
            lastPlayerChunk = currentChunk;

            RecruitsClaim claim = RecruitsClaimManager.getClaimAt(currentChunk, ClientManager.recruitsClaims);
            if (claim != null) {
                if (currentClaim != null && currentClaim.equals(claim)) return;
                currentClaim = claim;

                if (claim.isUnderSiege) {
                    /*
                    RecruitsTeam defender = claim.getOwnerFaction();
                    RecruitsTeam attacker = claim.attackingParties.get(0);

                    float healthRatio = (float) claim.getHealth() / (float) claim.getMaxHealth();

                    FactionClaimSiegeOverlay.display(claim.getName(), defender, attacker, healthRatio);

                     */
                } else {
                    FactionClaimBannerOverlay.display(claim.getOwnerFaction(), claim.getName());
                }
            } else {
                currentClaim = null;
            }
        }
    }

    @SubscribeEvent
    public void onRenderGui(RenderGuiOverlayEvent event) {
        FactionClaimBannerOverlay.renderOverlay(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth());
        //FactionClaimSiegeOverlay.renderOverlay(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth());
    }
}