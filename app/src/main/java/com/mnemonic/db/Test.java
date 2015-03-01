package com.mnemonic.db;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.EnumSet;
import java.util.Set;


public class Test implements Parcelable {

    public static final Parcelable.Creator<Test> CREATOR = new Parcelable.Creator<Test>() {

        @Override
        public Test createFromParcel(Parcel in) {
            return Test.readFromParcel(in);
        }

        @Override
        public Test[] newArray(int size) {
            return new Test[size];
        }
    };

    final long _id;

    private final String name;

    private final String description;

    boolean enabled;

    int taskCount;

    int pagesCount;

    Set<TaskFilter> availableTaskFilters;

    Test(long _id, String name, String description, boolean enabled, int taskCount,
         int pagesCount, Set<TaskFilter> availableTaskFilters) {
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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(_id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeInt(enabled ? 1 : 0);
        dest.writeInt(taskCount);
        dest.writeInt(pagesCount);
        dest.writeInt(availableTaskFilters.size());
        for (TaskFilter taskFilter : availableTaskFilters) {
            dest.writeString(taskFilter.name());
        }
    }

    static Test readFromParcel(Parcel in) {
        long _id = in.readLong();
        String name = in.readString();
        String description = in.readString();
        boolean enabled = in.readInt() != 0;
        int taskCount = in.readInt();
        int pagesCount = in.readInt();
        Set<TaskFilter> availableTaskFilters = EnumSet.noneOf(TaskFilter.class);
        int filterCount = in.readInt();
        for (int i = 0; i < filterCount; ++i) {
            availableTaskFilters.add(TaskFilter.valueOf(in.readString()));
        }

        return new Test(_id, name, description, enabled, taskCount, pagesCount, availableTaskFilters);
    }

    @Override
    public String toString() {
        return String.format("%s[name=%s, description=%s, enabled=%b, taskCount=%d, pagesCount=%d, availableTaskFilters=%s]",
                getClass().getSimpleName(), name, description, enabled, taskCount, pagesCount, availableTaskFilters);
    }
}
