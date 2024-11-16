package com.talhanation.recruits.client.gui.player;

import com.google.common.collect.Lists;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.world.NoteBlockEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class PlayersList extends ListScreenListBase<RecruitsPlayerEntry> {

    protected SelectPlayerScreen screen;
    protected final List<RecruitsPlayerEntry> entries;
    protected String filter;
    protected final boolean sameTeamOnly;
    public static List<RecruitsPlayerInfo> onlinePlayers;
    public final Player player;
    protected final boolean includeSelf;

    public  PlayersList(int width, int height, int x, int y, int size, SelectPlayerScreen screen, boolean sameTeamOnly, Player player, boolean includeSelf) {
        super(width, height, x, y, size);
        this.screen = screen;
        this.entries = Lists.newArrayList();
        this.filter = "";
        this.sameTeamOnly = sameTeamOnly;
        this.player = player;
        this.includeSelf = includeSelf;
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);
    }


    boolean hasUpdated;
    public void tick() {
        if(!hasUpdated && onlinePlayers != null){
            updateEntryList();
            hasUpdated = true;
        }
    }

    public void updateEntryList() {
        entries.clear();

        for (RecruitsPlayerInfo player : onlinePlayers) {
            if(!player.getUUID().equals(this.player.getUUID()) || includeSelf){

                if(sameTeamOnly){
                    if(player.getTeamName().equals(this.player.getTeam().getName())){
                        entries.add(new RecruitsPlayerEntry(screen, player));
                    }
                }
                else{
                    entries.add(new RecruitsPlayerEntry(screen, player));
                }
            }
        }

        updateFilter();
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

    enum FilterSetting{
        TEAM,
        ALL

    }
}
