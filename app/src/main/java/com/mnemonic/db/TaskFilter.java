package com.mnemonic.db;


public enum TaskFilter {

    ALL {
        @Override
        public String getColumn() {
            return Db.Task._COUNT;
        }

        @Override
        public String getFilterCondition() {
            return "1=1";
        }
    },

    FAVORITE {
        @Override
        public String getColumn() {
            return Db.Task.FAVORITE;
        }

        @Override
        public String getFilterCondition() {
            return getColumn() + "=1";
        }
    },

    COMMENTED {
        @Override
        public String getColumn() {
            return Db.Task.COMMENT;
        }

        @Override
        public String getFilterCondition() {
            return getColumn() + " is not null";
        }
    };

    public abstract String getColumn();

    public abstract String getFilterCondition();
}
