package com.talhanation.recruits.client.gui.player;

import com.google.common.collect.Lists;
import com.talhanation.recruits.client.ClientManager;
import com.talhanation.recruits.client.gui.widgets.ListScreenListBase;
import com.talhanation.recruits.world.RecruitsPlayerInfo;
import com.talhanation.recruits.world.RecruitsFaction;
import net.minecraft.world.entity.player.Player;

import java.util.*;


public class PlayersList extends ListScreenListBase<RecruitsPlayerEntry> {

    protected IPlayerSelection screen;
    protected final List<RecruitsPlayerEntry> entries;
    protected String filter;
    protected final PlayersList.FilterType filterType;
    public final Player player;
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
        if(ClientManager.onlinePlayers != null){
            updateEntryList();
        }
    }

    public void updateEntryList() {
        entries.clear();

        switch (filterType) {
            case SAME_TEAM -> {
                if (ClientManager.ownFaction != null) {
                    Set<UUID> onlineUUIDs = new HashSet<>();
                    for (RecruitsPlayerInfo online : ClientManager.onlinePlayers) {
                        onlineUUIDs.add(online.getUUID());
                    }

                    for (RecruitsPlayerInfo member : ClientManager.ownFaction.getMembers()) {
                        if (includeSelf || !member.getUUID().equals(this.player.getUUID())) {
                            member.setOnline(onlineUUIDs.contains(member.getUUID()));
                            entries.add(new RecruitsPlayerEntry(screen, member));
                        }
                    }
                }
            }
            default -> {
                for (RecruitsPlayerInfo playerInfo : ClientManager.onlinePlayers) {
                    if (includeSelf || !playerInfo.getUUID().equals(this.player.getUUID())) {
                        switch (filterType) {
                            default -> entries.add(new RecruitsPlayerEntry(screen, playerInfo));

                            case TEAM_JOIN_REQUEST -> {
                                if (ClientManager.ownFaction != null && ClientManager.ownFaction.getJoinRequests().contains(playerInfo.getName())) {
                                    entries.add(new RecruitsPlayerEntry(screen, playerInfo));
                                }
                            }

                            case ANY_TEAM -> {
                                if (playerInfo.getFaction() != null) {
                                    entries.add(new RecruitsPlayerEntry(screen, playerInfo));
                                }
                            }
                        }
                    }
                }
            }
        }

        updateFilter();
    }

    public void updateFilter() {
        clearEntries();
        List<RecruitsPlayerEntry> filteredEntries = new ArrayList<>(entries);
        if (!filter.isEmpty()) {
            filteredEntries.removeIf(playerEntry ->
                    playerEntry.getPlayerInfo() == null || !playerEntry.getPlayerInfo().getName().toLowerCase(Locale.ROOT).contains(filter));
        }

        filteredEntries.sort((e1, e2) -> {
            if (!e1.getClass().equals(e2.getClass())) {
                if (e1 instanceof RecruitsPlayerEntry) {
                    return 1;
                } else {
                    return -1;
                }
            }
            boolean o1online = e1.getPlayerInfo() != null && e1.getPlayerInfo().isOnline();
            boolean o2online = e2.getPlayerInfo() != null && e2.getPlayerInfo().isOnline();
            if (o1online != o2online) return o1online ? -1 : 1;
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
        TEAM_JOIN_REQUEST,
        ANY_TEAM
    }
}
