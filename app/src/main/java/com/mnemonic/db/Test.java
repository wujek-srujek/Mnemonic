package com.mnemonic.db;


import java.io.Serializable;


public class Test implements Serializable {

    final long _id;

    private final String name;

    private final String description;

    boolean hasFavorite;

    Test(long _id, String name, String description, boolean hasFavorite) {
        this._id = _id;
        this.name = name;
        this.description = description;
        this.hasFavorite = hasFavorite;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasFavorite() {
        return hasFavorite;
    }

    @Override
    public String toString() {
        return String.format("%s[name=%s, description=%s, hasFavorite=%b]",
                getClass().getSimpleName(), name, description, hasFavorite);
    }
}
