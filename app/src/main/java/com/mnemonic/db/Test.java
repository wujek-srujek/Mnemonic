package com.mnemonic.db;


import java.util.EnumSet;
import java.util.Set;


public class Test {

    final long _id;

    private final String name;

    private final String description;

    int taskCount;

    boolean hasFavorite;

    boolean hasCommented;

    Test(long _id, String name, String description, int taskCount, boolean hasFavorite, boolean hasCommented) {
        this._id = _id;
        this.name = name;
        this.description = description;
        this.taskCount = taskCount;
        this.hasFavorite = hasFavorite;
        this.hasCommented = hasCommented;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public boolean hasFavorite() {
        return hasFavorite;
    }

    public boolean hasCommented() {
        return hasCommented;
    }

    public Set<TaskFilter> availableTaskFilters() {
        Set<TaskFilter> availableTaskFilters = EnumSet.of(TaskFilter.ALL);
        if (hasFavorite) {
            availableTaskFilters.add(TaskFilter.FAVORITE);
        }
        if (hasCommented) {
            availableTaskFilters.add(TaskFilter.COMMENTED);
        }

        return availableTaskFilters;
    }

    @Override
    public String toString() {
        return String.format("%s[name=%s, description=%s, taskCount=%d, hasFavorite=%b, hasCommented=%b]",
                getClass().getSimpleName(), name, description, taskCount, hasFavorite, hasCommented);
    }
}
