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
import android.support.annotation.StringRes;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.mnemonic.db.DbHelper;
import com.mnemonic.db.Task;
import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;
import com.mnemonic.db.TestGroup;
import com.mnemonic.importer.ImportException;
import com.mnemonic.importer.Importer;
import com.mnemonic.view.HorizontallySwipeableRecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MnemonicActivity extends Activity implements
        TestGroupListAdapter.OnTestGroupClickListener,
        TestListAdapter.OnTestClickListener, TestListAdapter.OnTestLongClickListener,
        ActionMode.Callback, HorizontallySwipeableRecyclerView.SwipeListener {

    private static final String TAG = MnemonicActivity.class.getSimpleName();

    private static final String MULTITEST_MODE_ON_BUNDLE_KEY = "multitestModeOn";

    private static final String SELECTION_POSITIONS_BUNDLE_KEY = "selectedIndices";

    private static final String SELECTIONS_BUNDLE_KEY = "selections";

    private static final long UI_UPDATE_DELAY = 250;

    private Handler handler;

    private long afterSwipeAnimationDuration;

    private DbHelper dbHelper;

    private ActionBarDrawerToggle drawerToggle;

    private DrawerLayout drawerLayout;

    private RecyclerView testGroupList;

    private LinearLayoutManager testGroupListLayout;

    private View emptyTestGroupListInfoLabel;

    private TestGroupListAdapter testGroupListAdapter;

    private HorizontallySwipeableRecyclerView testList;

    private TextView emptyTestListInfoLabel;

    private TestListAdapter testListAdapter;

    private ImageButton startMultitestButton;

    private ActionMode multitestMode;

    private TestGroup currentTestGroup;

    private TestGroup newlyAddedTestGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mnemonic);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        handler = new Handler(Looper.getMainLooper());
        afterSwipeAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        dbHelper = MnemonicApplication.getDbHelper();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
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
        testList.setSwipeListener(this);
        testList.setItemAnimator(new DefaultItemAnimator() {

            @Override
            public void onRemoveFinished(RecyclerView.ViewHolder item) {
                // at this point, the remove animation is done
                onDismissComplete(item.itemView);
            }
        });

        emptyTestListInfoLabel = (TextView) findViewById(R.id.empty_test_list_info_label);
        startMultitestButton = (ImageButton) findViewById(R.id.start_multitest_button);

        initTestGroupList();
        initTestList();

        if (savedInstanceState != null) {
            boolean multitestModeOn = savedInstanceState.getBoolean(MULTITEST_MODE_ON_BUNDLE_KEY, false);
            if (multitestModeOn) {
                int[] selectionPositions = savedInstanceState.getIntArray(SELECTION_POSITIONS_BUNDLE_KEY);
                TaskFilter[] selections = (TaskFilter[]) savedInstanceState.getSerializable(SELECTIONS_BUNDLE_KEY);
                for (int i = 0; i < selectionPositions.length; ++i) {
                    testListAdapter.setSelection(selectionPositions[i], selections[i]);
                }
                multitestMode = startActionMode(this);
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
                TaskFilter taskFilter = testListAdapter.getSelection(i);
                Set<TaskFilter> previouslyAvailableTaskFilters = test.getAvailableTaskFilters();

                // refresh test information
                dbHelper.refreshTest(test);

                Set<TaskFilter> availableTaskFilters = test.getAvailableTaskFilters();
                // check if the test view is still valid, i.e. if all previous filters are still available
                if (!availableTaskFilters.equals(previouslyAvailableTaskFilters)) {
                    if (!availableTaskFilters.contains(taskFilter)) {
                        // the previously chosen filter is not available any longer
                        // so clear it; the view is refreshed as side effect
                        testListAdapter.clearSelection(i);
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
                testListAdapter.clearSelections();
            }

            invalidateOptionsMenu();
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
        SearchView searchView = (SearchView) menu.findItem(R.id.mnemonic_action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, TaskSearchActivity.class)));

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
            int[] selectionPositions = testListAdapter.getSelectionPositions();
            outState.putIntArray(SELECTION_POSITIONS_BUNDLE_KEY, selectionPositions);
            TaskFilter[] selections = new TaskFilter[selectionPositions.length];
            for (int i = 0; i < selectionPositions.length; ++i) {
                selections[i] = testListAdapter.getSelection(selectionPositions[i]);
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
        enableAllTests();
    }

    public void deleteTestGroup(MenuItem menuItem) {
        deleteTestGroup();
    }

    public void browse(MenuItem menuItem) {
        browse();
    }

    @Override
    public void onTestGroupClick(int position, TestGroup testGroup) {
        if (position == testGroupListAdapter.getSelection()) {
            return;
        }

        dbHelper.markTestGroupCurrent(testGroup);
        if (currentTestGroup != null) {
            // previous current group is not current any longer
            dbHelper.refreshTestGroup(currentTestGroup);
        }
        currentTestGroup = testGroup;
        testGroupListAdapter.setSelection(position);

        initTestList();

        // close the drawer with a delay
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                drawerLayout.closeDrawer(Gravity.START);
            }
        }, UI_UPDATE_DELAY);
    }

    @Override
    public void onTestClick(int position, Test test, TaskFilter taskFilter) {
        if (multitestMode == null) {
            // if not in multitest mode, start the test immediately
            testListAdapter.setSelection(position, taskFilter);
            startSelected();
        } else {
            applyMultitestEvent(position, taskFilter);
        }
    }

    @Override
    public void onTestLongClick(int position, Test test, TaskFilter taskFilter) {
        if (multitestMode == null) {
            multitestMode = startActionMode(this);
        }

        applyMultitestEvent(position, taskFilter);
    }

    private void applyMultitestEvent(int position, TaskFilter taskFilter) {
        TaskFilter currentTaskFilter = testListAdapter.getSelection(position);
        if (currentTaskFilter == taskFilter) {
            // the same view clicked, remove the selection
            testListAdapter.clearSelection(position);
        } else {
            // some other view clicked, replace the selection
            testListAdapter.setSelection(position, taskFilter);
        }

        updateMultitestMode();
    }

    private void updateMultitestMode() {
        int selectionCount = testListAdapter.getSelectionCount();
        if (selectionCount > 0) {
            multitestMode.setTitle(String.format(getString(R.string.multitest_mode_title), selectionCount));
        } else {
            // nothing selected any more, end multitest mode
            multitestMode.finish();
        }
    }

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

        testListAdapter.clearSelections();

        startMultitestButton.setVisibility(View.GONE);
    }

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
                disableTest(position);
            }
        }).start();
    }

    private void onDismissComplete(View view) {
        // reset the view after it is removed to be eligible for reuse
        view.setAlpha(1.F);
        view.setTranslationX(0.F);

        // enable swiping again after dismissal has been processed
        testList.setSwipingEnabled(true);

        dbHelper.refreshTestGroup(currentTestGroup);

        invalidateOptionsMenu();

        // if there are no enabled tests, show the message
        if (testListAdapter.getItemCount() == 0) {
            showEmptyTestListMessage(R.string.all_tests_disabled_in_test_group);
        }
    }

    public void startMultitest(View view) {
        startSelected();
    }

    private void initTestGroupList() {
        List<TestGroup> testGroups = dbHelper.getTestGroups();
        testGroupListAdapter = new TestGroupListAdapter(this, testGroups);
        testGroupListAdapter.setOnTestGroupClickListener(this);
        int currentSelection = testGroupListAdapter.getSelection();
        currentTestGroup = currentSelection != RecyclerView.NO_POSITION ?
                testGroupListAdapter.getItem(currentSelection) : null;

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

                testListAdapter = new TestListAdapter(tests, getString(R.string.default_test_name));
                testListAdapter.setOnTestClickListener(this);
                testListAdapter.setOnTestLongClickListener(this);
                testList.setAdapter(testListAdapter);
            } else {
                showEmptyTestListMessage(currentTestGroup.getTestCount() > 0 ?
                        R.string.all_tests_disabled_in_test_group : R.string.no_tests_in_test_group);
            }
        }

        updateTitle();
        invalidateOptionsMenu();
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

    private void enableAllTests() {
        dbHelper.enableAllTestsForTestGroup(currentTestGroup);

        initTestList();
    }

    private void deleteTestGroup() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.alert_delete_test_group)
                .setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteTestGroup(currentTestGroup);
                        testGroupListAdapter.deleteItem(testGroupListAdapter.getSelection());
                        currentTestGroup = null;
                        testListAdapter = null;
                        testList.setAdapter(null);

                        updateTitle();
                        invalidateOptionsMenu();
                        showEmptyTestListMessage(R.string.no_test_group_chosen);

                        if (testGroupListAdapter.getItemCount() == 0) {
                            refreshDrawerViews();
                        }
                    }
                })
                .setNegativeButton(R.string.alert_no, null)
                .show();
    }

    private void browse() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, 0);
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
            Toast.makeText(MnemonicActivity.this, R.string.import_error, Toast.LENGTH_LONG).show();
        }
    }

    private void startAll(TaskFilter taskFilter) {
        for (int i = 0; i < testListAdapter.getItemCount(); ++i) {
            Test test = testListAdapter.getItem(i);
            if (test.getAvailableTaskFilters().contains(taskFilter)) {
                testListAdapter.setSelection(i, taskFilter);
            }
        }

        startSelected();
    }

    private void startSelected() {
        ArrayList<Task> tasks;
        int pagesCount;

        int taskCount = 0;
        for (int position : testListAdapter.getSelectionPositions()) {
            taskCount += testListAdapter.getItem(position).getTaskCount();
        }

        pagesCount = 0;
        tasks = new ArrayList<>(taskCount);
        for (int position : testListAdapter.getSelectionPositions()) {
            Test test = testListAdapter.getItem(position);
            TaskFilter taskFilter = testListAdapter.getSelection(position);
            tasks.addAll(dbHelper.getTasksForTest(test, taskFilter));
            pagesCount += test.getPagesCount();
        }

        if (tasks.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_tasks), Toast.LENGTH_LONG).show();
            if (multitestMode == null) {
                // single test was started
                testListAdapter.clearSelections();
            }
        } else {
            Intent intent = new Intent(this, TestActivity.class);
            intent.putExtra(TestActivity.TASKS_EXTRA, tasks);
            intent.putExtra(TestActivity.PAGES_COUNT_EXTRA, pagesCount);
            intent.putExtra(TestActivity.RANDOMIZE_EXTRA, true);

            startActivity(intent);
        }
    }

    private void disableTest(int position) {
        Test test = testListAdapter.getItem(position);
        dbHelper.setTestEnabled(test, false);

        testListAdapter.removeItem(position);
        if (multitestMode != null) {
            // maybe a selected test was disabled
            updateMultitestMode();
        }
    }
}
