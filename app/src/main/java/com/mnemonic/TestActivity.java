package com.mnemonic;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;
import android.widget.Toolbar;

import java.util.List;


public class TestActivity extends Activity {

    public final static String TEST_EXTRA = "test";

    private final static String CURRENT_TASK_NUMBER_BUNDLE_KEY = "currentTaskNumber";

    private final static String IS_QUESTION_BUNDLE_KEY = "isQuestion";

    private TextView taskTextView;

    private Test test;

    private List<Task> tasks;

    private int currentTaskNumber;

    private boolean isQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        taskTextView = (TextView) findViewById(R.id.task);

        test = (Test) getIntent().getSerializableExtra(TEST_EXTRA);
        tasks = new TaskParser().parse(test.getFile(), test.getFirstLineNumber(), test.getLastLineNumber());

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(test.getName());

        if (savedInstanceState != null) {
            // recreating the state after destroy in the background
            currentTaskNumber = savedInstanceState.getInt(CURRENT_TASK_NUMBER_BUNDLE_KEY, 0);
            isQuestion = savedInstanceState.getBoolean(IS_QUESTION_BUNDLE_KEY, true);
        } else {
            // fresh instance
            currentTaskNumber = 0;
            isQuestion = true;
        }

        if (!tasks.isEmpty()) {
            findViewById(R.id.task).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (isQuestion) {
                        // question always has the next step - the answer
                        isQuestion = false;
                    } else if (currentTaskNumber < tasks.size() - 1) {
                        // answer shown, but there are still more tasks
                        ++currentTaskNumber;
                        isQuestion = true;
                    } else {
                        // answer of the last task shown, cycle to the beginning
                        currentTaskNumber = 0;
                        isQuestion = true;
                    }

                    updateViews();
                }
            });
            updateViews();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(CURRENT_TASK_NUMBER_BUNDLE_KEY, currentTaskNumber);
        outState.putBoolean(IS_QUESTION_BUNDLE_KEY, isQuestion);
    }

    private void updateViews() {
        Task task = tasks.get(currentTaskNumber);
        String taskPartIdentifier;
        String taskPartText;
        if (isQuestion) {
            taskPartIdentifier = getString(R.string.question);
            taskPartText = task.getQuestion();
        } else {
            taskPartIdentifier = getString(R.string.answer);
            taskPartText = task.getAnswer();
        }
        String taskInfo = String.format(getString(R.string.task_info_format), taskPartIdentifier, (currentTaskNumber) + 1, tasks.size());

        getActionBar().setSubtitle(taskInfo);
        taskTextView.setText(taskPartText);
    }
}
