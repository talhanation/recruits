package com.talhanation.recruits.command;

/**
 * Standard priority levels for queued recruit commands. Higher number means more urgent.
 */
public final class CommandIntentPriority {
    public static final int LOW = 1;
    public static final int NORMAL = 3;
    public static final int HIGH = 5;
    public static final int IMMEDIATE = 10;

    private CommandIntentPriority() {
    }
}
