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

    private static final String SELECT_TEST_GROUPS;

    private static final String SELECT_TEST_GROUP;

    private static final String SELECT_CURRENT_TEST_GROUP;

    static {
        String select = "select tg.*, count(te." + Db.Test._ID + ") as " + TEST_GROUP_TEST_COUNT + ", total(te." +
                Db.Test.ENABLED + ") as " + TEST_GROUP_ENABLED_COUNT + " from " + Db.TestGroup._TABLE_NAME + " as tg left join " +
                Db.Test._TABLE_NAME + " as te on tg." + Db.TestGroup._ID + "=te." + Db.Test._TEST_GROUP_ID;

        SELECT_TEST_GROUPS = select + " group by tg." + Db.TestGroup._ID + " order by tg." + Db.TestGroup.CREATION_TIMESTAMP + " desc";
        SELECT_TEST_GROUP = select + " where tg." + Db.TestGroup._ID + "=?";
        SELECT_CURRENT_TEST_GROUP = select + " where tg." + Db.TestGroup.CURRENT + "=1";
    }

    private static final String MARK_TEST_GROUP_CURRENT = "update " + Db.TestGroup._TABLE_NAME + " set " +
            Db.TestGroup.CURRENT + "=1 where " + Db.TestGroup._ID + "=?";

    private static final String SELECT_ENABLED_TESTS_FOR_TEST_GROUP;

    private static final String SELECT_TEST;

    private static final String TASK_EXISTENCE_CHECK_FOR_TEST_GROUP;

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

        String testData = "count(ta." + Db.Task.ANSWER + ") as " + TEST_ANSWER_COUNT + ", te.*";
        String fromTestLeftJoinTask = "from " + Db.Test._TABLE_NAME + " as te left join " + Db.Task._TABLE_NAME + " as ta on te." +
                Db.Test._ID + "=ta." + Db.Task._TEST_ID;
        String whereInGroupAndEnabled = "where te." + Db.Test._TEST_GROUP_ID + "=? and te." + Db.Test.ENABLED + "=1";

        SELECT_ENABLED_TESTS_FOR_TEST_GROUP = selectFilters + ", " + testData + " " + fromTestLeftJoinTask + " " +
                whereInGroupAndEnabled + " group by te." + Db.Test._ID + " order by te." + Db.Test._ID;
        SELECT_TEST = selectFilters + ", " + testData + " " + fromTestLeftJoinTask + " where te." + Db.Test._ID + "=?";
        TASK_EXISTENCE_CHECK_FOR_TEST_GROUP = selectFilters + " " + fromTestLeftJoinTask + " " + whereInGroupAndEnabled;
    }

    private static final String TEST_ENABLED_STATE_UPDATE = "update " + Db.Test._TABLE_NAME + " set " + Db.Test.ENABLED +
            "=? where " + Db.Test._ID + "=?";

    private static final String ENABLE_ALL_TESTS_FOR_TEST_GROUP = "update " + Db.Test._TABLE_NAME + " set " + Db.Test.ENABLED +
            "=1 where " + Db.Test._TEST_GROUP_ID + "=?";

    private static final String DELETE_TEST_GROUP = "delete from " + Db.TestGroup._TABLE_NAME + " where " + Db.TestGroup._ID + "=?";

    private static final Map<TaskFilter, String> TASK_GETTERS = new HashMap<>(TaskFilter.values().length);

    private static final Map<TaskFilter, String> TASK_UPDATERS = new EnumMap<>(TaskFilter.class);

    static {
        String tasksGetterFmt = "select * from " + Db.Task._TABLE_NAME + " where " +
                Db.Task._TEST_ID + "=? and %s order by " + Db.Task._ID + " asc";

        String taskUpdateFmt = "update " + Db.Task._TABLE_NAME + " set %s=? where " + Db.Task._ID + "=?";

        for (TaskFilter taskFilter : TaskFilter.values()) {
            TASK_GETTERS.put(taskFilter, String.format(tasksGetterFmt, taskFilter.getCondition(null)));
            if (taskFilter.isUpdatable()) {
                // updatable filter
                TASK_UPDATERS.put(taskFilter, String.format(taskUpdateFmt, taskFilter.getTaskColumn(null)));
            }
        }
    }

    // enabled state does not matter for search
    private static final String TASK_SEARCH_FOR_TEST_GROUP = "select ta.* from " + Db.Task._TABLE_NAME + " as ta inner join " +
            Db.TaskFullTextIndex._TABLE_NAME + " as tf on ta." + Db.Task._ID + "=tf." + Db.TaskFullTextIndex._DOC_ID + " inner join " +
            Db.Test._TABLE_NAME + " as te on ta." + Db.Task._TEST_ID + "=te." + Db.Test._ID + " where tf." +
            Db.TaskFullTextIndex._TABLE_NAME + " match ? and te." + Db.Test._TEST_GROUP_ID + "=?";

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
        db.execSQL(Db.TestGroup.Triggers.AFTER_UPDATE_CURRENT);

        db.execSQL(Db.Test._CREATE_TABLE);
        db.execSQL(Db.Test.Indexes.TEST_GROUP_ID_INDEX);

        db.execSQL(Db.Task._CREATE_TABLE);
        db.execSQL(Db.Task.Indexes.TEST_ID_INDEX);

        db.execSQL(Db.TaskFullTextIndex._CREATE_TABLE);

        db.execSQL(Db.Task.Triggers.AFTER_INSERT);
        db.execSQL(Db.Task.Triggers.BEFORE_UPDATE);
        db.execSQL(Db.Task.Triggers.AFTER_UPDATE);
        db.execSQL(Db.Task.Triggers.BEFORE_DELETE);
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

        return new TestGroup(_id, name, creationTimestamp, false, 0, 0);
    }

    public Test addTest(TestGroup testGroup, String name, String description) {
        ContentValues values = new ContentValues(3);
        values.put(Db.Test._TEST_GROUP_ID, testGroup._id);
        values.put(Db.Test.NAME, name);
        values.put(Db.Test.DESCRIPTION, description);

        long _id = getWritableDatabase().insertOrThrow(Db.Test._TABLE_NAME, null, values);

        Test test = new Test(_id, name, description, true, 0, 0, EnumSet.noneOf(TaskFilter.class));

        ++testGroup.testCount;
        ++testGroup.enabledCount;

        return test;
    }

    public Task addTask(Test test, String question, String answer) {
        ContentValues values = new ContentValues(3);
        values.put(Db.Task._TEST_ID, test._id);
        values.put(Db.Task.QUESTION, question);
        values.put(Db.Task.ANSWER, answer);

        long _id = getWritableDatabase().insertOrThrow(Db.Task._TABLE_NAME, null, values);

        Task task = new Task(_id, question, answer, false, null, test);

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

    public void refreshTestGroup(TestGroup testGroup) {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TEST_GROUP, new String[]{"" + testGroup._id});
        cursor.moveToNext();
        testGroup.current = cursor.getInt(cursor.getColumnIndex(Db.TestGroup.CURRENT)) != 0;
        testGroup.testCount = cursor.getInt(cursor.getColumnIndex(TEST_GROUP_TEST_COUNT));
        testGroup.enabledCount = cursor.getInt(cursor.getColumnIndex(TEST_GROUP_ENABLED_COUNT));
        cursor.close();
    }

    public TestGroup getCurrentTestGroup() {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_CURRENT_TEST_GROUP, null);
        if (!cursor.moveToNext()) {
            return null;
        }
        TestGroup testGroup = testGroupFromCursor(cursor);
        cursor.close();

        return testGroup;
    }

    public void markTestGroupCurrent(TestGroup testGroup) {
        // just set the current column, the after update trigger does the rest
        getWritableDatabase().execSQL(MARK_TEST_GROUP_CURRENT, new String[]{"" + testGroup._id});

        testGroup.current = true;
    }

    public List<Test> getEnabledTestsForTestGroup(TestGroup testGroup) {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_ENABLED_TESTS_FOR_TEST_GROUP, new String[]{"" + testGroup._id});
        List<Test> tests = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            tests.add(testFromCursor(cursor));
        }
        cursor.close();

        return tests;
    }

    public Set<TaskFilter> getExistingTaskFiltersForTestGroup(TestGroup testGroup) {
        Cursor cursor = getReadableDatabase().rawQuery(TASK_EXISTENCE_CHECK_FOR_TEST_GROUP, new String[]{"" + testGroup._id});
        cursor.moveToNext();
        Set<TaskFilter> existingTasksFilters = filtersFromCursor(cursor);
        cursor.close();

        return existingTasksFilters;
    }

    public void refreshTest(Test test) {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TEST, new String[]{"" + test._id});
        cursor.moveToNext();
        test.availableTaskFilters = filtersFromCursor(cursor);
        cursor.close();
    }

    public List<Task> getTasksForTest(Test test, TaskFilter taskFilter) {
        String query = TASK_GETTERS.get(taskFilter);
        Cursor cursor = getReadableDatabase().rawQuery(query, new String[]{"" + test._id});
        List<Task> tasks = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            tasks.add(taskFromCursor(cursor, test));
        }
        cursor.close();

        return tasks;
    }

    public List<Task> getTasksForQuery(String query, TestGroup testGroup) throws SearchException {
        Cursor cursor;
        List<Task> tasks;
        try {
            cursor = getReadableDatabase().rawQuery(TASK_SEARCH_FOR_TEST_GROUP, new String[]{query, "" + testGroup._id});
            tasks = new ArrayList<>(cursor.getCount());
        } catch (SQLiteException e) {
            throw new SearchException(e);
        }

        // this map will be filled with tests to minimize number of queries
        Map<Long, Test> tests = new HashMap<>(tasks.size());
        while (cursor.moveToNext()) {
            Long testId = cursor.getLong(cursor.getColumnIndex(Db.Task._TEST_ID));
            Test test = tests.get(testId);
            Task task = taskFromCursor(cursor, test);
            tasks.add(task);

            if (test == null) {
                // test was null and must have been fetched, cache it
                tests.put(testId, task.getTest());
            }
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

    public void enableAllTestsForTestGroup(TestGroup testGroup) {
        getWritableDatabase().execSQL(ENABLE_ALL_TESTS_FOR_TEST_GROUP, new String[]{"" + testGroup._id});
    }

    public void deleteTestGroup(TestGroup testGroup) {
        // just delete the test group, cascading and triggers does the rest
        getWritableDatabase().execSQL(DELETE_TEST_GROUP, new String[]{"" + testGroup._id});
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
        boolean current = cursor.getInt(cursor.getColumnIndex(Db.TestGroup.CURRENT)) != 0;
        int testCount = cursor.getInt(cursor.getColumnIndex(TEST_GROUP_TEST_COUNT));
        int enabledCount = cursor.getInt(cursor.getColumnIndex(TEST_GROUP_ENABLED_COUNT));

        return new TestGroup(_id, name, creationTimestamp, current, testCount, enabledCount);
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

    private Task taskFromCursor(Cursor cursor, Test test) {
        long _id = cursor.getLong(cursor.getColumnIndex(Db.Task._ID));
        String question = cursor.getString(cursor.getColumnIndex(Db.Task.QUESTION));
        String answer = cursor.getString(cursor.getColumnIndex(Db.Task.ANSWER));
        boolean favorite = cursor.getInt(cursor.getColumnIndex(Db.Task.FAVORITE)) != 0;
        String comment = cursor.getString(cursor.getColumnIndex(Db.Task.COMMENT));

        if (test == null) {
            // test not given, need to fetch it
            // normally, such implementation is naive as it causes the infamous N+1 selects problem
            // but data volume is small, the search filters it further, and the tests are cached
            // i.e. I don't care here
            long _testId = cursor.getLong(cursor.getColumnIndex(Db.Task._TEST_ID));
            Cursor testCursor = getReadableDatabase().rawQuery(SELECT_TEST, new String[]{"" + _testId});
            testCursor.moveToNext();
            test = testFromCursor(testCursor);
            testCursor.close();
        }

        return new Task(_id, question, answer, favorite, comment, test);
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
