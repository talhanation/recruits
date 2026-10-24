package com.talhanation.recruits.client.gui.governor;

import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsClaim;

import javax.annotation.Nullable;

public interface IClaimSelection {
    @Nullable
    RecruitsClaim getSelected();

    ListScreenListBase<RecruitsClaimEntry> getClaimList();

    /** Status line of a claim in the list (free, governed by …, this Governor). */
    ClaimStatus getStatus(RecruitsClaim claim);

    /** Name of the Governor in office in this claim, "" if none. */
    String getGovernorName(RecruitsClaim claim);

    enum ClaimStatus {
        FREE,
        CURRENT,
        TAKEN
    }
}
