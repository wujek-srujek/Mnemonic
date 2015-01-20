package com.mnemonic.db;


import java.io.Serializable;


public class Test implements Serializable {

    final long _id;

    private final String name;

    private final String description;

    int taskCount;

    boolean hasFavorite;

    Test(long _id, String name, String description, int taskCount, boolean hasFavorite) {
        this._id = _id;
        this.name = name;
        this.description = description;
        this.taskCount = taskCount;
        this.hasFavorite = hasFavorite;
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

    @Override
    public String toString() {
        return String.format("%s[name=%s, description=%s, taskCount=%d, hasFavorite=%b]",
                getClass().getSimpleName(), name, description, taskCount, hasFavorite);
    }
}
