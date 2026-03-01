package com.talhanation.recruits.client.gui.faction;

import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsFaction;

public interface IFactionSelection {
    RecruitsFaction getSelected();
    ListScreenListBase<RecruitsFactionEntry> getFactionList();


}