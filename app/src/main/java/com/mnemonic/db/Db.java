package com.mnemonic.db;


import android.provider.BaseColumns;


public final class Db {

    public static final String NAME = "mnemonic";

    public final static class TestGroup implements BaseColumns {

        private TestGroup() {
            // nope
        }

        static final String _TABLE_NAME = "test_group";

        public static final String NAME = "name";

        static final String _CREATE_TABLE = "create table " + _TABLE_NAME + " (" +
                _ID + " integer primary key, " +
                NAME + " text)";
    }

    public final static class Test implements BaseColumns {

        private Test() {
            // nope
        }

        static final String _TABLE_NAME = "test";

        static final String _TEST_GROUP_ID = "_test_group_id";

        public static final String NAME = "name";

        public static final String DESCRIPTION = "description";

        public static final String _TASK_COUNT = "_task_count";

        public static final String _ANSWER_COUNT = "_answer_count";

        static final String _CREATE_TABLE = "create table " + _TABLE_NAME + " (" +
                _ID + " integer primary key, " +
                _TEST_GROUP_ID + " integer not null references " + TestGroup._TABLE_NAME + " on delete cascade, " +
                NAME + " text, " +
                DESCRIPTION + " text)";
    }

    public final static class Task implements BaseColumns {

        private Task() {
            // nope
        }

        static final String _TABLE_NAME = "task";

        static final String _TEST_ID = "_test_id";

        public static final String QUESTION = "question";

        public static final String ANSWER = "answer";

        public static final String FAVORITE = "favorite";

        public static final String COMMENT = "comment";

        static final String _CREATE_TABLE = "create table " + _TABLE_NAME + " (" +
                _ID + " integer primary key, " +
                _TEST_ID + " integer not null references " + Test._TABLE_NAME + " on delete cascade, " +
                QUESTION + " text not null, " +
                ANSWER + " text, " +
                FAVORITE + " integer not null default 0, " +
                COMMENT + " text)";
    }

    public final static class TaskFullTextSearch {

        private TaskFullTextSearch() {
            // nope
        }

        static final String _TABLE_NAME = "task_full_text_search";

        static final String _DOC_ID = "docid";

        static final String _CREATE_TABLE = "create virtual table " + _TABLE_NAME + " using fts4(content=" + Task._TABLE_NAME +
                ", " + Task.QUESTION + ", " + Task.ANSWER + ", tokenize=porter)";

        static class Triggers {

            private Triggers() {
                // nope
            }

            private static final String AFTER_FMT = "create trigger " + Task._TABLE_NAME + "_after_%1$s after %1$s %2$s on "
                    + Task._TABLE_NAME + " begin insert into " + TaskFullTextSearch._TABLE_NAME + "(" +
                    TaskFullTextSearch._DOC_ID + ", " + Task.QUESTION + ", " + Task.ANSWER + ") values(new." +
                    Task._ID + ", new." + Task.QUESTION + ", new." + Task.ANSWER + "); end";

            private static final String BEFORE_FMT = "create trigger " + Task._TABLE_NAME + "_before_%1$s before %1$s %2$s on "
                    + Task._TABLE_NAME + " begin delete from " + TaskFullTextSearch._TABLE_NAME + " where "
                    + TaskFullTextSearch._DOC_ID + "=old." + Task._ID + "; end";

            private static final String RELEVANT_UPDATE_COLUMNS = "of " + Task.QUESTION + ", " + Task.ANSWER;

            static final String AFTER_INSERT = String.format(AFTER_FMT, "insert", "");

            static final String BEFORE_UPDATE = String.format(BEFORE_FMT, "update", RELEVANT_UPDATE_COLUMNS);

            static final String AFTER_UPDATE = String.format(AFTER_FMT, "update", RELEVANT_UPDATE_COLUMNS);

            static final String BEFORE_DELETE = String.format(BEFORE_FMT, "delete", "");
        }
    }

    private Db() {
        // nope
    }
}
