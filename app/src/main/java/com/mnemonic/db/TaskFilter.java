package com.mnemonic.db;


public enum TaskFilter {

    ALL {
        @Override
        public String getFilterCondition() {
            return "1=1";
        }
    },

    FAVORITE {
        @Override
        public String getFilterCondition() {
            return Db.Task.FAVORITE + "=1";
        }
    },

    COMMENTED {
        @Override
        public String getFilterCondition() {
            return Db.Task.COMMENT + " is not null";
        }
    };

    public abstract String getFilterCondition();
}
