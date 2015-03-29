package com.mnemonic;


import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.mnemonic.db.DbHelper;
import com.mnemonic.db.SearchException;
import com.mnemonic.db.Task;
import com.mnemonic.db.TestGroup;
import com.mnemonic.view.recycler.ListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TaskSearchActivity extends Activity {

    private static final String TAG = TaskSearchActivity.class.getSimpleName();

    private DbHelper dbHelper;

    private RecyclerView taskList;

    private View noResultsLabel;

    private SearchView searchView;

    private List<Task> foundTasks;

    private TestGroup testGroup;

    private ListAdapter.OnItemClickListener<Task, Void> onTaskClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task_search);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        dbHelper = MnemonicApplication.getDbHelper();

        taskList = (RecyclerView) findViewById(R.id.task_list);
        taskList.setLayoutManager(new LinearLayoutManager(this));

        noResultsLabel = findViewById(R.id.no_results_info_label);

        testGroup = dbHelper.getCurrentTestGroup();

        onTaskClickListener = new ListAdapter.OnItemClickListener<Task, Void>() {

            @Override
            public void onItemClick(int position, Task item, Void extra, View view) {
                int pagesCount = 0;
                ArrayList<Task> tasks = new ArrayList<>(foundTasks.size());
                for (Task foundTask : foundTasks) {
                    pagesCount += foundTask.getPagesCount();
                    tasks.add(foundTask);
                }

                Intent intent = new Intent(TaskSearchActivity.this, TestActivity.class);
                intent.putParcelableArrayListExtra(TestActivity.TASKS_EXTRA, tasks);
                intent.putExtra(TestActivity.PAGES_COUNT_EXTRA, pagesCount);
                intent.putExtra(TestActivity.START_TASK_INDEX_EXTRA, position);

                startActivity(intent);
            }
        };

        showSearchResults();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_task_search, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchMenuItem = menu.findItem(R.id.task_search_action_search);
        searchMenuItem.expandActionView();

        searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQuery(getIntent().getStringExtra(SearchManager.QUERY), false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus();

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.clearFocus();

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);

        showSearchResults();
    }

    private void showSearchResults() {
        Intent intent = getIntent();
        String query = intent.getStringExtra(SearchManager.QUERY);
        try {
            foundTasks = dbHelper.getTasksForQuery(query, testGroup);
        } catch (SearchException e) {
            Log.e(TAG, "error searching for tasks", e);
            Toast.makeText(this, R.string.invalid_query, Toast.LENGTH_LONG).show();

            foundTasks = Collections.emptyList();
        }

        if (!foundTasks.isEmpty()) {
            taskList.setVisibility(View.VISIBLE);
            noResultsLabel.setVisibility(View.GONE);

            TaskListAdapter taskListAdapter = new TaskListAdapter(foundTasks);
            taskListAdapter.setOnItemClickListener(onTaskClickListener);
            taskList.setAdapter(taskListAdapter);
        } else {
            taskList.setVisibility(View.GONE);
            noResultsLabel.setVisibility(View.VISIBLE);

            taskList.setAdapter(null);
        }
    }
}
