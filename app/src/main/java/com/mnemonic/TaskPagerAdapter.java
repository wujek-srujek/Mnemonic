package com.mnemonic;


import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mnemonic.db.Task;

import java.util.List;


public class TaskPagerAdapter extends PagerAdapter {

    private LayoutInflater inflater;

    private List<Task> tasks;

    public TaskPagerAdapter(LayoutInflater inflater, List<Task> tasks) {
        this.inflater = inflater;
        this.tasks = tasks;
    }

    @Override
    public int getCount() {
        // question and answer are separate pages
        return tasks.size() * 2;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Task task = tasks.get(taskNumberForPosition(position));

        TextView textView = (TextView) inflater.inflate(R.layout.task_page, container, false);
        textView.setText(isQuestion(position) ? task.getQuestion() : task.getAnswer());
        container.addView(textView);

        return textView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        TextView textView = (TextView) object;
        textView.setText(null);
        container.removeView(textView);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public Task taskForPosition(int position) {
        return tasks.get(taskNumberForPosition(position));
    }

    public int taskNumberForPosition(int position) {
        return position / 2;
    }

    public boolean isQuestion(int position) {
        return position % 2 == 0;
    }
}
