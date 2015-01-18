package com.mnemonic;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toolbar;

import com.mnemonic.db.DbHelper;
import com.mnemonic.db.Task;
import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class TestActivity extends Activity {

    public final static String TEST_EXTRA = "test";

    public final static String TASK_FILTER_EXTRA = "taskFiter";

    private final static String TASKS_BUNDLE_KEY = "tasks";

    private final static String CURRENT_TASK_NUMBER_BUNDLE_KEY = "currentTaskNumber";

    private final static String IS_QUESTION_BUNDLE_KEY = "isQuestion";

    private final static String RANDOM_SEED_BUNDLE_KEY = "randomSeed";

    private DbHelper dbHelper;

    private ViewPager taskPager;

    private ImageButton favoriteButton;

    private List<Task> orderedTasks;

    private int currentTaskNumber;

    private boolean isQuestion;

    private long randomSeed;

    private TaskPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        dbHelper = new DbHelper(this);

        Test test = (Test) getIntent().getSerializableExtra(TEST_EXTRA);
        if (test == null) {
            throw new IllegalStateException("must specify the test using intent extra " + TEST_EXTRA);
        }
        TaskFilter taskFilter = (TaskFilter) getIntent().getSerializableExtra(TASK_FILTER_EXTRA);
        if (taskFilter == null) {
            throw new IllegalStateException("must specify the filter using intent extra " + TASK_FILTER_EXTRA);
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setTitle(test.getName() != null ? test.getName() : getString(R.string.default_test_name));

        if (savedInstanceState != null) {
            // recreating the state after destroy in the background
            currentTaskNumber = savedInstanceState.getInt(CURRENT_TASK_NUMBER_BUNDLE_KEY, 0);
            isQuestion = savedInstanceState.getBoolean(IS_QUESTION_BUNDLE_KEY, true);
            randomSeed = savedInstanceState.getLong(RANDOM_SEED_BUNDLE_KEY, 0);
        } else {
            // fresh instance
            initBasicState();
        }
        // if there was no filter, the tasks need to be fetched as well
        if (savedInstanceState != null && taskFilter != TaskFilter.ALL) {
            @SuppressWarnings("unchecked")
            List<Task> tasks = (List<Task>) savedInstanceState.getSerializable(TASKS_BUNDLE_KEY);
            orderedTasks = tasks;
        } else {
            orderedTasks = dbHelper.getTasks(test, taskFilter);
        }

        taskPager = (ViewPager) findViewById(R.id.task_pager);
        favoriteButton = (ImageButton) findViewById(R.id.test_favorite_button);

        if (orderedTasks.isEmpty()) {
            taskPager.setVisibility(View.GONE);
            favoriteButton.setVisibility(View.GONE);
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

        initPager();
        updateTaskInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // returning false seems better here, but there is an issue which disables
        // the 'up' arrow navigation when false is returned, so just do it instead
        if (!orderedTasks.isEmpty()) {
            getMenuInflater().inflate(R.menu.menu_test, menu);
        }

        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // if the tasks were filtered (like favorite), it may have happened that
        // some of them were changed in a way that they would not appear again in
        // search results; this would result in strange effects in rotations etc.
        // so save them as well; for unfiltered tests, don't bother
        if (getIntent().getSerializableExtra(TASK_FILTER_EXTRA) != TaskFilter.ALL) {
            outState.putSerializable(TASKS_BUNDLE_KEY, new ArrayList<>(orderedTasks));
        }
        outState.putInt(CURRENT_TASK_NUMBER_BUNDLE_KEY, currentTaskNumber);
        outState.putBoolean(IS_QUESTION_BUNDLE_KEY, isQuestion);
        outState.putLong(RANDOM_SEED_BUNDLE_KEY, randomSeed);
    }

    public void restartTest(MenuItem menuItem) {
        restartTest();
    }

    public void toggleFavorite(View view) {
        toggleFavorite();
    }

    private void initBasicState() {
        currentTaskNumber = 0;
        isQuestion = true;
        randomSeed = System.nanoTime();
    }

    private void initPager() {
        List<Task> shuffledTasks = new ArrayList<>(orderedTasks);
        Collections.shuffle(shuffledTasks, new Random(randomSeed));
        adapter = new TaskPagerAdapter(getLayoutInflater(), shuffledTasks);
        taskPager.setAdapter(adapter);
    }

    private void updateTaskInfo() {
        String taskPart = getString(isQuestion ? R.string.question : R.string.answer);
        String taskInfo = String.format(getString(R.string.task_info_format), taskPart, (currentTaskNumber) + 1, orderedTasks.size());
        getActionBar().setSubtitle(taskInfo);

        updateFavoriteState();
    }

    private void restartTest() {
        initBasicState();
        initPager();
        updateTaskInfo();
    }

    private void toggleFavorite() {
        Task currentTask = adapter.taskForPosition(taskPager.getCurrentItem());
        dbHelper.setFavorite(currentTask, !currentTask.isFavorite());

        updateFavoriteState();
    }

    private void updateFavoriteState() {
        Task currentTask = adapter.taskForPosition(taskPager.getCurrentItem());
        favoriteButton.setImageResource(currentTask.isFavorite() ? R.drawable.ic_favorite : R.drawable.ic_not_favorite);
    }
}
