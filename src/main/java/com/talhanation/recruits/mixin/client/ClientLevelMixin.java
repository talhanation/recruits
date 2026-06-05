package com.talhanation.recruits.mixin.client;

import com.talhanation.recruits.client.gui.worldmap.WorldMapTileManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(method = "setBlock", at = @At("RETURN"))
    private void recruits$updateWorldMap(BlockPos pos, BlockState state, int flags, int recursionLeft,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            WorldMapTileManager.getInstance().onBlockUpdated((ClientLevel) (Object) this, pos);
        }
    }
}
