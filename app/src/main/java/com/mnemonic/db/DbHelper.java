package com.mnemonic.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;


public class DbHelper extends SQLiteOpenHelper {

    private static class DowngradeRequestException extends RuntimeException {
        // nothing
    }

    private static final int DB_VERSION = 1;

    private static final String TEST_GROUP_TEST_COUNT = "_test_group_test_count";

    private static final String TEST_GROUP_ENABLED_COUNT = "_test_group_enabled_count";

    private static final String TEST_ANSWER_COUNT = "_test_answer_count";

    private static final String SELECT_TEST_GROUPS = "select tg.*, count(*) as " + TEST_GROUP_TEST_COUNT +
            ", total(te." + Db.Test.ENABLED + ") as " + TEST_GROUP_ENABLED_COUNT + " from " + Db.TestGroup._TABLE_NAME +
            " as tg inner join " + Db.Test._TABLE_NAME + " as te on tg." + Db.TestGroup._ID + "=te." + Db.Test._TEST_GROUP_ID +
            " group by tg." + Db.TestGroup._ID;

    private static final String SELECT_TESTS;

    private static final String TASK_EXISTENCE_CHECK;

    private static final String TASK_EXISTENCE_CHECK_FOR_TEST;

    static {
        String selectFilters = "select ";
        boolean first = true;
        for (TaskFilter taskFilter : TaskFilter.values()) {
            if (!first) {
                selectFilters += ", ";
            }
            first = false;
            selectFilters += taskFilter.getSelect("ta");
        }

        String fromTask = "from " + Db.Task._TABLE_NAME + " as ta";
        String innerJoinTest = "inner join " + Db.Test._TABLE_NAME + " as te on te." + Db.Test._ID + "=ta." + Db.Task._TEST_ID;
        String testEnabled = "te." + Db.Test.ENABLED + "=1";

        SELECT_TESTS = selectFilters + ", count(ta." + Db.Task.ANSWER + ") as " + TEST_ANSWER_COUNT + ", te.* " +
                fromTask + " " + innerJoinTest + " where " + testEnabled + " group by te." + Db.Test._ID;
        TASK_EXISTENCE_CHECK = selectFilters + " " + fromTask + " " + innerJoinTest + " where " + testEnabled;
        TASK_EXISTENCE_CHECK_FOR_TEST = selectFilters + " " + fromTask + " where ta." + Db.Task._TEST_ID + "=?";
    }

    private static final Map<TaskFilter, String> TASK_GETTERS = new HashMap<>(TaskFilter.values().length);

    private static final Map<TaskFilter, String> TASK_UPDATERS = new EnumMap<>(TaskFilter.class);

    static {
        String tasksGetterFmt = "select * from " + Db.Task._TABLE_NAME + " where " +
                Db.Task._TEST_ID + "=? and %s order by " + Db.Task._ID + " asc";

        String taskUpdateFmt = "update " + Db.Task._TABLE_NAME + " set %s=? where " + Db.Task._ID + "=?";

        for (TaskFilter taskFilter : TaskFilter.values()) {
            TASK_GETTERS.put(taskFilter, String.format(tasksGetterFmt, taskFilter.getCondition(null)));
            if (taskFilter.getTaskColumn(null) != null) {
                // updatable filter
                TASK_UPDATERS.put(taskFilter, String.format(taskUpdateFmt, taskFilter.getTaskColumn(null)));
            }
        }
    }

    private static final String TASK_FULL_TEXT_SEARCH = "select ta.* from " + Db.Task._TABLE_NAME + " as ta inner join " +
            Db.TaskFullTextSearch._TABLE_NAME + " as tf on ta." + Db.Task._ID + "=" + "tf." + Db.TaskFullTextSearch._DOC_ID +
            " where tf." + Db.TaskFullTextSearch._TABLE_NAME + " match ?";

    private static final String TEST_ENABLED_STATE_UPDATE = "update " + Db.Test._TABLE_NAME + " set " + Db.Test.ENABLED +
            "=? where " + Db.Test._ID + "=?";

    private static final String ENABLE_ALL_TESTS = "update " + Db.Test._TABLE_NAME + " set " + Db.Test.ENABLED + "=1";

