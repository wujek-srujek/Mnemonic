package com.mnemonic;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.mnemonic.db.DbHelper;
import com.mnemonic.db.Task;
import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;
import com.mnemonic.db.TestGroup;
import com.mnemonic.importer.ImportException;
import com.mnemonic.importer.Importer;
import com.mnemonic.view.recycler.Extras;
import com.mnemonic.view.recycler.HorizontallySwipeableRecyclerView;
import com.mnemonic.view.recycler.ListAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MnemonicActivity extends Activity {

    private static final String TAG = MnemonicActivity.class.getSimpleName();

    private static final String MULTITEST_MODE_ON_BUNDLE_KEY = "multitestModeOn";

    private static final String SELECTION_POSITIONS_BUNDLE_KEY = "selectedIndices";

    private static final String SELECTIONS_BUNDLE_KEY = "selections";

    private static final long UI_UPDATE_DELAY = 500;

    private Handler handler;

    private long afterSwipeAnimationDuration;

    private DbHelper dbHelper;

    private ActionBarDrawerToggle drawerToggle;

    private RecyclerView testGroupList;

    private LinearLayoutManager testGroupListLayout;

    private View emptyTestGroupListInfoLabel;

    private TestGroupListAdapter testGroupListAdapter;

    private HorizontallySwipeableRecyclerView testList;

    private TextView emptyTestListInfoLabel;

    private TestListAdapter testListAdapter;

    private View startMultitestButton;

    private ActionMode multitestMode;

    private TestGroup currentTestGroup;

    private TestGroup newlyAddedTestGroup;

    private ListAdapter.OnItemClickListener<TestGroup, Boolean> onTestGroupClickListener;

    private ListAdapter.OnItemClickListener<Test, TaskFilter> onTestClickListener;

    private ListAdapter.OnItemLongClickListener<Test, TaskFilter> onTestLongClickListener;

    private ActionMode.Callback multitestModeCallback;

    private int snackbarBackgroundColor;

    private Snackbar currentSnackbar;

    private Test lastDisabledTest;

    private int lastDisabledTestPosition = -1;

    private View.OnClickListener reenableTestClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mnemonic);
        setActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        handler = new Handler(Looper.getMainLooper());
        afterSwipeAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        dbHelper = MnemonicApplication.getDbHelper();

        final DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.action_open_drawer, R.string.action_close_drawer);
        drawerLayout.setDrawerListener(drawerToggle);

        testGroupList = (RecyclerView) findViewById(R.id.test_group_list);
        testGroupListLayout = new LinearLayoutManager(this);
        testGroupList.setLayoutManager(testGroupListLayout);

        emptyTestGroupListInfoLabel = findViewById(R.id.empty_test_group_list_info_label);

        testList = (HorizontallySwipeableRecyclerView) findViewById(R.id.test_list);
        RecyclerView.LayoutManager layoutManager =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ?
                        new LinearLayoutManager(this) : new GridLayoutManager(this, 2);
        testList.setLayoutManager(layoutManager);
        testList.setSwipeListener(new HorizontallySwipeableRecyclerView.SwipeListener() {

            @Override
            public boolean isSwipeable(View view, int position) {
                return true;
            }

            @Override
            public void onSwipeFeedback(View view, int position, float deltaX) {
                view.setTranslationX(deltaX);
                view.setAlpha(1 - Math.abs(deltaX / view.getWidth()));
            }

            @Override
            public void onSwipeCancel(View view, int position) {
                view.animate().translationX(0.F).alpha(1.F).setDuration(afterSwipeAnimationDuration).start();
            }

            @Override
            public void onSwipe(final View view, final int position, boolean right) {
                // disable swiping while this dismissal is being processed
                testList.setSwipingEnabled(false);

                float translationX = right ? view.getWidth() : -view.getWidth();
                view.animate().translationX(translationX).alpha(0.F).
                        setDuration(afterSwipeAnimationDuration).setListener(new AnimatorListenerAdapter() {

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        lastDisabledTest = testListAdapter.getItem(position);
                        lastDisabledTestPosition = position;

                        dbHelper.setTestEnabled(lastDisabledTest, false);
                        dbHelper.refreshTestGroup(currentTestGroup);

                        // deleting the item will trigger recycler view animations
                        // when they are done, the animator will be called to finish
                        testListAdapter.deleteItem(position);
                        if (multitestMode != null) {
                            // maybe a selected test was disabled
                            updateMultitestMode();
                        }
                    }
                }).start();
            }
        });

        testList.setItemAnimator(new DefaultItemAnimator() {

            private int animationsCount = 0;

            @Override
            public void onRemoveStarting(RecyclerView.ViewHolder item) {
                ++animationsCount;
            }

            @Override
            public void onAddStarting(RecyclerView.ViewHolder item) {
                ++animationsCount;
            }

            @Override
            public void onMoveStarting(RecyclerView.ViewHolder item) {
                ++animationsCount;
            }

            @Override
            public void onChangeStarting(RecyclerView.ViewHolder item, boolean oldItem) {
                ++animationsCount;
            }

            @Override
            public void onRemoveFinished(RecyclerView.ViewHolder item) {
                // this method might be called when test are removed while all tests are being enabled
                // I guess the algorithm there invokes removals
                // we want to run some code only when we really removed the test by swiping it away
                boolean swipeRemoved = item.itemView.getTranslationX() != 0;

                if (swipeRemoved) {
                    // at this point, the remove animation is done
                    // reset the view after it is removed to be eligible for reuse
                    item.itemView.setAlpha(1.F);
                    item.itemView.setTranslationX(0.F);

                    // when all animations are done, refresh the test list views if necessary
                    if (testListAdapter.getItemCount() == 0) {
                        isRunning(new ItemAnimatorFinishedListener() {

                            @Override
                            public void onAnimationsFinished() {
                                showEmptyTestListMessage(R.string.all_tests_disabled_in_test_group);
                            }
                        });
                    }

                    // after all animations (most likely moves) are done, show a snackbar
                    // to allow undoing the operation
                    isRunning(new ItemAnimatorFinishedListener() {

                        @Override
                        public void onAnimationsFinished() {
                            showSnackbar(getString(R.string.test_disabled), getString(R.string.undo), reenableTestClickListener);
                        }
                    });
                }

                enableSwipingAfterAnimations();
            }

            @Override
            public void onAddFinished(RecyclerView.ViewHolder item) {
                enableSwipingAfterAnimations();
            }

            @Override
            public void onMoveFinished(RecyclerView.ViewHolder item) {
                enableSwipingAfterAnimations();
            }

            @Override
            public void onChangeFinished(RecyclerView.ViewHolder item, boolean oldItem) {
                enableSwipingAfterAnimations();
            }

            private void enableSwipingAfterAnimations() {
                if (animationsCount == 1) {
                    // current animations count is 1 which means the last animation has just finished
                    // this makes sure that the callback will be called only once; for example, when a
                    // removal triggers multiple moves, this callback will be called after the last one
                    isRunning(new ItemAnimatorFinishedListener() {

                        @Override
                        public void onAnimationsFinished() {
                            testList.setSwipingEnabled(true);
                        }
                    });
                }

                --animationsCount;
            }
        });

        emptyTestListInfoLabel = (TextView) findViewById(R.id.empty_test_list_info_label);
        startMultitestButton = findViewById(R.id.start_multitest_button);

        onTestGroupClickListener = new ListAdapter.OnItemClickListener<TestGroup, Boolean>() {

            @Override
            public void onItemClick(int position, TestGroup item, Boolean extra, View view) {
                boolean selected = (Boolean.TRUE == extra);
                if (!selected) {
                    // new group clicked
                    dismissSnackbar();

                    dbHelper.markTestGroupCurrent(item);
                    currentTestGroup = item;
                    testGroupListAdapter.getExtras().setExclusive(position, Boolean.TRUE);

                    initTestList();
                }

                // close the drawer with a delay for a nicer affect
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        drawerLayout.closeDrawer(Gravity.START);
                    }
                }, UI_UPDATE_DELAY);
            }
        };

        onTestClickListener = new ListAdapter.OnItemClickListener<Test, TaskFilter>() {

            @Override
            public void onItemClick(int position, Test item, TaskFilter extra, View view) {
                TaskFilter taskFilter = TestListAdapter.TestViewHolder.taskFilterForView(view);
                if (multitestMode == null) {
                    // if not in multitest mode, start the test immediately
                    startTest(item, taskFilter);
                } else {
                    applyMultitestEvent(position, extra, taskFilter);
                }
            }
        };

        onTestLongClickListener = new ListAdapter.OnItemLongClickListener<Test, TaskFilter>() {

            @Override
            public void onItemLongClick(int position, Test item, TaskFilter extra, View view) {
                if (multitestMode == null) {
                    multitestMode = startActionMode(multitestModeCallback);
                }

                TaskFilter taskFilter = TestListAdapter.TestViewHolder.taskFilterForView(view);
                applyMultitestEvent(position, extra, taskFilter);
            }
        };

        multitestModeCallback = new ActionMode.Callback() {

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                startMultitestButton.setVisibility(View.VISIBLE);

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                multitestMode = null;

                testListAdapter.getExtras().clearAll();

                startMultitestButton.setVisibility(View.GONE);
            }
        };

        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, value, true);
        snackbarBackgroundColor = value.data;

        reenableTestClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // disable swiping while the undo is being processed
                testList.setSwipingEnabled(false);

                dbHelper.setTestEnabled(lastDisabledTest, true);
                dbHelper.refreshTestGroup(currentTestGroup);

                testListAdapter.addItem(lastDisabledTestPosition, lastDisabledTest);

                lastDisabledTest = null;
                lastDisabledTestPosition = -1;
            }
        };

        initTestGroupList();
        initTestList();

        if (savedInstanceState != null) {
            boolean multitestModeOn = savedInstanceState.getBoolean(MULTITEST_MODE_ON_BUNDLE_KEY, false);
            if (multitestModeOn) {
                int[] selectionPositions = savedInstanceState.getIntArray(SELECTION_POSITIONS_BUNDLE_KEY);
                TaskFilter[] selections = (TaskFilter[]) savedInstanceState.getSerializable(SELECTIONS_BUNDLE_KEY);
                for (int i = 0; i < selectionPositions.length; ++i) {
                    testListAdapter.getExtras().set(selectionPositions[i], selections[i]);
                }
                multitestMode = startActionMode(multitestModeCallback);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        drawerToggle.syncState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (testListAdapter != null) {
            // the tests might have changed, e.g. all favorites may have been deselected etc.
            // refresh the test group, the tests and their visible views
            // consider all tests as we might be coming back from search
            for (int i = 0; i < testListAdapter.getItemCount(); ++i) {
                Test test = testListAdapter.getItem(i);
                TaskFilter taskFilter = testListAdapter.getExtras().get(i);
                Set<TaskFilter> previouslyAvailableTaskFilters = test.getAvailableTaskFilters();

                // refresh test information
                dbHelper.refreshTest(test);

                Set<TaskFilter> availableTaskFilters = test.getAvailableTaskFilters();
                // check if the test view is still valid, i.e. if all previous filters are still available
                if (!availableTaskFilters.equals(previouslyAvailableTaskFilters)) {
                    if (!availableTaskFilters.contains(taskFilter)) {
                        // the previously chosen filter is not available any longer
                        // so clear it; the view is refreshed as side effect
                        testListAdapter.getExtras().clear(i);
                    } else {
                        // some other filter is not available, just refresh
                        testList.getAdapter().notifyItemChanged(i);
                    }
                }
            }

            if (multitestMode != null) {
                updateMultitestMode();
            } else {
                // single test was started or activity initially shown
                testListAdapter.getExtras().clearAll();
            }
        }

        // if resuming after import, nicely add the row
        if (newlyAddedTestGroup != null) {
            final int testGroupCountBefore = testGroupListAdapter.getItemCount();
            final TestGroup testGroup = newlyAddedTestGroup;
            newlyAddedTestGroup = null;

            // update the UI after a delay for a nicer effect without flicker
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    testGroupListAdapter.addItem(0, testGroup);

                    if (testGroupCountBefore == 0) {
                        refreshDrawerViews();
                    } else if (testGroupListLayout.findFirstCompletelyVisibleItemPosition() == 0) {
                        testGroupListLayout.scrollToPosition(0);
                    }
                }
            }, UI_UPDATE_DELAY);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mnemonic, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final MenuItem searchMenuItem = menu.findItem(R.id.mnemonic_action_search);
        final SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, TaskSearchActivity.class)));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();
                searchMenuItem.collapseActionView();

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Map<TaskFilter, MenuItem> filterMenuItems = new EnumMap<>(TaskFilter.class);
        for (TaskFilter taskFilter : TaskFilter.values()) {
            int menuItemId;
            switch (taskFilter) {
                case FAVORITE:
                    menuItemId = R.id.mnemonic_action_favorite;
                    break;

                case COMMENTED:
                    menuItemId = R.id.mnemonic_action_commented;
                    break;

                default:
                    menuItemId = R.id.mnemonic_action_all;
                    break;
            }

            filterMenuItems.put(taskFilter, menu.findItem(menuItemId));
        }

        MenuItem searchMenuItem = menu.findItem(R.id.mnemonic_action_search);
        MenuItem enableAllTestsMenuItem = menu.findItem(R.id.mnemonic_action_enable_all_tests);
        MenuItem deleteTestGroupMenuItem = menu.findItem(R.id.mnemonic_action_delete_test_grup);

        if (currentTestGroup != null) {
            Set<TaskFilter> existingTaskFilters = dbHelper.getExistingTaskFiltersForTestGroup(currentTestGroup);
            for (Map.Entry<TaskFilter, MenuItem> entry : filterMenuItems.entrySet()) {
                entry.getValue().setVisible(existingTaskFilters.contains(entry.getKey()));
            }
            enableAllTestsMenuItem.setVisible(currentTestGroup.disabledCount() > 0);
            deleteTestGroupMenuItem.setVisible(true);
        } else {
            for (MenuItem menuItem : filterMenuItems.values()) {
                menuItem.setVisible(false);
            }
            enableAllTestsMenuItem.setVisible(false);
            deleteTestGroupMenuItem.setVisible(false);
        }

        searchMenuItem.setVisible(filterMenuItems.get(TaskFilter.ALL).isVisible());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importTestGroup(uri);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (multitestMode != null) {
            outState.putBoolean(MULTITEST_MODE_ON_BUNDLE_KEY, true);
            int[] selectionPositions = testListAdapter.getExtras().getPositions();
            outState.putIntArray(SELECTION_POSITIONS_BUNDLE_KEY, selectionPositions);
            TaskFilter[] selections = new TaskFilter[selectionPositions.length];
            for (int i = 0; i < selectionPositions.length; ++i) {
                selections[i] = testListAdapter.getExtras().get(selectionPositions[i]);
            }
            outState.putSerializable(SELECTIONS_BUNDLE_KEY, selections);
        }
    }

    public void startAll(MenuItem menuItem) {
        startAll(TaskFilter.ALL);
    }

    public void startAllFavorite(MenuItem menuItem) {
        startAll(TaskFilter.FAVORITE);
    }

    public void startAllCommented(MenuItem menuItem) {
        startAll(TaskFilter.COMMENTED);
    }

    public void enableAllTests(MenuItem menuItem) {
        dismissSnackbar();

        // disable swiping while adds/moves are being processed
        testList.setSwipingEnabled(false);

        dbHelper.enableAllTestsForTestGroup(currentTestGroup);

        initTestList();
    }

    public void deleteTestGroup(MenuItem menuItem) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.alert_delete_test_group)
                .setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteTestGroup(currentTestGroup);
                        testGroupListAdapter.deleteItem(testGroupListAdapter.getExtras().getPositions()[0]);
                        currentTestGroup = null;
                        testListAdapter = null;
                        testList.setAdapter(null);

                        updateTitle();
                        showEmptyTestListMessage(R.string.no_test_group_chosen);

                        if (testGroupListAdapter.getItemCount() == 0) {
                            refreshDrawerViews();
                        }
                    }
                })
                .setNegativeButton(R.string.alert_no, null)
                .show();
    }

    public void addTestGroup(MenuItem menuItem) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, 0);
    }

    private void applyMultitestEvent(int position, TaskFilter oldTaskFilter, TaskFilter newTaskFilter) {
        if (oldTaskFilter == newTaskFilter) {
            // the filter chosen, remove the selection
            testListAdapter.getExtras().clear(position);
        } else {
            // some other filter chosen, replace the selection
            testListAdapter.getExtras().set(position, newTaskFilter);
        }

        updateMultitestMode();
    }

    private void updateMultitestMode() {
        int selectionCount = testListAdapter.getExtras().getCount();
        if (selectionCount > 0) {
            multitestMode.setTitle(String.format(getString(R.string.multitest_mode_title), selectionCount));
        } else {
            // nothing selected any more, end multitest mode
            multitestMode.finish();
        }
    }

    public void startMultitest(View view) {
        startSelected();
    }

    private void initTestGroupList() {
        List<TestGroup> testGroups = dbHelper.getTestGroups();
        Extras<Boolean> extras = new Extras<>(1);
        int i = 0;
        for (TestGroup testGroup : testGroups) {
            if (testGroup.isCurrent()) {
                currentTestGroup = testGroup;
                extras.set(i, Boolean.TRUE);
                break;
            }
            ++i;
        }

        testGroupListAdapter = new TestGroupListAdapter(testGroups, extras, getString(R.string.default_test_group_name), this);
        testGroupListAdapter.setOnItemClickListener(onTestGroupClickListener);

        refreshDrawerViews();
    }

    private void refreshDrawerViews() {
        if (testGroupListAdapter.getItemCount() > 0) {
            testGroupList.setVisibility(View.VISIBLE);
            emptyTestGroupListInfoLabel.setVisibility(View.GONE);

            testGroupList.setAdapter(testGroupListAdapter);
        } else {
            testGroupList.setVisibility(View.GONE);
            emptyTestGroupListInfoLabel.setVisibility(View.VISIBLE);
        }
    }

    private void initTestList() {
        if (currentTestGroup == null) {
            showEmptyTestListMessage(R.string.no_test_group_chosen);
        } else {
            List<Test> tests = dbHelper.getEnabledTestsForTestGroup(currentTestGroup);

            if (!tests.isEmpty()) {
                testList.setVisibility(View.VISIBLE);
                emptyTestListInfoLabel.setVisibility(View.GONE);
                emptyTestListInfoLabel.setText(null);

                testListAdapter = new TestListAdapter(tests, new Extras<TaskFilter>(tests.size()), getString(R.string.default_test_name));
                testListAdapter.setOnItemClickListener(onTestClickListener);
                testListAdapter.setOnItemLongClickListener(onTestLongClickListener);
                testList.swapAdapter(testListAdapter, false);
            } else {
                showEmptyTestListMessage(currentTestGroup.getTestCount() > 0 ?
                        R.string.all_tests_disabled_in_test_group : R.string.no_tests_in_test_group);
            }
        }

        updateTitle();
    }

    private void updateTitle() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if (currentTestGroup == null) {
                actionBar.setTitle(null);
            } else {
                String testGroupName = currentTestGroup.getName();
                actionBar.setTitle(testGroupName != null ? testGroupName : getString(R.string.default_test_group_name));
            }
        }
    }

    private void showEmptyTestListMessage(@StringRes int id) {
        testList.setVisibility(View.GONE);
        emptyTestListInfoLabel.setVisibility(View.VISIBLE);
        emptyTestListInfoLabel.setText(id);
    }

    private void importTestGroup(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null);
             InputStream inputStream = getContentResolver().openInputStream(uri)) {
            String name = null;
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }

            newlyAddedTestGroup = new Importer(dbHelper).importTestGroup(name, inputStream);
        } catch (IOException | ImportException e) {
            Log.e(TAG, "error importing data", e);
            showSnackbar(getString(R.string.import_error), null, null);
        }
    }

    private void startTest(Test test, TaskFilter taskFilter) {
        List<Task> filteredTasks = dbHelper.getTasksForTest(test, taskFilter);
        ArrayList<Task> tasks = new ArrayList<>(filteredTasks.size());
        int pagesCount = 0;
        for (Task task : filteredTasks) {
            tasks.add(task);
            pagesCount += task.getPagesCount();
        }

        start(tasks, pagesCount);
    }

    private void startAll(TaskFilter taskFilter) {
        List<List<Task>> taskLists = new ArrayList<>(testListAdapter.getItemCount());
        int taskCount = 0;
        for (int i = 0; i < testListAdapter.getItemCount(); ++i) {
            Test test = testListAdapter.getItem(i);
            if (test.getAvailableTaskFilters().contains(taskFilter)) {
                List<Task> filteredTasks = dbHelper.getTasksForTest(test, taskFilter);
                taskLists.add(filteredTasks);
                taskCount += filteredTasks.size();
            }
        }

        start(taskLists, taskCount);
    }

    private void startSelected() {
        List<List<Task>> taskLists = new ArrayList<>(testListAdapter.getExtras().getCount());
        int taskCount = 0;
        for (int position : testListAdapter.getExtras().getPositions()) {
            Test test = testListAdapter.getItem(position);
            TaskFilter taskFilter = testListAdapter.getExtras().get(position);
            List<Task> tasks = dbHelper.getTasksForTest(test, taskFilter);
            taskLists.add(tasks);
            taskCount += tasks.size();
        }

        start(taskLists, taskCount);
    }

    private void start(List<List<Task>> taskLists, int taskCount) {
        ArrayList<Task> tasks = new ArrayList<>(taskCount);
        int pagesCount = 0;
        for (List<Task> filteredTasks : taskLists) {
            for (Task task : filteredTasks) {
                tasks.add(task);
                pagesCount += task.getPagesCount();
            }
        }

        start(tasks, pagesCount);
    }

    private void start(ArrayList<Task> tasks, int pagesCount) {
        if (tasks.isEmpty()) {
            showSnackbar(getString(R.string.no_tasks), null, null);
            if (multitestMode == null) {
                // single test was started
                testListAdapter.getExtras().clearAll();
            }
        } else {
            Intent intent = new Intent(this, TestActivity.class);
            intent.putExtra(TestActivity.TASKS_EXTRA, tasks);
            intent.putExtra(TestActivity.PAGES_COUNT_EXTRA, pagesCount);
            intent.putExtra(TestActivity.RANDOMIZE_EXTRA, true);

            startActivity(intent);
        }
    }

    private void showSnackbar(@NonNull String text, @Nullable String actionText, @Nullable View.OnClickListener actionListener) {
        currentSnackbar = Snackbar.make(testList, text, Snackbar.LENGTH_LONG);
        currentSnackbar.setAction(actionText, actionListener);
        currentSnackbar.getView().setBackgroundColor(snackbarBackgroundColor);
        currentSnackbar.show();
        // TODO: it would be perfect to get a callback when the snackbar is dismised automatically
        // TODO: so that the saved snackbar reference and the disabled test are cleared
    }

    private void dismissSnackbar() {
        if (currentSnackbar != null) {
            currentSnackbar.dismiss();
            currentSnackbar = null;
        }
    }
}
