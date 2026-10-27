package com.talhanation.recruits.client.governor;

import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.governor.GovernorScreen;
import com.talhanation.recruits.client.gui.governor.GovernorUnitEditScreen;
import com.talhanation.recruits.client.gui.governor.SelectClaimScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps the latest Governor snapshot per claim on the client and refreshes open screens. */
@OnlyIn(Dist.CLIENT)
public final class GovernorClientHandler {

    private static final Map<UUID, GovernorSnapshot> SNAPSHOTS = new HashMap<>();

    private GovernorClientHandler() {
    }

    public static void onData(@Nullable CompoundTag tag, boolean openScreen) {
        if (tag == null || (!tag.hasUUID("claim") && !tag.hasUUID("self"))) return;
        GovernorSnapshot snapshot = GovernorSnapshot.fromNBT(tag);
        if (snapshot.claim != null) SNAPSHOTS.put(snapshot.claim, snapshot);

        Minecraft minecraft = Minecraft.getInstance();
        if (openScreen) {
            if (snapshot.self != null) minecraft.setScreen(new GovernorScreen(snapshot));
        } else if (minecraft.screen instanceof GovernorScreen governorScreen && governorScreen.matches(snapshot)) {
            governorScreen.update(snapshot);
        } else if (minecraft.screen instanceof GovernorUnitEditScreen editScreen && editScreen.getParent().matches(snapshot)) {
            // the edit screen keeps its draft, the Governor screen behind it gets the new data
            editScreen.getParent().update(snapshot);
        } else if (minecraft.screen instanceof SelectClaimScreen claimScreen && claimScreen.getParentScreen().matches(snapshot)) {
            claimScreen.getParentScreen().update(snapshot);
            claimScreen.update(snapshot);
        } else if (minecraft.screen instanceof CommandScreen commandScreen) {
            commandScreen.refreshButtons();
        }
    }

    @Nullable
    public static GovernorSnapshot get(@Nullable UUID claim) {
        return claim == null ? null : SNAPSHOTS.get(claim);
    }

    public static void clear() {
        SNAPSHOTS.clear();
    }
}
