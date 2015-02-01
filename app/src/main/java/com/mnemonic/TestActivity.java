package com.mnemonic;


import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toolbar;

import com.mnemonic.db.DbHelper;
import com.mnemonic.db.Task;
import com.mnemonic.db.TaskPage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class TestActivity extends Activity {

    public final static String TEST_NAME_EXTRA = "testName";

    public final static String TASKS_EXTRA = "tasks";

    public final static String PAGES_COUNT_EXTRA = "pagesCount";

    public final static String RANDOMIZE_EXTRA = "randomize";

    public final static String START_TASK_INDEX_EXTRA = "startTaskIndex";

    private final static String RANDOM_SEED_BUNDLE_KEY = "randomSeed";

    private DbHelper dbHelper;

    private ViewPager taskPager;

    private ImageButton favoriteButton;

    private ArrayList<Task> orderedTasks;

    private int pagesCount;

    private boolean randomize;

    private long randomSeed;

    private TaskPagerAdapter taskPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_test);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        Intent intent = getIntent();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(intent.getStringExtra(TEST_NAME_EXTRA));
        }

        dbHelper = new DbHelper(this);

        orderedTasks = intent.getParcelableArrayListExtra(TASKS_EXTRA);
        pagesCount = intent.getIntExtra(PAGES_COUNT_EXTRA, 0);
        randomize = intent.getBooleanExtra(RANDOMIZE_EXTRA, false);
        int startTaskIndex = intent.getIntExtra(START_TASK_INDEX_EXTRA, 0);

        if (randomize) {
            if (savedInstanceState != null) {
                // recreating the state after destroy in the background
                randomSeed = savedInstanceState.getLong(RANDOM_SEED_BUNDLE_KEY, 0);
            } else {
                // fresh instance
                randomSeed = System.nanoTime();
            }
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
                updateTaskInfo();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // nope
            }
        });

        initPager(startTaskIndex);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (taskPagerAdapter != null) {
            updateTaskInfo();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (orderedTasks.isEmpty()) {
            return false;
        }

        getMenuInflater().inflate(R.menu.menu_test, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.test_action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, TaskSearchActivity.class)));

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (taskPagerAdapter != null) {
            Task currentTask = taskPagerAdapter.getTask(taskPager.getCurrentItem());
            boolean hasComment = currentTask.getComment() != null;

            MenuItem commentMenuItem = menu.findItem(R.id.test_action_comment);
            commentMenuItem.setIcon(hasComment ? R.drawable.ic_action_comment : R.drawable.ic_action_no_comment);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            String comment = data.getStringExtra(CommentActivity.COMMENT_EXTRA);
            if (comment != null) {
                comment = comment.trim();
                if (comment.isEmpty()) {
                    comment = null;
                }
            }

            Task currentTask = taskPagerAdapter.getTask(taskPager.getCurrentItem());
            dbHelper.setComment(currentTask, comment);

            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (randomize) {
            outState.putLong(RANDOM_SEED_BUNDLE_KEY, randomSeed);
        }
    }

    public void restartTest(MenuItem menuItem) {
        restartTest();
    }

    public void toggleFavorite(View view) {
        toggleFavorite();
    }

    public void editComment(MenuItem menuItem) {
        editComment();
    }

    private void initPager(int startTaskIndex) {
        List<Task> usedTasks;
        if (randomize) {
            usedTasks = new ArrayList<>(orderedTasks);
            Collections.shuffle(usedTasks, new Random(randomSeed));
        } else {
            usedTasks = orderedTasks;
        }
        List<TaskPage> taskPages = new ArrayList<>(pagesCount);
        int pageIndex = 0;
        int i = 0;
        for (Task task : usedTasks) {
            if (i == startTaskIndex) {
                pageIndex = taskPages.size();
            }
            taskPages.addAll(task.getPages(++i));
        }

        taskPagerAdapter = new TaskPagerAdapter(getLayoutInflater(), taskPages);
        taskPager.setAdapter(taskPagerAdapter);

        taskPager.setCurrentItem(pageIndex, false);
    }

    private void updateTaskInfo() {
        TaskPage currentPage = taskPagerAdapter.getTaskPage(taskPager.getCurrentItem());

        int stringId;
        switch (currentPage.getType()) {
            case QUESTION:
                stringId = R.string.question;
                break;

            case ANSWER:
                stringId = R.string.answer;
                break;

            default:
                stringId = R.string.info;
                break;
        }

        String taskInfo = String.format(getString(R.string.task_info_format),
                currentPage.getTaskNumber(), orderedTasks.size(), getString(stringId));

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(taskInfo);
        }

        Task currentTask = currentPage.getTask();
        updateFavoriteState(currentTask);
        invalidateOptionsMenu();
    }

    private void restartTest() {
        randomSeed = System.nanoTime();
        initPager(0);
        updateTaskInfo();
    }

    private void toggleFavorite() {
        Task currentTask = taskPagerAdapter.getTask(taskPager.getCurrentItem());
        dbHelper.setFavorite(currentTask, !currentTask.isFavorite());

        updateFavoriteState(currentTask);
    }

    private void editComment() {
        Task currentTask = taskPagerAdapter.getTask(taskPager.getCurrentItem());

        Intent intent = new Intent(this, CommentActivity.class);
        intent.putExtra(CommentActivity.COMMENT_EXTRA, currentTask.getComment());

        startActivityForResult(intent, 0);
    }

    private void updateFavoriteState(Task task) {
        favoriteButton.setImageResource(task.isFavorite() ?
                R.drawable.ic_action_favorite : R.drawable.ic_action_not_favorite);
    }
}
