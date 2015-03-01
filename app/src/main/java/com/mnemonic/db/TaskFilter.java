package com.mnemonic.db;


public enum TaskFilter {

    ALL {
        @Override
        String getAggregateColumn() {
            return Db.Views.TaskAggregates.TASK_COUNT;
        }

        @Override
        String getCondition() {
            return "1=1";
        }

        @Override
        String getTaskColumn() {
            return null;
        }
    },

    FAVORITE {
        @Override
        String getAggregateColumn() {
            return Db.Views.TaskAggregates.FAVORITE_COUNT;
        }

        @Override
        String getCondition() {
            return getTaskColumn() + "=1";
        }

        @Override
        String getTaskColumn() {
            return Db.Task.FAVORITE;
        }
    },

    COMMENTED {
        @Override
        String getAggregateColumn() {
            return Db.Views.TaskAggregates.COMMENTED_COUNT;
        }

        @Override
        String getCondition() {
            return getTaskColumn() + " is not null";
        }

        @Override
        String getTaskColumn() {
            return Db.Task.COMMENT;
        }
    };

    abstract String getAggregateColumn();

    abstract String getCondition();

    abstract String getTaskColumn();
}
