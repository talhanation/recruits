package com.talhanation.recruits.client.gui.player;

import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsPlayerInfo;

public interface IPlayerSelection {
    RecruitsPlayerInfo getSelected();
    ListScreenListBase<RecruitsPlayerEntry> getPlayerList();


}
