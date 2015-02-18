package com.mnemonic.db;


import android.provider.BaseColumns;


public final class Db {

    public static final String NAME = "mnemonic";

    public static final class TestGroup implements BaseColumns {

        private TestGroup() {
            // nope
        }

        public static final String _TABLE_NAME = "test_group";

        public static final String NAME = "name";

        public static final String CREATION_TIMESTAMP = "creation_timestamp";

        public static final String CURRENT = "current";

        static final String _CREATE_TABLE = "create table " + _TABLE_NAME + " (" +
                _ID + " integer primary key, " +
                NAME + " text, " +
                CREATION_TIMESTAMP + " integer not null, " +
                CURRENT + " integer not null default 0)";

        static final class Triggers {

            private Triggers() {
                // nope
            }

            static final String AFTER_UPDATE_CURRENT = "create trigger after_update_" + _TABLE_NAME + "_" + CURRENT +
                    " after update of " + CURRENT + " on " + _TABLE_NAME + " begin update " + _TABLE_NAME +
                    " set " + CURRENT + "=0 where " + _ID + "!=old." + _ID + "; end";
        }
    }

    public static final class Test implements BaseColumns {

        private Test() {
            // nope
        }

        public static final String _TABLE_NAME = "test";

        public static final String _TEST_GROUP_ID = "_test_group_id";

        public static final String NAME = "name";

        public static final String DESCRIPTION = "description";

        public static final String ENABLED = "enabled";

        static final String _CREATE_TABLE = "create table " + _TABLE_NAME + " (" +
                _ID + " integer primary key, " +
                _TEST_GROUP_ID + " integer not null references " + TestGroup._TABLE_NAME + " on delete cascade, " +
                NAME + " text, " +
                DESCRIPTION + " text, " +
                ENABLED + " integer not null default 1)";

        static final class Indexes {

            private Indexes() {
                // nope
            }

            static final String TEST_GROUP_ID_INDEX = "create index " + Test._TEST_GROUP_ID + "_index on " + Test._TABLE_NAME +
                    "(" + Test._TEST_GROUP_ID + ")";
        }
    }

    public static final class Task implements BaseColumns {

        private Task() {
            // nope
        }

        public static final String _TABLE_NAME = "task";

        public static final String _TEST_ID = "_test_id";

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

        static final class Indexes {

            private Indexes() {
                // nope
            }

            static final String TEST_ID_INDEX = "create index " + Task._TEST_ID + "_index on " + Task._TABLE_NAME +
                    "(" + Task._TEST_ID + ")";
        }
    }

    public static final class TaskFullTextIndex {

        private TaskFullTextIndex() {
            // nope
        }

        public static final String _TABLE_NAME = "task_full_text_index";

        public static final String _DOC_ID = "docid";

        static final String _CREATE_TABLE = "create virtual table " + _TABLE_NAME + " using fts4(" +
                "content=" + Task._TABLE_NAME + ", " +
                Task.QUESTION + ", "
                + Task.ANSWER + "," +
                "tokenize=porter)";

        static final class Triggers {

            private Triggers() {
                // nope
            }

            private static final String AFTER_FMT = "create trigger " + Task._TABLE_NAME + "_after_%1$s after %1$s %2$s on "
                    + Task._TABLE_NAME + " begin insert into " + _TABLE_NAME + "(" + _DOC_ID + ", " + Task.QUESTION + ", " +
                    Task.ANSWER + ") values(new." + Task._ID + ", new." + Task.QUESTION + ", new." + Task.ANSWER + "); end";

            private static final String BEFORE_FMT = "create trigger " + Task._TABLE_NAME + "_before_%1$s before %1$s %2$s on "
                    + Task._TABLE_NAME + " begin delete from " + _TABLE_NAME + " where " + _DOC_ID + "=old." + Task._ID + "; end";

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
