package com.mnemonic.db;


import java.io.Serializable;


public class Test implements Serializable {

    final long _id;

    private final String name;

    private final String description;

    Test(long _id, String name, String description) {
        this._id = _id;
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("%s[name=%s, description=%s]", getClass().getSimpleName(), name, description);
    }
}
