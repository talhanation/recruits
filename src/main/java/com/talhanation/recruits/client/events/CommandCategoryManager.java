package com.talhanation.recruits.client.events;

import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class CommandCategoryManager {
    private static final LinkedHashMap<ICommandCategory, Integer> categories = new LinkedHashMap<>();

    private CommandCategoryManager() {}

    public static void register(ICommandCategory category) {
        register(category, 0);
    }

    public static void register(ICommandCategory category, int priority) {
        categories.put(category, priority);
    }


    public static List<ICommandCategory> getCategories() {
        List<Map.Entry<ICommandCategory, Integer>> entries = new ArrayList<>(categories.entrySet());

        entries.sort(Comparator.comparingInt(Map.Entry::getValue));
        List<ICommandCategory> result = new ArrayList<>(entries.size());
        for (Map.Entry<ICommandCategory, Integer> e : entries) result.add(e.getKey());
        return Collections.unmodifiableList(result);
    }

    public static ICommandCategory getByIndex(int index) {
        List<ICommandCategory> cats = getCategories();
        if (index >= 0 && index < cats.size()) return cats.get(index);
        return cats.isEmpty() ? null : cats.get(0);
    }

    public static ICommandCategory getNext(ICommandCategory current) {
        List<ICommandCategory> cats = getCategories();
        int i = cats.indexOf(current);
        if (i != -1 && i < cats.size()-1) return cats.get(i+1);
        return current;
    }

    public static ICommandCategory getPrevious(ICommandCategory current) {
        List<ICommandCategory> cats = getCategories();
        int i = cats.indexOf(current);
        if (i > 0) return cats.get(i-1);
        return current;
    }
}