package com.talhanation.recruits.api.governor;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The daily report of a Governor to his owner: one message at the meeting, or at the next login.
 * Modules add a few short lines; the report is cut after {@link #MAX_LINES} so it stays readable.
 */
public class GovernorReport {

    public static final int MAX_LINES = 6;

    private final List<Component> lines = new ArrayList<>();

    /** Adds a line, false if the report is full. Keep lines short: what happened, one bottleneck, one hint. */
    public boolean add(Component line) {
        if (line == null || lines.size() >= MAX_LINES) return false;
        lines.add(line);
        return true;
    }

    public boolean isFull() {
        return lines.size() >= MAX_LINES;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public List<Component> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
