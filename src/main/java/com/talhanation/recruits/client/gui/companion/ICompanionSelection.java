package com.talhanation.recruits.client.gui.companion;

import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.CompanionRecord;

import javax.annotation.Nullable;
import java.util.UUID;

public interface ICompanionSelection {
    @Nullable
    CompanionRecord getSelected();

    ListScreenListBase<RecruitsCompanionEntry> getCompanionList();

    /** The companion orders are being written for right now, see {@code CompanionListScreen}. */
    @Nullable
    UUID getOrdersCompanion();
}
