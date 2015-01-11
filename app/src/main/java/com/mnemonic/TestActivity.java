package com.mnemonic;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Toolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class TestActivity extends Activity {

    public final static String TEST_EXTRA = "test";

    private final static String CURRENT_TASK_NUMBER_BUNDLE_KEY = "currentTaskNumber";

    private final static String IS_QUESTION_BUNDLE_KEY = "isQuestion";

    private final static String RANDOM_SEED_BUNDLE_KEY = "randomSeed";

    private ViewPager taskPager;

    private List<Task> orderedTasks;

    private int currentTaskNumber;

    private boolean isQuestion;

    private long randomSeed;

    private List<Task> shuffledTasks;

    private TaskPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        Test test = (Test) getIntent().getSerializableExtra(TEST_EXTRA);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(test.getName());

        orderedTasks = new TaskParser().parse(test.getFile(), test.getFirstLineNumber(), test.getLastLineNumber());

        taskPager = (ViewPager) findViewById(R.id.task_pager);
        View restartButton = findViewById(R.id.restart_button);

        if (orderedTasks.isEmpty()) {
            taskPager.setVisibility(View.GONE);
            restartButton.setVisibility(View.GONE);
            findViewById(R.id.empty_test_info_label).setVisibility(View.VISIBLE);
            return;
        }

        taskPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // nope
            }

            @Override
            public void onPageSelected(int position) {
                isQuestion = adapter.isQuestion(position);
                currentTaskNumber = adapter.taskNumberForPosition(position);

                updateTaskInfo();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // nope
            }
        });

        restartButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                initValues();
                initPager();
                updateTaskInfo();
            }
        });

        if (savedInstanceState != null) {
            // recreating the state after destroy in the background
            currentTaskNumber = savedInstanceState.getInt(CURRENT_TASK_NUMBER_BUNDLE_KEY, 0);
            isQuestion = savedInstanceState.getBoolean(IS_QUESTION_BUNDLE_KEY, true);
            randomSeed = savedInstanceState.getLong(RANDOM_SEED_BUNDLE_KEY, 0);
        } else {
            // fresh instance
            initValues();
        }

        initPager();
        updateTaskInfo();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(CURRENT_TASK_NUMBER_BUNDLE_KEY, currentTaskNumber);
        outState.putBoolean(IS_QUESTION_BUNDLE_KEY, isQuestion);
        outState.putLong(RANDOM_SEED_BUNDLE_KEY, randomSeed);
    }

    private void updateTaskInfo() {
        String taskPart = getString(isQuestion ? R.string.question : R.string.answer);
        String taskInfo = String.format(getString(R.string.task_info_format), taskPart, (currentTaskNumber) + 1, shuffledTasks.size());
        getActionBar().setSubtitle(taskInfo);
    }

    private void initValues() {
        currentTaskNumber = 0;
        isQuestion = true;
        randomSeed = System.nanoTime();
    }

    private void initPager() {
        shuffledTasks = new ArrayList<>(orderedTasks);
        Collections.shuffle(shuffledTasks, new Random(randomSeed));
        adapter = new TaskPagerAdapter(getLayoutInflater(), shuffledTasks);
        taskPager.setAdapter(adapter);
    }
}
