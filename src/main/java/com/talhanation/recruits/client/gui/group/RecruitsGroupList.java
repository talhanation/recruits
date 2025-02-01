package com.talhanation.recruits.client.gui.group;


import com.google.common.collect.Lists;
import com.talhanation.recruits.Main;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.network.MessageServerSavePlayerGroups;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecruitsGroupList extends ListScreenListBase<RecruitsGroupEntry> {

    protected RecruitsGroupListScreen screen;
    protected final List<RecruitsGroupEntry> entries;
    protected String filter;
    public static List<RecruitsGroup> groups;
    public RecruitsGroupList(int width, int height, int x, int y, int size, RecruitsGroupListScreen screen) {
        super(width, height, x, y, size);
        this.screen = screen;
        this.entries = Lists.newArrayList();
        this.filter = "";
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);
        hasUpdated = false;
    }

    public static boolean hasUpdated;
    public void tick() {
        if(!hasUpdated && groups != null){
            updateEntryList();
            hasUpdated = true;
        }
    }

    public void updateEntryList() {
        entries.clear();

        for (RecruitsGroup group : groups) {
            entries.add(new RecruitsGroupEntry(screen, group));
        }

        updateFilter();
    }

    public void updateFilter() {
        clearEntries();
        List<RecruitsGroupEntry> filteredEntries = new ArrayList<>(entries);

        if (!filter.isEmpty()) {
            filteredEntries.removeIf(teamEntry -> {
                return teamEntry.getGroup() == null ||
                        !teamEntry.getGroup().getName().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT));
            });
        }

        filteredEntries.sort((e1, e2) -> {
            if (e1.getGroup() == null || e2.getGroup() == null) {
                return 0;
            }
            return Integer.compare(e1.getGroup().getId(), e2.getGroup().getId());
        });

        replaceEntries(filteredEntries);
    }


    private String volumeEntryToString(RecruitsGroupEntry entry) {
        return entry.getGroup().getName() == null ? "" : entry.getGroup().getName();
    }

    public void setFilter(String filter) {
        this.filter = filter;
        updateFilter();
    }

    public boolean isEmpty() {
        return children().isEmpty();
    }

    public static void saveGroups(boolean update) {
        Main.SIMPLE_CHANNEL.sendToServer(new MessageServerSavePlayerGroups(groups, update));
        hasUpdated = false;
    }
}

