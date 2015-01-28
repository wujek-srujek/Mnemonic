package com.mnemonic;


import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Toolbar;

import com.mnemonic.db.DbHelper;
import com.mnemonic.db.Task;
import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;
import com.mnemonic.db.TestGroup;
import com.mnemonic.importer.ImportException;
import com.mnemonic.importer.Importer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class MnemonicActivity extends Activity implements OnTestClickListener, OnTestLongClickListener, ActionMode.Callback {

    private static final String TAG = MnemonicActivity.class.getSimpleName();

    private final static String MULTITEST_MODE_ON_BUNDLE_KEY = "multitestModeOn";

    private final static String SELECTION_POSITIONS_BUNDLE_KEY = "selectedIndices";

    private final static String SELECTIONS_BUNDLE_KEY = "selections";

    private DbHelper dbHelper;

    private RecyclerView testList;

    private TestListAdapter testListAdapter;

    private ImageButton startMultitestButton;

    private ActionMode multitestMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mnemonic);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        dbHelper = new DbHelper(this);

        testList = (RecyclerView) findViewById(R.id.test_list);
        RecyclerView.LayoutManager layoutManager =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ?
                        new LinearLayoutManager(this) : new GridLayoutManager(this, 2);
        testList.setLayoutManager(layoutManager);

        startMultitestButton = (ImageButton) findViewById(R.id.start_multitest_button);

        initUi();

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
    protected void onResume() {
        super.onResume();

        if (testListAdapter == null) {
            return;
        }

        if (testListAdapter.getSelectionCount() > 0) {
            // the tests might have changed, e.g. all favorites may have been deselected etc.
            // refresh the tests and their visible views
            for (int position : testListAdapter.getSelectionPositions()) {
                Test test = testListAdapter.getItem(position);
                TaskFilter taskFilter = testListAdapter.getSelection(position);
                Set<TaskFilter> previouslyAvailableTaskFilters = test.availableTaskFilters();

                // refresh test information
                dbHelper.refreshTest(test);

                Set<TaskFilter> availableTaskFilters = test.availableTaskFilters();
                // check if the test view is still valid, i.e. if all previous filters are still available
                if (!availableTaskFilters.equals(previouslyAvailableTaskFilters)) {
                    if (!availableTaskFilters.contains(taskFilter)) {
                        // the previously chosen filter is not available any longer
                        // so clear it; the view is refreshed as side effect
                        testListAdapter.clearSelection(position);
                    } else {
                        // some other filter is not available, just refresh
                        testList.getAdapter().notifyItemChanged(position);
                    }
                }
            }
        }

        if (multitestMode != null) {
            int selectionCount = testListAdapter.getSelectionCount();
            if (selectionCount > 0) {
                updateMultitestTitle(selectionCount);
            } else {
                multitestMode.finish();
            }
        } else {
            // single test was started
            testListAdapter.clearSelections();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mnemonic, menu);

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importTests(uri);
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
        startAll(String.format(getString(R.string.meta_name_braces), getString(R.string.action_all_tasks)),
                TaskFilter.ALL);
    }

    public void startAllFavorite(MenuItem menuItem) {
        startAll(String.format(getString(R.string.meta_name_braces), getString(R.string.action_all_favorite)),
                TaskFilter.FAVORITE);
    }

    public void startAllCommented(MenuItem menuItem) {
        startAll(String.format(getString(R.string.meta_name_braces), getString(R.string.action_all_commented)),
                TaskFilter.COMMENTED);
    }

    public void browse(MenuItem menuItem) {
        browse();
    }

    @Override
    public void onTestClick(int position, Test test, TaskFilter taskFilter) {
        if (multitestMode == null) {
            // if not in multitest mode, start the test immediately
            testListAdapter.setSelection(position, taskFilter);
            startSelected(null);
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

        int selectionCount = testListAdapter.getSelectionCount();
        if (selectionCount > 0) {
            updateMultitestTitle(selectionCount);
        } else {
            // nothing selected any more, end multitest mode
            multitestMode.finish();
        }
    }

    private void updateMultitestTitle(int selectionCount) {
        multitestMode.setTitle(String.format(getString(R.string.multitest_mode_title), selectionCount));
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
        testListAdapter.clearSelections();

        multitestMode = null;

        startMultitestButton.setVisibility(View.GONE);
    }

    public void startMultitest(View view) {
        startSelected(null);
    }

    private void importTests(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null);
             InputStream inputStream = getContentResolver().openInputStream(uri)) {
            String name = null;
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
            new Importer(dbHelper).importTests(name, inputStream);
            initUi();
        } catch (IOException | ImportException e) {
            Log.e(TAG, "error importing data", e);
            Toast.makeText(MnemonicActivity.this, R.string.import_error, Toast.LENGTH_LONG).show();
        }
    }

    private void initUi() {
        List<Test> tests = new ArrayList<>(dbHelper.getTestCount());
        List<TestGroup> testGroups = dbHelper.getTestGroups();
        for (TestGroup testGroup : testGroups) {
            tests.addAll(dbHelper.getTests(testGroup));
        }

        View infoLabel = findViewById(R.id.empty_test_list_info_label);
        if (!tests.isEmpty()) {
            testList.setVisibility(View.VISIBLE);
            infoLabel.setVisibility(View.GONE);

            testListAdapter = new TestListAdapter(tests, getString(R.string.default_test_name));
            testListAdapter.setOnTestClickListener(this);
            testListAdapter.setOnTestLongClickListener(this);
            testList.setAdapter(testListAdapter);
        } else {
            testList.setVisibility(View.GONE);
            infoLabel.setVisibility(View.VISIBLE);
        }
    }

    private void browse() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, 0);
    }

    private void startAll(String testName, TaskFilter taskFilter) {
        for (int i = 0; i < testListAdapter.getItemCount(); ++i) {
            Test test = testListAdapter.getItem(i);
            if (test.availableTaskFilters().contains(taskFilter)) {
                testListAdapter.setSelection(i, taskFilter);
            }
        }

        startSelected(testName);
    }

    private void startSelected(String testName) {
        ArrayList<Task> tasks;

        int[] selectionPositions = testListAdapter.getSelectionPositions();
        if (testListAdapter.getSelectionCount() == 1) {
            Test test = testListAdapter.getItem(selectionPositions[0]);
            TaskFilter taskFilter = testListAdapter.getSelection(selectionPositions[0]);

            if (testName == null) {
                testName = test.getName() != null ? test.getName() : getString(R.string.default_test_name);
            }
            tasks = new ArrayList<>(dbHelper.getTasks(test, taskFilter));
        } else {
            if (testName == null) {
                testName = getString(R.string.multitest_title);
            }

            int taskCount = 0;
            for (int position : selectionPositions) {
                taskCount += testListAdapter.getItem(position).getTaskCount();
            }
            tasks = new ArrayList<>(taskCount);
            for (int position : selectionPositions) {
                Test test = testListAdapter.getItem(position);
                TaskFilter taskFilter = testListAdapter.getSelection(position);
                tasks.addAll(dbHelper.getTasks(test, taskFilter));
            }
        }

        if (tasks.isEmpty()) {
            Toast.makeText(this, getString(R.string.empty_test_warning), Toast.LENGTH_LONG).show();
            if (multitestMode == null) {
                // single test was started
                testListAdapter.clearSelections();
            }
        } else {
            Intent intent = new Intent(MnemonicActivity.this, TestActivity.class);
            intent.putExtra(TestActivity.TEST_NAME_EXTRA, testName);
            intent.putExtra(TestActivity.TASKS_EXTRA, tasks);

            startActivity(intent);
        }
    }
}
