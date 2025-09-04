package com.talhanation.recruits.client.gui.faction;


import com.google.common.collect.Lists;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsFaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecruitsTeamList extends ListScreenListBase<RecruitsTeamEntry> {

    protected RecruitsTeamListScreen screen;
    protected final List<RecruitsTeamEntry> entries;
    protected String filter;

    public RecruitsTeamList(int width, int height, int x, int y, int size, RecruitsTeamListScreen screen) {
        super(width, height, x, y, size);
        this.screen = screen;
        this.entries = Lists.newArrayList();
        this.filter = "";
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);
    }
    public void tick() {
        if(ClientManager.factions != null){
            updateEntryList();
        }
    }
    public void updateEntryList() {
        entries.clear();

        for (RecruitsFaction team : ClientManager.factions) {
            entries.add(new RecruitsTeamEntry(screen, team));
        }

        updateFilter();
    }

    public void updateFilter() {
        clearEntries();
        List<RecruitsTeamEntry> filteredEntries = new ArrayList<>(entries);
        if (!filter.isEmpty()) {
            filteredEntries.removeIf(teamEntry -> {
                return teamEntry.getTeamInfo() == null || !teamEntry.getTeamInfo().getTeamDisplayName().toLowerCase(Locale.ROOT).contains(filter);
            });
        }

        filteredEntries.sort((e1, e2) -> {
            if (!e1.getClass().equals(e2.getClass())) {
                if (e1 instanceof RecruitsTeamEntry) {
                    return 1;
                } else {
                    return -1;
                }
            }
            return volumeEntryToString(e1).compareToIgnoreCase(volumeEntryToString(e2));
        });

        replaceEntries(filteredEntries);
    }

    private String volumeEntryToString(RecruitsTeamEntry entry) {
        return entry.getTeamInfo() == null ? "" : entry.getTeamInfo().getStringID();
    }

    public void setFilter(String filter) {
        this.filter = filter;
        updateFilter();
    }

    public boolean isEmpty() {
        return children().isEmpty();
    }
}
