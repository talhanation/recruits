package com.talhanation.recruits.client.events;

import com.talhanation.recruits.ClaimEvents;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.worldmap.claim.WorldMapClaimIndex;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.function.Predicate;

/**
 * Client-side claim blocker. Uses the claims the server already syncs to the client
 * (incl. the breaking/placing/interaction flags and BlockPlacingBreakingOnlyWhenClaimed)
 * to stop the action before it starts: no break animation, no ghost blocks, no door/chest flicker.
 *
 * Purely cosmetic / UX - the server checks in {@link ClaimEvents} stay authoritative.
 * The rules here must mirror the server handlers there.
 */
public class ClientClaimProtectionEvents {

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if(!event.getLevel().isClientSide()) return;

        if(isBlocked(event.getEntity(), event.getLevel(), event.getPos(), RecruitsClaim::isBlockBreakingAllowed)){
            // covers START and CLIENT_HOLD: no mining progress, no break packet
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if(!event.getLevel().isClientSide()) return;

        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if(isAdmin(player)) return;

        // interaction (doors, chests, buttons, ...) - same block list as the server; only inside claims
        RecruitsClaim claim = getClaim(level, pos);
        if(claim != null && !claim.isBlockInteractionAllowed() && !ClaimEvents.isClaimMember(player, claim)
                && ClaimEvents.isProtectedInteractionBlock(level.getBlockState(pos), level.getBlockEntity(pos))){
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        // placing: check where the block would end up
        if(event.getItemStack().getItem() instanceof BlockItem){
            BlockPos target = level.getBlockState(pos).canBeReplaced() ? pos : pos.relative(event.getHitVec().getDirection());
            if(isBlocked(player, level, target, RecruitsClaim::isBlockPlacementAllowed)){
                // only deny the item use, so allowed block interactions still work
                event.setUseItem(Event.Result.DENY);
            }
        }
    }

    /**
     * Same rule as ClaimEvents.onBlockBreakEvent / onBlockPlaceEvent:
     * no claim -> blocked if BlockPlacingBreakingOnlyWhenClaimed,
     * claim    -> blocked if the action isn't allowed there and the player isn't a member.
     */
    private static boolean isBlocked(Player player, Level level, BlockPos pos, Predicate<RecruitsClaim> allowed) {
        if(player == null || isAdmin(player)) return false;

        RecruitsClaim claim = getClaim(level, pos);
        if(claim == null) return ClientManager.configValueBlockPlacingBreakingOnlyWhenClaimed;

        return !allowed.test(claim) && !ClaimEvents.isClaimMember(player, claim);
    }

    private static boolean isAdmin(Player player) {
        return player.isCreative() && player.hasPermissions(2);
    }

    /** Claims only exist in the overworld. */
    private static RecruitsClaim getClaim(Level level, BlockPos pos) {
        if(level.dimension() != Level.OVERWORLD) return null;
        return WorldMapClaimIndex.getClaimAt(new ChunkPos(pos));
    }
}
