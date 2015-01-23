package com.mnemonic;


import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;


public interface OnTestLongClickListener {

    void onTestLongClick(int position, Test test, TaskFilter taskFilter);
}
