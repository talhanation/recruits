package com.talhanation.recruits.client.events;

import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandCategoryManager {
    private static final List<ICommandCategory> categories = new ArrayList<>();
    public static void register(ICommandCategory category) {
        categories.add(category);
    }

    public static List<ICommandCategory> getCategories() {
        return categories;
    }
    public static ICommandCategory getByIndex(int index) {
        List<ICommandCategory> categories = getCategories();
        if (index >= 0 && index < categories.size()) {
            return categories.get(index);
        }
        return categories.isEmpty() ? null : categories.get(0);
    }

    public static ICommandCategory getNext(ICommandCategory current) {
        int index = categories.indexOf(current);
        if (index != -1 && index < categories.size() - 1) {
            return categories.get(index + 1);
        }
        return current;
    }

    public static ICommandCategory getPrevious(ICommandCategory current) {
        int index = categories.indexOf(current);
        if (index > 0) {
            return categories.get(index - 1);
        }
        return current;
    }
}
