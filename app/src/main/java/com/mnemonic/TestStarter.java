package com.mnemonic;


import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;


public interface TestStarter {

    void startTest(Test test, TaskFilter taskFilter);
}
