package com.mnemonic.db;


public final class Db {

    public static final String NAME = "mnemonic";

    public final static class TestGroup {

        private TestGroup() {
            // nope
        }

        static final String _TABLE_NAME = "test_group";

        static final String _ID = "_id";

        public static final String NAME = "name";

        static final String _CREATE_TABLE = "create table " + _TABLE_NAME + " (" +
                _ID + " integer primary key, " +
                NAME + " text)";
    }

    public final static class Test {

        private Test() {
            // nope
        }

        static final String _TABLE_NAME = "test";

        static final String _ID = "_id";

        static final String _TEST_GROUP_ID = "_test_group_id";

        public static final String NAME = "name";

        public static final String DESCRIPTION = "description";

        static final String _CREATE_TABLE = "create table " + _TABLE_NAME + " (" +
                _ID + " integer primary key, " +
                _TEST_GROUP_ID + " integer not null references " + TestGroup._TABLE_NAME + " on delete cascade, " +
                NAME + " text, " +
                DESCRIPTION + " text)";
    }

    public final static class Task {

        private Task() {
            // nope
        }

        static final String _TABLE_NAME = "task";

        static final String _ID = "_id";

        static final String _TEST_ID = "_test_id";

        public static final String QUESTION = "question";

        public static final String ANSWER = "answer";

        public static final String FAVORITE = "favorite";

        static final String _CREATE_TABLE = "create table " + _TABLE_NAME + " (" +
                _ID + " integer primary key, " +
                _TEST_ID + " integer not null references " + Test._TABLE_NAME + " on delete cascade, " +
                QUESTION + " text not null, " +
                ANSWER + " text, " +
                FAVORITE + " integer not null default 0)";
    }

    private Db() {
        // nope
    }
}
