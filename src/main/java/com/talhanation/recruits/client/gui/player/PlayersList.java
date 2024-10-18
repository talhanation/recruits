package com.talhanation.recruits.client.gui.player;

import com.google.common.collect.Lists;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class PlayersList extends ListScreenListBase<RecruitsPlayerEntry> {

    protected SelectPlayerScreen screen;
    protected final List<RecruitsPlayerEntry> entries;
    protected String filter;
    public static List<RecruitsPlayerInfo> onlinePlayers = new ArrayList<>();
    public PlayersList(int width, int height, int x, int y, int size, SelectPlayerScreen screen) {
        super(width, height, x, y, size);
        this.screen = screen;
        this.entries = Lists.newArrayList();
        this.filter = "";
        onlinePlayers.add(new RecruitsPlayerInfo(new UUID(3,3), "Test1"));
        onlinePlayers.add(new RecruitsPlayerInfo(new UUID(3,3), "Test2"));
        onlinePlayers.add(new RecruitsPlayerInfo(new UUID(3,3), "Test3"));
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);
        updateEntryList();
    }

    public void updateEntryList() {
        entries.clear();

        for (RecruitsPlayerInfo player : onlinePlayers) {
            entries.add(new RecruitsPlayerEntry(screen, player));
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
}
