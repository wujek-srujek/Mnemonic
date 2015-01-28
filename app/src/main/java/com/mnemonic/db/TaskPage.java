package com.mnemonic.db;


public class TaskPage {

    public enum Type {
        QUESTION, ANSWER, INFO
    }

    private final int taskNumber;

    private final Task task;

    private final Type type;

    private final String text;

    public TaskPage(int taskNumber, Task task, Type type, String text) {
        this.taskNumber = taskNumber;
        this.task = task;
        this.type = type;
        this.text = text;
    }

    public int getTaskNumber() {
        return taskNumber;
    }

    public Task getTask() {
        return task;
    }

    public Type getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return String.format("%s[taskNumber=%d, type=%s, text=%s]", getClass().getSimpleName(),
                taskNumber, type, text.replaceAll("\n", "|"));
    }
}
