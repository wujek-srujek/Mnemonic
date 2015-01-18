package com.mnemonic.db;


import java.io.Serializable;


public class Task implements Serializable {

    final Long _id;

    private final String question;

    private final String answer;

    boolean favorite;

    String comment;

    Task(long _id, String question, String answer, boolean favorite, String comment) {
        this._id = _id;
        this.question = question;
        this.answer = answer;
        this.favorite = favorite;
        this.comment = comment;
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

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return String.format("%s[question=%s, answer=%s, favorite=%b, comment=%s]",
                getClass().getSimpleName(), question.replaceAll("\n", "|"),
                answer != null ? answer.replaceAll("\n", "|") : null,
                favorite, comment != null ? comment.replaceAll("\n", "|") : null);
    }
}
