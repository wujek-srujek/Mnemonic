package com.mnemonic.db;


import java.util.EnumSet;
import java.util.Set;


public class TestGroup {

    final long _id;

    private final String name;

    private final long creationTimestamp;

    boolean current;

    int testCount;

    int enabledCount;

    Set<TaskFilter> availableTaskFilters;

    TestGroup(long _id, String name, long creationTimestamp, boolean current,
              int testCount, int enabledCount, Set<TaskFilter> availableTaskFilters) {
        this._id = _id;
        this.name = name;
        this.creationTimestamp = creationTimestamp;
        this.current = current;
        this.testCount = testCount;
        this.enabledCount = enabledCount;
        this.availableTaskFilters = availableTaskFilters;
    }

    public String getName() {
        return name;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public boolean isCurrent() {
        return current;
    }

    public int getTestCount() {
        return testCount;
    }

    public int enabledCount() {
        return enabledCount;
    }

    public int disabledCount() {
        return testCount - enabledCount;
    }

    public Set<TaskFilter> getAvailableTaskFilters() {
        return EnumSet.copyOf(availableTaskFilters);
    }

    public boolean hasTaskFilter(TaskFilter taskFilter) {
        return availableTaskFilters.contains(taskFilter);
    }

    @Override
    public String toString() {
        return String.format("%s[name=%s, creationTimestamp=%d, current=%b, testCount=%d, enabledCount=%d, availableTaskFilters=%s]",
                getClass().getSimpleName(), name, creationTimestamp, current, testCount, enabledCount, availableTaskFilters);
    }
}
