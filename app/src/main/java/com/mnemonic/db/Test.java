package com.mnemonic.db;


import java.util.EnumSet;
import java.util.Set;


public class Test {

    final long _id;

    private final String name;

    private final String description;

    boolean enabled;

    int taskCount;

    int pagesCount;

    Set<TaskFilter> availableTaskFilters;

    Test(long _id, String name, String description, boolean enabled, int taskCount, int pagesCount, Set<TaskFilter> availableTaskFilters) {
        this._id = _id;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.taskCount = taskCount;
        this.pagesCount = pagesCount;
        this.availableTaskFilters = availableTaskFilters;
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

    public int getPagesCount() {
        return pagesCount;
    }

    public Set<TaskFilter> getAvailableTaskFilters() {
        return EnumSet.copyOf(availableTaskFilters);
    }

    public boolean hasTaskFilter(TaskFilter taskFilter) {
        return availableTaskFilters.contains(taskFilter);
    }

    @Override
    public String toString() {
        return String.format("%s[name=%s, description=%s, enabled=%b, taskCount=%d, pagesCount=%d, availableTaskFilters=%s]",
                getClass().getSimpleName(), name, description, enabled, taskCount, pagesCount, availableTaskFilters);
    }
}
