package com.mnemonic.db;


import android.os.Parcel;
import android.os.Parcelable;


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
