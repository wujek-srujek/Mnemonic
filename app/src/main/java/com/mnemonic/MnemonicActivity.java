package com.mnemonic;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.File;
import java.util.List;


public class MnemonicActivity extends Activity {

    private final static String LAST_FILE_PATH_KEY = "lastFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mnemonic);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        ((ListView) findViewById(R.id.test_list)).setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Test test = (Test) parent.getItemAtPosition(position);
                if (test.isEmpty()) {
                    Toast.makeText(MnemonicActivity.this,
                            String.format(getString(R.string.empty_test_format), test.getName()), Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Intent intent = new Intent(MnemonicActivity.this, TestActivity.class);
                    intent.putExtra(TestActivity.TEST_EXTRA, test);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_mnemonic, menu);

        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String filePath = data.getData().getPath();
            SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
            editor.putString(LAST_FILE_PATH_KEY, filePath);
            editor.apply();

            initializeTests(filePath);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String filePath = getPreferences(Context.MODE_PRIVATE).getString(LAST_FILE_PATH_KEY, null);
        if (filePath != null) {
            initializeTests(filePath);
        }
    }

    public void chooseDirectory(MenuItem item) {
        String startPath = getPreferences(Context.MODE_PRIVATE).getString(LAST_FILE_PATH_KEY,
                Environment.getExternalStorageDirectory().getAbsolutePath());
        File startFile = new File(startPath);
        if (startFile.isFile()) {
            startPath = startFile.getParent();
        }

        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(FilePickerActivity.EXTRA_START_PATH, startPath);

        startActivityForResult(intent, 0);
    }

    private void initializeTests(String filePath) {
        ListView testList = (ListView) findViewById(R.id.test_list);
        List<Test> tests = new TestParser().parse(new File(filePath), getString(R.string.default_test_name_format));
        testList.setAdapter(new TestListAdapter(this, tests));
    }
}
