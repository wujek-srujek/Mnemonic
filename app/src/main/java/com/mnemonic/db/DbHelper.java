package com.mnemonic.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;


public class DbHelper extends SQLiteOpenHelper {

    private static class DowngradeRequestException extends RuntimeException {
        // nothing
    }

    private static final int DB_VERSION = 1;

    private static final String SET_TASK_DATA_FORMAT = "update " + Db.Task._TABLE_NAME + " set %s=? where " + Db.Task._ID + "=?";

    private static final String SET_TASK_FAVORITE = String.format(SET_TASK_DATA_FORMAT, Db.Task.FAVORITE);

    private static final String SET_TASK_COMMENT = String.format(SET_TASK_DATA_FORMAT, Db.Task.COMMENT);

    private static final String SELECT_TEST_GROUPS = "select * from " + Db.TestGroup._TABLE_NAME +
            " order by " + Db.TestGroup._ID + " desc";

    private static final String SELECT_TEST_COUNT = "select count(*) from " + Db.Test._TABLE_NAME;

    private static final String TEST_PROPERTY_CHECK = "exists(select " + Db.Task._ID + " from " + Db.Task._TABLE_NAME +
            " where " + Db.Task._TEST_ID + "=test." + Db.Test._ID + " and %s limit 1) as %s";

    private static final String TEST_HAS_FAVORITE_CHECK =
            String.format(TEST_PROPERTY_CHECK, TaskFilter.FAVORITE.getFilterCondition(), Db.Task.FAVORITE);

    private static final String TEST_HAS_COMMENTED_CHECK =
            String.format(TEST_PROPERTY_CHECK, TaskFilter.COMMENTED.getFilterCondition(), Db.Task.COMMENT);

    private static final String SELECT_TESTS_FOR_GROUP = "select test.*, (select count(*) from " +
            Db.Task._TABLE_NAME + " where " + Db.Task._TEST_ID + "=test." + Db.Test._ID + ") as " + Db.Task._COUNT + ", " +
            TEST_HAS_FAVORITE_CHECK + ", " + TEST_HAS_COMMENTED_CHECK + ", (select count(" + Db.Task.ANSWER + ") from " +
            Db.Task._TABLE_NAME + " where " + Db.Task._TEST_ID + "=test." + Db.Test._ID + ") as " +
            Db.Test._ANSWER_COUNT + " from " + Db.Test._TABLE_NAME + " as test where test." +
            Db.Test._TEST_GROUP_ID + "=? order by test." + Db.Test._ID + " asc";

    private static final String SELECT_TEST_FOR_ID = "select " + TEST_HAS_FAVORITE_CHECK + ", " + TEST_HAS_COMMENTED_CHECK +
            " from " + Db.Test._TABLE_NAME + " as test where test." + Db.Test._ID + "=?";

    private static final String SELECT_TASKS_FOR_TEST_FORMAT = "select * from " + Db.Task._TABLE_NAME + " where " +
            Db.Task._TEST_ID + "=? and %s order by " + Db.Task._ID + " asc";

    private final Context context;

    public DbHelper(Context context) {
        super(context, Db.NAME, null, DB_VERSION);

        this.context = context;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Db.TestGroup._CREATE_TABLE);
        db.execSQL(Db.Test._CREATE_TABLE);
        db.execSQL(Db.Task._CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new DowngradeRequestException();
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        try {
            return super.getWritableDatabase();
        } catch (DowngradeRequestException e) {
            context.deleteDatabase(getDatabaseName());
        }

        return super.getWritableDatabase();
    }

