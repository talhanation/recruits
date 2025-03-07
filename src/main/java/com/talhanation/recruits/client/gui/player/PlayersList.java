package com.talhanation.recruits.client.gui.player;

import com.google.common.collect.Lists;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsTeam;
import net.minecraft.world.entity.player.Player;

import java.util.*;


public class PlayersList extends ListScreenListBase<RecruitsPlayerEntry> {

    protected IPlayerSelection screen;
    protected final List<RecruitsPlayerEntry> entries;
    protected String filter;
    protected final PlayersList.FilterType filterType;
    public static List<RecruitsPlayerInfo> onlinePlayers;
    public final Player player;
    public RecruitsTeam recruitsTeam;
    protected final boolean includeSelf;


    public PlayersList(int width, int height, int x, int y, int size, IPlayerSelection screen, PlayersList.FilterType filterType, Player player, boolean includeSelf) {
        super(width, height, x, y, size);
        this.screen = screen;
        this.entries = Lists.newArrayList();
        this.filter = "";
        this.filterType = filterType;
        this.player = player;
        this.includeSelf = includeSelf;
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);
    }

    public void tick() {
        if(onlinePlayers != null){
            updateEntryList();
        }
    }

    public void updateEntryList() {
        entries.clear();
        this.recruitsTeam = this.getRecruitsTeam();


        for (RecruitsPlayerInfo player : onlinePlayers) {

            if(includeSelf || !player.getUUID().equals(this.player.getUUID())){

                switch (filterType){
                    default -> {
                        entries.add(new RecruitsPlayerEntry(screen, player));
                    }
                    case SAME_TEAM -> {
                        RecruitsTeam recruitsTeam = player.getRecruitsTeam();

                        if(recruitsTeam != null && recruitsTeam.getStringID().equals(this.recruitsTeam.getStringID())){
                            entries.add(new RecruitsPlayerEntry(screen, player));
                        }
                    }

                    case TEAM_JOIN_REQUEST -> {
                        if(this.recruitsTeam != null && this.recruitsTeam.getJoinRequests().contains(player.getName())){
                            entries.add(new RecruitsPlayerEntry(screen, player));
                        }
                    }
                }
            }
        }

        updateFilter();
    }

    private RecruitsTeam getRecruitsTeam() {
        RecruitsTeam recruitsTeam = null;
        for (RecruitsPlayerInfo player : onlinePlayers) {
            if(player.getUUID().equals(this.player.getUUID())){
                recruitsTeam = player.getRecruitsTeam();
                break;
            }
        }
        return recruitsTeam;
    }

    public void updateFilter() {
        clearEntries();
        List<RecruitsPlayerEntry> filteredEntries = new ArrayList<>(entries);
        if (!filter.isEmpty()) {
            filteredEntries.removeIf(playerEntry -> {
                return playerEntry.getPlayerInfo() == null || !playerEntry.getPlayerInfo().getName().toLowerCase(Locale.ROOT).contains(filter);
            });
        }

        filteredEntries.sort((e1, e2) -> {
            if (!e1.getClass().equals(e2.getClass())) {
                if (e1 instanceof RecruitsPlayerEntry) {
                    return 1;
                } else {
                    return -1;
                }
            }
            return volumeEntryToString(e1).compareToIgnoreCase(volumeEntryToString(e2));
        });

        replaceEntries(filteredEntries);
    }

    private String volumeEntryToString(RecruitsPlayerEntry entry) {
        return entry.getPlayerInfo() == null ? "" : entry.getPlayerInfo().getName();
    }

    public void setFilter(String filter) {
        this.filter = filter;
        updateFilter();
    }

    public boolean isEmpty() {
        return children().isEmpty();
    }

    public int size(){
        return children().size();
    }

    public enum FilterType{
        NONE,
        SAME_TEAM,
        TEAM_JOIN_REQUEST
    }
}
