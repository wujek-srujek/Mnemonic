package com.mnemonic.db;


import java.io.Serializable;


public class TestGroup implements Serializable {

    final long _id;

    private final String name;

    TestGroup(long _id, String name) {
        this._id = _id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("%s[name=%s]", getClass().getSimpleName(), name);
    }
}
