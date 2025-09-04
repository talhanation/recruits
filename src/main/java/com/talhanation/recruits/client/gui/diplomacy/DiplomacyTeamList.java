package com.talhanation.recruits.client.gui.diplomacy;

import com.google.common.collect.Lists;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsFaction;

import java.util.*;

public class DiplomacyTeamList extends ListScreenListBase<DiplomacyTeamEntry> {

    protected DiplomacyTeamListScreen screen;
    protected final List<DiplomacyTeamEntry> entries;
    protected String filter;
    public DiplomacyFilter diplomacyFilter;

    public DiplomacyTeamList(int width, int height, int x, int y, int size, DiplomacyTeamListScreen screen) {
        super(width, height, x, y, size);
        this.screen = screen;
        this.entries = Lists.newArrayList();
        this.filter = "";
        this.diplomacyFilter = DiplomacyFilter.ALL;

        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);
    }

    public void tick() {
        if (ClientManager.factions != null && ClientManager.diplomacyMap != null) {
            updateEntryList();
        }
    }

    public void updateEntryList() {
        entries.clear();

        for (RecruitsFaction team : ClientManager.factions) {
            if (ClientManager.ownFaction != null && !team.getStringID().equals(ClientManager.ownFaction.getStringID())) {
                RecruitsDiplomacyManager.DiplomacyStatus status = ClientManager.getRelation(ClientManager.ownFaction.getStringID(), team.getStringID());

                switch (diplomacyFilter) {
                    case ALL -> {
                        entries.add(new DiplomacyTeamEntry(screen, team, status));
                    }
                    case ALLIES -> {
                        if (status == RecruitsDiplomacyManager.DiplomacyStatus.ALLY) {
                            entries.add(new DiplomacyTeamEntry(screen, team, status));
                        }
                    }
                    case NEUTRALS -> {
                        if (status == RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL) {
                            entries.add(new DiplomacyTeamEntry(screen, team, status));
                        }
                    }
                    case ENEMIES -> {
                        if (status == RecruitsDiplomacyManager.DiplomacyStatus.ENEMY) {
                            entries.add(new DiplomacyTeamEntry(screen, team, status));
                        }
                    }
                }
            }
        }

        updateFilter();
    }

    public void updateFilter() {
        clearEntries();
        List<DiplomacyTeamEntry> filteredEntries = new ArrayList<>(entries);
        if (!filter.isEmpty()) {
            filteredEntries.removeIf(teamEntry -> {
                return teamEntry.getTeamInfo() == null || !teamEntry.getTeamInfo().getTeamDisplayName().toLowerCase(Locale.ROOT).contains(filter);
            });
        }

        filteredEntries.sort((e1, e2) -> {
            if (!e1.getClass().equals(e2.getClass())) {
                if (e1 instanceof DiplomacyTeamEntry) {
                    return 1;
                } else {
                    return -1;
                }
            }
            return volumeEntryToString(e1).compareToIgnoreCase(volumeEntryToString(e2));
        });

        replaceEntries(filteredEntries);
    }

    private String volumeEntryToString(DiplomacyTeamEntry entry) {
        return entry.getTeamInfo() == null ? "" : entry.getTeamInfo().getStringID();
    }

    public void setFilter(String filter) {
        this.filter = filter;
        updateFilter();
    }

    public boolean isEmpty() {
        return children().isEmpty();
    }

    public enum DiplomacyFilter {
        ALL,
        ALLIES,
        NEUTRALS,
        ENEMIES
    }
}
