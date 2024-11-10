package com.talhanation.recruits.client.gui.diplomacy;

import com.google.common.collect.Lists;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsDiplomacyManager;
import com.talhanation.recruits.world.RecruitsTeam;

import java.util.*;

public class DiplomacyTeamList extends ListScreenListBase<DiplomacyTeamEntry> {

    protected DiplomacyTeamListScreen screen;
    protected final List<DiplomacyTeamEntry> entries;
    protected String filter;
    public static List<RecruitsTeam> teams;
    public static Map<String, Map<String, RecruitsDiplomacyManager.DiplomacyStatus>> diplomacyMap;
    public RecruitsTeam ownTeam;
    public DiplomacyTeamList(int width, int height, int x, int y, int size, DiplomacyTeamListScreen screen) {
        super(width, height, x, y, size);
        this.screen = screen;
        this.entries = Lists.newArrayList();
        this.filter = "";

        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);
    }

    boolean hasUpdated;
    public void tick() {
        if(!hasUpdated && teams != null && diplomacyMap != null){
            this.ownTeam = getOwnTeam(teams);
            screen.ownTeam = this.ownTeam;
            updateEntryList();
            hasUpdated = true;
        }
    }

    private RecruitsTeam getOwnTeam(List<RecruitsTeam> list){
        String playerTeam = minecraft.player.getTeam().getName();
        for(RecruitsTeam team : list){
            if(team.getTeamName().equals(playerTeam)){
                return team;
            }
        }
        return null;
    }

    public void updateEntryList() {
        entries.clear();

        for (RecruitsTeam team : teams) {
            if(!team.equals(ownTeam)){
                entries.add(new DiplomacyTeamEntry(screen, team));
            }
        }

        updateFilter();
    }

    public RecruitsDiplomacyManager.DiplomacyStatus getRelation(String team, String otherTeam) {
        return diplomacyMap.getOrDefault(team, new HashMap<>()).getOrDefault(otherTeam, RecruitsDiplomacyManager.DiplomacyStatus.NEUTRAL);
    }

    public void updateFilter() {
        clearEntries();
        List<DiplomacyTeamEntry> filteredEntries = new ArrayList<>(entries);
        if (!filter.isEmpty()) {
            filteredEntries.removeIf(teamEntry -> {
                return teamEntry.getTeamInfo() == null || !teamEntry.getTeamInfo().getTeamName().toLowerCase(Locale.ROOT).contains(filter);
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
        return entry.getTeamInfo() == null ? "" : entry.getTeamInfo().getTeamName();
    }

    public void setFilter(String filter) {
        this.filter = filter;
        updateFilter();
    }

    public boolean isEmpty() {
        return children().isEmpty();
    }
}
