package com.mnemonic.db;


import java.io.Serializable;


public class TestGroup implements Serializable {

    final long _id;

    private final String name;

    private final long creationTimestamp;

    int testCount;

    boolean hasEnabled;

    TestGroup(long _id, String name, long creationTimestamp, int testCount, boolean hasEnabled) {
        this._id = _id;
        this.name = name;
        this.creationTimestamp = creationTimestamp;
        this.testCount = testCount;
        this.hasEnabled = hasEnabled;
    }

    public String getName() {
        return name;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public int isTestCount() {
        return testCount;
    }

    public boolean hasEnabled() {
        return hasEnabled;
    }

    @Override
    public String toString() {
        return String.format("%s[name=%s, creationTimestamp=%d, testCount=%d, hasEnabled=%b]",
                getClass().getSimpleName(), name, creationTimestamp, testCount, hasEnabled);
    }
}
