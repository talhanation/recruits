package com.talhanation.recruits.client.events;

import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.overlay.FactionClaimBannerOverlay;
import com.talhanation.recruits.client.gui.overlay.FactionClaimSiegeOverlay;
import com.talhanation.recruits.world.RecruitsClaim;
import com.talhanation.recruits.world.RecruitsClaimManager;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class ClientPlayerEvents {

    private State state;
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || event.phase != TickEvent.Phase.END) return;

        ChunkPos currentChunk = player.chunkPosition();
        RecruitsClaim claim = RecruitsClaimManager.getClaimAt(currentChunk, ClientManager.recruitsClaims);

        if(state == null) {
            state = State.NO_CLAIM;
            FactionClaimBannerOverlay.deactivate();
            FactionClaimSiegeOverlay.deactivate();
        }

        switch (state) {
            case NO_CLAIM -> {
                if(claim != null){
                    if(!claim.isUnderSiege) FactionClaimBannerOverlay.activate(claim);
                    state = State.IN_CLAIM;
                }
            }

            case IN_CLAIM -> {
                if(claim != null){
                    if(claim.isUnderSiege){
                        RecruitsFaction defender = claim.getOwnerFaction();
                        RecruitsFaction attacker = claim.attackingParties != null && !claim.attackingParties.isEmpty() ? claim.attackingParties.get(0) : null;

                        FactionClaimBannerOverlay.deactivate();

                        FactionClaimSiegeOverlay.activate(
                                claim.getName(),
                                defender,
                                attacker,
                                claim.getHealth(),
                                claim.getMaxHealth()
                        );
                        state = State.SIEGE;
                    }
                }
                else {
                    FactionClaimBannerOverlay.deactivate();
                    state = State.NO_CLAIM;
                }
            }

            case SIEGE -> {
                if(claim == null){
                    FactionClaimSiegeOverlay.deactivate();
                    state = State.NO_CLAIM;
                }
                else if(!claim.isUnderSiege){
                    FactionClaimBannerOverlay.activate(claim);
                    FactionClaimSiegeOverlay.deactivate();
                    state = State.IN_CLAIM;
                }
                else {
                    FactionClaimSiegeOverlay.update(claim);
                }
            }
        }
    }


    @SubscribeEvent
    public void onRenderGui(RenderGuiOverlayEvent event) {
        FactionClaimBannerOverlay.renderOverlay(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth());
        FactionClaimSiegeOverlay.renderOverlay(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth());
    }

    enum State{
        NO_CLAIM,
        IN_CLAIM,
        SIEGE
    }
}