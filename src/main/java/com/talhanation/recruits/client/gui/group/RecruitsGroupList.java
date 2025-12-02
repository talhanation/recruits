package com.talhanation.recruits.client.gui.group;


import com.google.common.collect.Lists;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsGroup;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecruitsGroupList extends ListScreenListBase<RecruitsGroupEntry> {

    protected IGroupSelection screen;
    protected final List<RecruitsGroupEntry> entries;
    protected String filter;
    public RecruitsGroupList(int width, int height, int x, int y, int size, IGroupSelection screen) {
        super(width, height, x, y, size);
        this.screen = screen;
        this.entries = Lists.newArrayList();
        this.filter = "";
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);
    }

    public void tick() {
        if(ClientManager.groups != null){
            updateEntryList();
        }
    }

    public void updateEntryList() {
        entries.clear();

        for (RecruitsGroup group : ClientManager.groups) {
            entries.add(new RecruitsGroupEntry(screen, group));
        }

        updateFilter();
    }

    public void updateFilter() {
        clearEntries();
        List<RecruitsGroupEntry> filteredEntries = new ArrayList<>(entries);
        if (!filter.isEmpty()) {
            filteredEntries.removeIf(groupEntry -> {
                return groupEntry.getGroup() == null || !groupEntry.getGroup().getName().toLowerCase(Locale.ROOT).contains(filter);
            });
        }

        replaceEntries(filteredEntries);
    }

    public void setFilter(String filter) {
        this.filter = filter;
        updateFilter();
    }

    public boolean isEmpty() {
        return children().isEmpty();
    }
}

