package com.mnemonic.db;


import java.io.Serializable;


public class Task implements Serializable {

    final Long _id;

    private final String question;

    private final String answer;

    boolean favorite;

    Task(long _id, String question, String answer, boolean favorite) {
        this._id = _id;
        this.question = question;
        this.answer = answer;
        this.favorite = favorite;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isFavorite() {
        return favorite;
    }

    @Override
    public String toString() {
        return String.format("%s[question=%s, answer=%s, favorite=%b]",
                getClass().getSimpleName(), question.replaceAll("\n", "|"),
                answer != null ? answer.replaceAll("\n", "|") : null,
                favorite);
    }
}
