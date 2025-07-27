package com.talhanation.recruits.client.gui.widgets;

public class ContextMenuEntry {
    public final String label;
    public final Runnable action;
    public final boolean enabled;

    public ContextMenuEntry(String label, Runnable action, boolean enabled) {
        this.label = label;
        this.action = action;
        this.enabled = enabled;
    }
}
