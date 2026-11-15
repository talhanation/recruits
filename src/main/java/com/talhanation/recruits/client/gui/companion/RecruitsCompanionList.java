package com.talhanation.recruits.client.gui.companion;

import com.google.common.collect.Lists;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The companions of the player, like the faction and group lists. The entries come from the server once
 * ({@link #setEntries}) and keep the order they are given in.
 */
public class RecruitsCompanionList extends ListScreenListBase<RecruitsCompanionEntry> {
    protected final List<RecruitsCompanionEntry> entries;
    protected String filter;

    public RecruitsCompanionList(int width, int height, int x, int y, int size) {
        super(width, height, x, y, size);
        this.entries = Lists.newArrayList();
        this.filter = "";
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);
    }

    public void setEntries(List<RecruitsCompanionEntry> list) {
        entries.clear();
        entries.addAll(list);
        updateFilter();
    }

    public void updateFilter() {
        clearEntries();
        List<RecruitsCompanionEntry> filteredEntries = new ArrayList<>(entries);
        if (!filter.isEmpty()) {
            filteredEntries.removeIf(entry -> !entry.getTitle().toLowerCase(Locale.ROOT).contains(filter));
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
