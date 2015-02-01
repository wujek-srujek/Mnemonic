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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;


public class DbHelper extends SQLiteOpenHelper {

    private static class DowngradeRequestException extends RuntimeException {
        // nothing
    }

    private static final int DB_VERSION = 1;

    private static final String FILTER_CHECK_FORMAT =
            "exists(select " + Db.Task._ID + " from " + Db.Task._TABLE_NAME + " where %s limit 1) as %s";

    private static final String TEST_QUALIFIER_FORMAT = " and " + Db.Task._TEST_ID + "=test." + Db.Test._ID;

    private static final String TASK_FILTER_CHECKS;

    private static final String TEST_FILTER_CHECKS;

    private static final String TASK_PROPERTY_UPDATE_FORMAT =
            "update " + Db.Task._TABLE_NAME + " set %s=? where " + Db.Task._ID + "=?";

    private static final Map<TaskFilter, String> TASK_PROPERTY_UPDATES = new EnumMap<>(TaskFilter.class);

    static {
        String separator = ", ";
        StringBuilder taskFilterChecks = new StringBuilder();
        StringBuilder testFilterChecks = new StringBuilder();
        for (TaskFilter taskFilter : TaskFilter.values()) {
            String taskWhere = taskFilter.getFilterCondition();
            taskFilterChecks.append(String.format(FILTER_CHECK_FORMAT, taskWhere, taskFilter.getColumn())).append(separator);

            String testWhere = taskWhere + TEST_QUALIFIER_FORMAT;
            testFilterChecks.append(String.format(FILTER_CHECK_FORMAT, testWhere, taskFilter.getColumn())).append(separator);

            if (taskFilter != TaskFilter.ALL) {
                TASK_PROPERTY_UPDATES.put(taskFilter, String.format(TASK_PROPERTY_UPDATE_FORMAT, taskFilter.getColumn()));
            }
        }
        taskFilterChecks.setLength(taskFilterChecks.length() - separator.length());
        testFilterChecks.setLength(testFilterChecks.length() - separator.length());

        TASK_FILTER_CHECKS = taskFilterChecks.toString();
        TEST_FILTER_CHECKS = testFilterChecks.toString();
    }

    private static final String SELECT_TEST_GROUPS =
            "select * from " + Db.TestGroup._TABLE_NAME + " order by " + Db.TestGroup._ID + " desc";

    private static final String SELECT_TEST_COUNT = "select count(*) from " + Db.Test._TABLE_NAME;

    private static final String SELECT_TESTS_FOR_GROUP =
            "select test.*, (select count(*) from " + Db.Task._TABLE_NAME + " where " +
                    Db.Task._TEST_ID + "=test." + Db.Test._ID + ") as " + Db.Test._TASK_COUNT + ", "
                    + TEST_FILTER_CHECKS + ", (select count(" + Db.Task.ANSWER + ") from " +
                    Db.Task._TABLE_NAME + " where " + Db.Task._TEST_ID + "=test." + Db.Test._ID + ") as " +
                    Db.Test._ANSWER_COUNT + " from " + Db.Test._TABLE_NAME + " as test where test." +
                    Db.Test._TEST_GROUP_ID + "=? order by test." + Db.Test._ID + " asc";

    private static final String SELECT_TASK_FILTERS_FOR_TEST =
            "select " + TEST_FILTER_CHECKS + " from " + Db.Test._TABLE_NAME + " as test where test." + Db.Test._ID + "=?";

    private static final String SELECT_TASKS_FOR_TEST_FMT =
            "select * from " + Db.Task._TABLE_NAME + " where " + Db.Task._TEST_ID +
                    "=? and %s order by " + Db.Task._ID + " asc";

    private static final String TASK_EXISTENCE_CHECK = "select " + TASK_FILTER_CHECKS;

