package com.talhanation.recruits.client.gui.governor;

import com.google.common.collect.Lists;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsClaim;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** List of the player's own claims (claims synced to the client whose owner is the player). */
@OnlyIn(Dist.CLIENT)
public class ClaimsList extends ListScreenListBase<RecruitsClaimEntry> {

    protected final IClaimSelection screen;
    protected final List<RecruitsClaimEntry> entries = Lists.newArrayList();
    protected final Player player;
    protected String filter = "";

    public ClaimsList(int width, int height, int x, int y, int size, IClaimSelection screen, Player player) {
        super(width, height, x, y, size);
        this.screen = screen;
        this.player = player;
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);
        updateEntryList();
    }

    public void updateEntryList() {
        entries.clear();
        for (RecruitsClaim claim : ClientManager.recruitsClaims) {
            if (claim == null || claim.isRemoved || claim.getPlayerInfo() == null) continue;
            if (!player.getUUID().equals(claim.getPlayerInfo().getUUID())) continue;
            entries.add(new RecruitsClaimEntry(screen, claim));
        }
        updateFilter();
    }

    public void updateFilter() {
        clearEntries();
        List<RecruitsClaimEntry> filtered = new ArrayList<>(entries);
        if (!filter.isEmpty()) {
            filtered.removeIf(entry -> !entry.getClaim().getName().toLowerCase(Locale.ROOT).contains(filter));
        }
        // own Governor first, then free claims, then taken ones; by name inside each block
        filtered.sort(Comparator.comparingInt((RecruitsClaimEntry e) -> switch (screen.getStatus(e.getClaim())) {
                    case CURRENT -> 0;
                    case FREE -> 1;
                    case TAKEN -> 2;
                })
                .thenComparing(e -> e.getClaim().getName(), String.CASE_INSENSITIVE_ORDER));
        replaceEntries(filtered);
    }

    public void setFilter(String filter) {
        this.filter = filter;
        updateFilter();
    }

    public boolean isEmpty() {
        return children().isEmpty();
    }
}
