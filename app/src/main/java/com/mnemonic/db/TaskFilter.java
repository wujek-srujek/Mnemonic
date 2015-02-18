package com.mnemonic.db;


public enum TaskFilter {

    ALL {
        @Override
        String getRawTaskColumn() {
            return null;
        }

        @Override
        String getSelectExpression(String tablePrefix) {
            return "count(*)";
        }

        @Override
        String getCondition(String tablePrefix) {
            return "1=1";
        }

        @Override
        String getAlias() {
            return "_all_filter";
        }
    },

    FAVORITE {
        @Override
        String getRawTaskColumn() {
            return Db.Task.FAVORITE;
        }

        @Override
        String getSelectExpression(String tablePrefix) {
            return "total(" + getTaskColumn(tablePrefix) + ")";
        }

        @Override
        String getCondition(String tablePrefix) {
            return getTaskColumn(tablePrefix) + "=1";
        }

        @Override
        String getAlias() {
            return "_favorite_filter";
        }
    },

    COMMENTED {
        @Override
        String getRawTaskColumn() {
            return Db.Task.COMMENT;
        }

        @Override
        String getSelectExpression(String tablePrefix) {
            return "count(" + getTaskColumn(tablePrefix) + ")";
        }

        @Override
        String getCondition(String tablePrefix) {
            return getTaskColumn(tablePrefix) + " is not null";
        }

        @Override
        String getAlias() {
            return "_commented_filter";
        }
    };

    String getTaskColumn(String tablePrefix) {
        String rawColumn = getRawTaskColumn();

        if (rawColumn == null) {
            return null;
        }

        if (tablePrefix != null) {
            return tablePrefix + "." + rawColumn;
        }

        return rawColumn;
    }

    String getSelect(String tablePrefix) {
        return getSelectExpression(tablePrefix) + " as " + getAlias();
    }

    abstract String getRawTaskColumn();

    abstract String getSelectExpression(String tablePrefix);

    abstract String getCondition(String tablePrefix);

    abstract String getAlias();
}
