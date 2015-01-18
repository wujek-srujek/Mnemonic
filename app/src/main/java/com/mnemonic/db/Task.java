package com.mnemonic.db;


import java.io.Serializable;


public class Task implements Serializable {

    final Long _id;

    private final String question;

    private final String answer;

    Task(long _id, String question, String answer) {
        this._id = _id;
        this.question = question;
        this.answer = answer;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    @Override
    public String toString() {
        return String.format("%s[question=%s, answer=%s]",
                getClass().getSimpleName(), question.replaceAll("\n", "|"),
                answer != null ? answer.replaceAll("\n", "|") : null);
    }
}
