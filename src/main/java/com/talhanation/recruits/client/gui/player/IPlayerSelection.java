package com.talhanation.recruits.client.gui.player;

import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;

public interface IPlayerSelection {
    RecruitsPlayerEntry getSelected();
    ListScreenListBase<RecruitsPlayerEntry> getPlayerList();


}
