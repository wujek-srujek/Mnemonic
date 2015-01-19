package com.mnemonic;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.Toolbar;

import com.mnemonic.db.DbHelper;
import com.mnemonic.db.Test;
import com.mnemonic.db.TestGroup;
import com.mnemonic.importer.ImportException;
import com.mnemonic.importer.Importer;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class MnemonicActivity extends Activity implements OnTestChoiceListener {

    private static final String TAG = MnemonicActivity.class.getSimpleName();

    private DbHelper dbHelper;

    private RecyclerView testList;

    private TestChoiceInfo testChoiceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mnemonic);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        dbHelper = new DbHelper(this);

        testList = (RecyclerView) findViewById(R.id.test_list);
        testList.setLayoutManager(new LinearLayoutManager(this));

        initUi();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (testChoiceInfo != null) {
            // the test might have changed, e.g. all favorites may have
            // been unchecked - need to refresh it
            dbHelper.refreshTest(testChoiceInfo.test);
            testList.getAdapter().notifyItemChanged(testChoiceInfo.position);

            testChoiceInfo = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mnemonic, menu);

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            importTests(data.getData().getPath());

            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void browse(MenuItem menuItem) {
        browse();
    }

    @Override
    public void onTestChoice(TestChoiceInfo testChoiceInfo) {
        this.testChoiceInfo = testChoiceInfo;

        Intent intent = new Intent(MnemonicActivity.this, TestActivity.class);
        intent.putExtra(TestActivity.TEST_EXTRA, testChoiceInfo.test);
        intent.putExtra(TestActivity.TASK_FILTER_EXTRA, testChoiceInfo.taskFilter);

        startActivity(intent);
    }

    private void importTests(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            new Importer(dbHelper).importTests(filePath, inputStream);
            initUi();
        } catch (IOException | ImportException e) {
            Log.e(TAG, "error importing data", e);
            Toast.makeText(MnemonicActivity.this, R.string.import_error, Toast.LENGTH_LONG).show();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                    // ignored
                }
            }
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

            testList.setAdapter(new TestListAdapter(tests, getString(R.string.default_test_name), this));
        } else {
            testList.setVisibility(View.GONE);
            infoLabel.setVisibility(View.VISIBLE);
        }
    }

    private void browse() {
        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);

        startActivityForResult(intent, 0);
    }
}
