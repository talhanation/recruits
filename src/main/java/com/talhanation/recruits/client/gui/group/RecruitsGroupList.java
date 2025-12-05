package com.talhanation.recruits.client.gui.group;


import com.google.common.collect.Lists;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsGroup;
import org.jetbrains.annotations.Nullable;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RecruitsGroupList extends ListScreenListBase<RecruitsGroupEntry> {

    protected IGroupSelection screen;
    protected final List<RecruitsGroupEntry> entries;
    protected String filter;
    protected List<UUID> blackList = new ArrayList<>();
    public RecruitsGroupList(int width, int height, int x, int y, int size, IGroupSelection screen, List<UUID> blackList) {
        super(width, height, x, y, size);
        this.screen = screen;
        this.entries = Lists.newArrayList();
        this.filter = "";
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        setRenderSelection(true);

        if(blackList != null){
            this.blackList.addAll(blackList);
        }
    }

    public void tick() {
        if(ClientManager.groups != null){
            updateEntryList();
        }
    }

    public void updateEntryList() {
        entries.clear();

        for (RecruitsGroup group : ClientManager.groups) {
            if(!blackList.contains(group.getUUID()))
                entries.add(new RecruitsGroupEntry(screen, group));
        }

        updateFilter();
    }

    public void updateFilter() {
        clearEntries();
        List<RecruitsGroupEntry> filteredEntries = new ArrayList<>(entries);
        if (!filter.isEmpty()) {
            filteredEntries.removeIf(
                groupEntry -> groupEntry.getGroup() == null ||
                              !groupEntry.getGroup().getName().toLowerCase(Locale.ROOT).contains(filter)
            );
        }

        replaceEntries(filteredEntries);
    }

    public void setFilter(String filter) {
        this.filter = filter;
        updateFilter();
    }
    @Nullable
    public RecruitsGroupEntry getGroupEntryAtPosition(double x, double y){
        return this.getEntryAtPosition(x,y);
    }



    public boolean isEmpty() {
        return children().isEmpty();
    }
}

