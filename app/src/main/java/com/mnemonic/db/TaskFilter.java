package com.mnemonic.db;


public enum TaskFilter {

    ALL {
        @Override
        public String getColumn() {
            return Db.Task._COUNT;
        }

        @Override
        public String getFilterCondition(String tableAlias) {
            return "1=1";
        }
    },

    FAVORITE {
        @Override
        public String getColumn() {
            return Db.Task.FAVORITE;
        }

        @Override
        public String getFilterCondition(String tableAlias) {
            return column(tableAlias) + "=1";
        }
    },

    COMMENTED {
        @Override
        public String getColumn() {
            return Db.Task.COMMENT;
        }

        @Override
        public String getFilterCondition(String tableAlias) {
            return column(tableAlias) + " is not null";
        }
    };

    public abstract String getColumn();

    public abstract String getFilterCondition(String tableAlias);

    String column(String tableAlias) {
        if (tableAlias == null || tableAlias.isEmpty()) {
            return getColumn();
        }

        return tableAlias + "." + getColumn();
    }
}
