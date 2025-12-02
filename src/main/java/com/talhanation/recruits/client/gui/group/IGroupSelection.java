package com.talhanation.recruits.client.gui.group;

import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsGroup;


public interface IGroupSelection {
    RecruitsGroup getSelected();
    ListScreenListBase<RecruitsGroupEntry> getGroupList();
}