    private static final String DISABLED_TESTS_CHECK = "select " + Db.Test._ID + " from " + Db.Test._TABLE_NAME +
            " where " + Db.Test.ENABLED + "=0 limit 1";

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
        db.execSQL(Db.Test.Indexes.TEST_GROUP_ID_INDEX);

        db.execSQL(Db.Task._CREATE_TABLE);
        db.execSQL(Db.Task.Indexes.TEST_ID_INDEX);

        db.execSQL(Db.TaskFullTextSearch._CREATE_TABLE);
        db.execSQL(Db.TaskFullTextSearch.Triggers.AFTER_INSERT);
        db.execSQL(Db.TaskFullTextSearch.Triggers.BEFORE_UPDATE);
        db.execSQL(Db.TaskFullTextSearch.Triggers.AFTER_UPDATE);
        db.execSQL(Db.TaskFullTextSearch.Triggers.BEFORE_DELETE);
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
        long creationTimestamp = System.currentTimeMillis();

        ContentValues values = new ContentValues(2);
        values.put(Db.TestGroup.NAME, name);
        values.put(Db.TestGroup.CREATION_TIMESTAMP, creationTimestamp);

        long _id = getWritableDatabase().insertOrThrow(Db.TestGroup._TABLE_NAME, null, values);

        return new TestGroup(_id, name, creationTimestamp, 0, false);
    }

    public Test addTest(TestGroup testGroup, String name, String description) {
        ContentValues values = new ContentValues(3);
        values.put(Db.Test._TEST_GROUP_ID, testGroup._id);
        values.put(Db.Test.NAME, name);
        values.put(Db.Test.DESCRIPTION, description);

        long _id = getWritableDatabase().insertOrThrow(Db.Test._TABLE_NAME, null, values);

        Test test = new Test(_id, name, description, true, 0, 0, EnumSet.noneOf(TaskFilter.class));

        ++testGroup.testCount;
        testGroup.hasEnabled |= test.enabled;

        return test;
    }

    public Task addTask(Test test, String question, String answer) {
        ContentValues values = new ContentValues(3);
        values.put(Db.Task._TEST_ID, test._id);
        values.put(Db.Task.QUESTION, question);
        values.put(Db.Task.ANSWER, answer);

        long _id = getWritableDatabase().insertOrThrow(Db.Task._TABLE_NAME, null, values);

        Task task = new Task(_id, question, answer, false, null);

        ++test.taskCount;
        test.pagesCount += task.getPagesCount();
        test.availableTaskFilters.add(TaskFilter.ALL);

        return task;
    }

    public List<TestGroup> getTestGroups() {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TEST_GROUPS, null);
        List<TestGroup> testGroups = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            testGroups.add(testGroupFromCursor(cursor));
        }
        cursor.close();

        return testGroups;
    }

    public List<Test> getTests() {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TESTS, null);
        List<Test> tests = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            tests.add(testFromCursor(cursor));
        }
        cursor.close();

        return tests;
    }

    public Set<TaskFilter> getExistingTaskFilters() {
        Cursor cursor = getReadableDatabase().rawQuery(TASK_EXISTENCE_CHECK, null);
        cursor.moveToNext();
        Set<TaskFilter> existingTasksFilters = filtersFromCursor(cursor);
        cursor.close();

        return existingTasksFilters;
    }

    public void refreshTest(Test test) {
        Cursor cursor = getReadableDatabase().rawQuery(TASK_EXISTENCE_CHECK_FOR_TEST, new String[]{"" + test._id});
        cursor.moveToNext();
        test.availableTaskFilters = filtersFromCursor(cursor);
        cursor.close();
    }

    public List<Task> getTasks(Test test, TaskFilter taskFilter) {
        String query = TASK_GETTERS.get(taskFilter);
        Cursor cursor = getReadableDatabase().rawQuery(query, new String[]{"" + test._id});
        List<Task> tasks = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            tasks.add(taskFromCursor(cursor));
        }
        cursor.close();

        return tasks;
    }

    public List<Task> getTasksForQuery(String query) throws SearchException {
        Cursor cursor;
        List<Task> tasks;
        try {
            cursor = getReadableDatabase().rawQuery(TASK_FULL_TEXT_SEARCH, new String[]{query});
            tasks = new ArrayList<>(cursor.getCount());
        } catch (SQLiteException e) {
            throw new SearchException(e);
        }
        while (cursor.moveToNext()) {
            tasks.add(taskFromCursor(cursor));
        }
        cursor.close();

        return tasks;
    }

    public void setTestEnabled(Test test, boolean enabled) {
        if (test.enabled == enabled) {
            return;
        }

        getWritableDatabase().execSQL(TEST_ENABLED_STATE_UPDATE, new String[]{"" + (enabled ? 1 : 0), "" + test._id});

        test.enabled = enabled;
    }

    public void enableAllTests() {
        getWritableDatabase().execSQL(ENABLE_ALL_TESTS);
    }

    public boolean hasDisabledTests() {
        Cursor cursor = getReadableDatabase().rawQuery(DISABLED_TESTS_CHECK, null);
        boolean hasDisabled = cursor.getCount() > 0;
        cursor.close();

        return hasDisabled;
    }

    public void setTaskFavorite(Task task, boolean favorite) {
        setTaskProperty(task, TaskFilter.FAVORITE, "" + (favorite ? 1 : 0));

        task.favorite = favorite;
    }

    public void setTaskComment(Task task, String comment) {
        setTaskProperty(task, TaskFilter.COMMENTED, comment);

        task.comment = comment;
    }

    private void setTaskProperty(Task task, TaskFilter taskFilter, String value) {
        String updateSql = TASK_UPDATERS.get(taskFilter);
        getWritableDatabase().execSQL(updateSql, new String[]{value, "" + task._id});
    }

    private TestGroup testGroupFromCursor(Cursor cursor) {
        long _id = cursor.getLong(cursor.getColumnIndex(Db.TestGroup._ID));
        String name = cursor.getString(cursor.getColumnIndex(Db.TestGroup.NAME));
        long creationTimestamp = cursor.getLong(cursor.getColumnIndex(Db.TestGroup.CREATION_TIMESTAMP));
        int testCount = cursor.getInt(cursor.getColumnIndex(TEST_GROUP_TEST_COUNT));
        boolean hasEnabled = cursor.getInt(cursor.getColumnIndex(TEST_GROUP_ENABLED_COUNT)) > 0;

        return new TestGroup(_id, name, creationTimestamp, testCount, hasEnabled);
    }

    private Test testFromCursor(Cursor cursor) {
        long _id = cursor.getLong(cursor.getColumnIndex(Db.Test._ID));
        String name = cursor.getString(cursor.getColumnIndex(Db.Test.NAME));
        String description = cursor.getString(cursor.getColumnIndex(Db.Test.DESCRIPTION));
        boolean enabled = cursor.getInt(cursor.getColumnIndex(Db.Test.ENABLED)) != 0;
        int taskCount = cursor.getInt(cursor.getColumnIndex(TaskFilter.ALL.getAlias()));
        int answerCount = cursor.getInt(cursor.getColumnIndex(TEST_ANSWER_COUNT));
        Set<TaskFilter> availableTaskFilters = filtersFromCursor(cursor);

        return new Test(_id, name, description, enabled, taskCount, taskCount + answerCount, availableTaskFilters);
    }

    private Task taskFromCursor(Cursor cursor) {
        long _id = cursor.getLong(cursor.getColumnIndex(Db.Task._ID));
        String question = cursor.getString(cursor.getColumnIndex(Db.Task.QUESTION));
        String answer = cursor.getString(cursor.getColumnIndex(Db.Task.ANSWER));
        boolean favorite = cursor.getInt(cursor.getColumnIndex(Db.Task.FAVORITE)) != 0;
        String comment = cursor.getString(cursor.getColumnIndex(Db.Task.COMMENT));

        return new Task(_id, question, answer, favorite, comment);
    }

    private Set<TaskFilter> filtersFromCursor(Cursor cursor) {
        Set<TaskFilter> taskFilters = EnumSet.noneOf(TaskFilter.class);
        for (TaskFilter taskFilter : TaskFilter.values()) {
            int columnIndex = cursor.getColumnIndex(taskFilter.getAlias());
            if (columnIndex != -1 && cursor.getInt(columnIndex) > 0) {
                taskFilters.add(taskFilter);
            }
        }

        return taskFilters;
    }
}
