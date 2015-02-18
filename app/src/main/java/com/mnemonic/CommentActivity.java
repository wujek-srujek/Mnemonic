package com.mnemonic;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toolbar;


public class CommentActivity extends Activity {

    public static final String COMMENT_EXTRA = "comment";

    private EditText commentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_comment);
        setActionBar((Toolbar) findViewById(R.id.toolbar));

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.action_edit_comment);
        }

        commentText = (EditText) findViewById(R.id.comment_text);
        commentText.setText(getIntent().getStringExtra(COMMENT_EXTRA));
        commentText.setSelection(commentText.length());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_comment, menu);

        return true;
    }

    public void cancelDelete(MenuItem menuItem) {
        setComment(null);
    }

    public void apply(MenuItem menuItem) {
        setComment(commentText.getText().toString().trim());
    }

    private void setComment(String comment) {
        Intent intent = new Intent();
        intent.putExtra(COMMENT_EXTRA, comment);
        setResult(Activity.RESULT_OK, intent);

        finish();
    }
}
