package com.mnemonic;


import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;


public class TestChoiceInfo {

    public final int position;

    public final Test test;

    public final TaskFilter taskFilter;

    public TestChoiceInfo(int position, Test test, TaskFilter taskFilter) {
        this.position = position;
        this.test = test;
        this.taskFilter = taskFilter;
    }
}