    private static final String TASK_FULL_TEXT_SEARCH = "select task.* from " + Db.Task._TABLE_NAME + " as task inner join " +
            Db.TaskFullTextSearch._TABLE_NAME + " as taskfts on task." + Db.Task._ID + "=" + "taskfts." + Db.TaskFullTextSearch._DOC_ID +
            " where taskfts." + Db.TaskFullTextSearch._TABLE_NAME + " match ?";

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

        return new Test(_id, name, description, 0, 0, EnumSet.noneOf(TaskFilter.class));
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

    public void setFavorite(Task task, boolean favorite) {
        setProperty(task, TaskFilter.FAVORITE, "" + (favorite ? 1 : 0));
        task.favorite = favorite;
    }

    public void setComment(Task task, String comment) {
        setProperty(task, TaskFilter.COMMENTED, comment);
        task.comment = comment;
    }

    private void setProperty(Task task, TaskFilter taskFilter, String value) {
        String updateSql = TASK_PROPERTY_UPDATES.get(taskFilter);
        getWritableDatabase().execSQL(updateSql, new String[]{value, "" + task._id});
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
            int taskCount = cursor.getInt(cursor.getColumnIndex(Db.Test._TASK_COUNT));
            int answerCount = cursor.getInt(cursor.getColumnIndex(Db.Test._ANSWER_COUNT));
            Set<TaskFilter> availableTaskFilters = EnumSet.noneOf(TaskFilter.class);
            for (TaskFilter taskFilter : TaskFilter.values()) {
                if (cursor.getInt(cursor.getColumnIndex(taskFilter.getColumn())) != 0) {
                    availableTaskFilters.add(taskFilter);
                }
            }

            tests.add(new Test(_id, name, description, taskCount, taskCount + answerCount, availableTaskFilters));
        }
        cursor.close();

        return tests;
    }

    public void refreshTest(Test test) {
        Cursor cursor = getReadableDatabase().rawQuery(SELECT_TASK_FILTERS_FOR_TEST, new String[]{"" + test._id});
        cursor.moveToNext();
        Set<TaskFilter> availableTaskFilters = EnumSet.noneOf(TaskFilter.class);
        for (TaskFilter taskFilter : TaskFilter.values()) {
            if (cursor.getInt(cursor.getColumnIndex(taskFilter.getColumn())) != 0) {
                availableTaskFilters.add(taskFilter);
            }
        }
        test.availableTaskFilters = availableTaskFilters;
        cursor.close();
    }

    public List<Task> getTasks(Test test, TaskFilter taskFilter) {
        String query = String.format(SELECT_TASKS_FOR_TEST_FMT, taskFilter.getFilterCondition());
        Cursor cursor = getReadableDatabase().rawQuery(query, new String[]{"" + test._id});
        List<Task> tasks = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            tasks.add(taskFromCursor(cursor));
        }
        cursor.close();

        return tasks;
    }

    public Set<TaskFilter> getExistingTaskFilters() {
        Set<TaskFilter> existingTasksFilters = EnumSet.noneOf(TaskFilter.class);

        Cursor cursor = getReadableDatabase().rawQuery(TASK_EXISTENCE_CHECK, null);
        cursor.moveToNext();

        for (TaskFilter taskFilter : TaskFilter.values()) {
            if (cursor.getInt(cursor.getColumnIndex(taskFilter.getColumn())) != 0) {
                existingTasksFilters.add(taskFilter);
            }
        }

        cursor.close();

        return existingTasksFilters;
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

    private Task taskFromCursor(Cursor cursor) {
        long _id = cursor.getLong(cursor.getColumnIndex(Db.Task._ID));
        String question = cursor.getString(cursor.getColumnIndex(Db.Task.QUESTION));
        String answer = cursor.getString(cursor.getColumnIndex(Db.Task.ANSWER));
        boolean favorite = cursor.getInt(cursor.getColumnIndex(Db.Task.FAVORITE)) != 0;
        String comment = cursor.getString(cursor.getColumnIndex(Db.Task.COMMENT));

        return new Task(_id, question, answer, favorite, comment);
    }
}
