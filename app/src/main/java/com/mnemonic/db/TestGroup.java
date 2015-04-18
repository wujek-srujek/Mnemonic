package com.mnemonic.db;


public class TestGroup {

    private final long _id;

    private final String name;

    private final long creationTimestamp;

    boolean current;

    int testCount;

    int enabledCount;

    TestGroup(long _id, String name, long creationTimestamp, boolean current, int testCount, int enabledCount) {
        this._id = _id;
        this.name = name;
        this.creationTimestamp = creationTimestamp;
        this.current = current;
        this.testCount = testCount;
        this.enabledCount = enabledCount;
    }

    public long getId() {
        return _id;
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

    @Override
    public String toString() {
        return String.format("%s[_id=%d, name=%s, creationTimestamp=%d, current=%b, testCount=%d, enabledCount=%d]",
                getClass().getSimpleName(), _id, name, creationTimestamp, current, testCount, enabledCount);
    }
}
