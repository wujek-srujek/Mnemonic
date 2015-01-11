package com.mnemonic;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.widget.Toolbar;

import java.util.List;


public class TestActivity extends Activity {

    public final static String TEST_EXTRA = "test";

    private final static String CURRENT_TASK_NUMBER_BUNDLE_KEY = "currentTaskNumber";

    private final static String IS_QUESTION_BUNDLE_KEY = "isQuestion";

    private List<Task> tasks;

    private int currentTaskNumber;

    private boolean isQuestion;

    private TaskPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        Test test = (Test) getIntent().getSerializableExtra(TEST_EXTRA);
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
            ViewPager pager = (ViewPager) findViewById(R.id.pager);
            adapter = new TaskPagerAdapter(getLayoutInflater(), tasks);
            pager.setAdapter(adapter);
            pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    // nope
                }

                @Override
                public void onPageSelected(int position) {
                    isQuestion = adapter.isQuestion(position);
                    currentTaskNumber = adapter.taskNumberForPosition(position);

                    updateViews();
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    // nope
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
        String taskPart = getString(isQuestion ? R.string.question : R.string.answer);
        String taskInfo = String.format(getString(R.string.task_info_format), taskPart, (currentTaskNumber) + 1, tasks.size());
        getActionBar().setSubtitle(taskInfo);
    }
}
