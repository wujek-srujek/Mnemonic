package com.mnemonic.db;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;


public class Task implements Parcelable {

    public static final Parcelable.Creator<Task> CREATOR = new Parcelable.Creator<Task>() {

        @Override
        public Task createFromParcel(Parcel in) {
            long _id = in.readLong();
            String question = in.readString();
            String answer = in.readString();
            boolean favorite = in.readInt() != 0;
            String comment = in.readString();

            return new Task(_id, question, answer, favorite, comment);
        }

        @Override
        public Task[] newArray(int size) {
            return new Task[size];
        }
    };

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

    public boolean isFavorite() {
        return favorite;
    }

    public String getComment() {
        return comment;
    }

    public int getPagesCount() {
        return answer != null ? 2 : 1;
    }

    public List<TaskPage> getPages(int taskNumber) {
        boolean isInfo = answer == null;
        List<TaskPage> pages = new ArrayList<>(isInfo ? 1 : 2);
        pages.add(new TaskPage(taskNumber, this, isInfo ? TaskPage.Type.INFO : TaskPage.Type.QUESTION, question));
        if (answer != null) {
            pages.add(new TaskPage(taskNumber, this, TaskPage.Type.ANSWER, answer));
        }

        return pages;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(_id);
        dest.writeString(question);
        dest.writeString(answer);
        dest.writeInt(favorite ? 1 : 0);
        dest.writeString(comment);
    }

    @Override
    public String toString() {
        return String.format("%s[question=%s, answer=%s, favorite=%b, comment=%s]",
                getClass().getSimpleName(), question.replaceAll("\n", "|"),
                answer != null ? answer.replaceAll("\n", "|") : null,
                favorite, comment != null ? comment.replaceAll("\n", "|") : null);
    }
}