    public <R> R runTransactional(Callable<R> action) throws Exception {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            R result = action.call();
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    public TestGroup addTestGroup(String name) {
        ContentValues values = new ContentValues(1);
        values.put(Db.Test.NAME, name);

        long _id = getWritableDatabase().insertOrThrow(Db.TestGroup._TABLE_NAME, null, values);

        return new TestGroup(_id, name);
    }

    public Test addTest(TestGroup testGroup, String name, String description) {
        ContentValues values = new ContentValues(3);
        values.put(Db.Test._TEST_GROUP_ID, testGroup._id);
        values.put(Db.Test.NAME, name);
        values.put(Db.Test.DESCRIPTION, description);

        long _id = getWritableDatabase().insertOrThrow(Db.Test._TABLE_NAME, null, values);

        return new Test(_id, name, description, 0, 0, false, false);
    }

    public Task addTask(Test test, String question, String answer) {
        ContentValues values = new ContentValues(3);
        values.put(Db.Task._TEST_ID, test._id);
        values.put(Db.Task.QUESTION, question);
        values.put(Db.Task.ANSWER, answer);

        long _id = getWritableDatabase().insertOrThrow(Db.Task._TABLE_NAME, null, values);

        ++test.taskCount;
        test.pagesCount += answer != null ? 2 : 1;

        return new Task(_id, question, answer, false, null);
    }

    public void setFavorite(Task task, boolean favorite) {
        getWritableDatabase().execSQL(SET_TASK_FAVORITE, new String[]{"" + (favorite ? 1 : 0), "" + task._id});
        task.favorite = favorite;
    }

    public void setComment(Task task, String comment) {
        getWritableDatabase().execSQL(SET_TASK_COMMENT, new String[]{comment, "" + task._id});
        task.comment = comment;
    }

    public List<TestGroup> getTestGroups() {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TEST_GROUPS, null);
        List<TestGroup> testGroups = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            long _id = cursor.getLong(cursor.getColumnIndex(Db.TestGroup._ID));
            String name = cursor.getString(cursor.getColumnIndex(Db.TestGroup.NAME));

            testGroups.add(new TestGroup(_id, name));
        }
        cursor.close();

        return testGroups;
    }

    public int getTestCount() {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TEST_COUNT, null);
        cursor.moveToNext();
        int count = cursor.getInt(0);
        cursor.close();

        return count;
    }

    public List<Test> getTests(TestGroup testGroup) {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TESTS_FOR_GROUP, new String[]{"" + testGroup._id});
        List<Test> tests = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            long _id = cursor.getLong(cursor.getColumnIndex(Db.Test._ID));
            String name = cursor.getString(cursor.getColumnIndex(Db.Test.NAME));
            String description = cursor.getString(cursor.getColumnIndex(Db.Test.DESCRIPTION));
            int taskCount = cursor.getInt(cursor.getColumnIndex(Db.Task._COUNT));
            int answerCount = cursor.getInt(cursor.getColumnIndex(Db.Test._ANSWER_COUNT));
            boolean hasFavorite = cursor.getInt(cursor.getColumnIndex(Db.Task.FAVORITE)) != 0;
            boolean hasCommented = cursor.getInt(cursor.getColumnIndex(Db.Task.COMMENT)) != 0;

            tests.add(new Test(_id, name, description, taskCount, taskCount + answerCount, hasFavorite, hasCommented));
        }
        cursor.close();

        return tests;
    }

    public void refreshTest(Test test) {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TEST_FOR_ID, new String[]{"" + test._id});
        cursor.moveToNext();
        test.hasFavorite = cursor.getInt(cursor.getColumnIndex(Db.Task.FAVORITE)) != 0;
        test.hasCommented = cursor.getInt(cursor.getColumnIndex(Db.Task.COMMENT)) != 0;
        cursor.close();
    }

    public List<Task> getTasks(Test test, TaskFilter taskFilter) {
        String query = String.format(SELECT_TASKS_FOR_TEST_FORMAT, taskFilter.getFilterCondition());
        Cursor cursor = getReadableDatabase().rawQuery(query, new String[]{"" + test._id});
        List<Task> tasks = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            long _id = cursor.getLong(cursor.getColumnIndex(Db.Task._ID));
            String question = cursor.getString(cursor.getColumnIndex(Db.Task.QUESTION));
            String answer = cursor.getString(cursor.getColumnIndex(Db.Task.ANSWER));
            boolean favorite = cursor.getInt(cursor.getColumnIndex(Db.Task.FAVORITE)) != 0;
            String comment = cursor.getString(cursor.getColumnIndex(Db.Task.COMMENT));

            tasks.add(new Task(_id, question, answer, favorite, comment));
        }
        cursor.close();

        return tasks;
    }
}
