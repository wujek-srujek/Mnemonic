package com.mnemonic;


import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;


public interface OnTestClickListener {

    void onTestClick(int position, Test test, TaskFilter taskFilter);
}
