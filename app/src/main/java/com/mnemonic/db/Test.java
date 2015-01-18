package com.mnemonic.db;


import java.io.Serializable;


public class Test implements Serializable {

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

    @Override
    public String toString() {
        return String.format("%s[name=%s, description=%s, taskCount=%d, hasFavorite=%b, hasCommented=%b]",
                getClass().getSimpleName(), name, description, taskCount, hasFavorite, hasCommented);
    }
}
