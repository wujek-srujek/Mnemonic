package com.mnemonic;


public class Task {

    private final String question;

    private final String answer;

    public Task(String question, String answer) {
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
        return String.format("Task: question=%s, answer=%s", question, answer != null ? answer.replaceAll("\n", "|") : null);
    }
}