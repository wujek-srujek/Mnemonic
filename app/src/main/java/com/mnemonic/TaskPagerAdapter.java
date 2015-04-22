package com.mnemonic;


import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mnemonic.db.Task;
import com.mnemonic.db.TaskPage;

import java.util.List;


public class TaskPagerAdapter extends PagerAdapter {

    static class TaskPageViewHolder {

        final View mainView;

        final TextView textView;

        TaskPageViewHolder(View mainView, TextView textView) {
            this.mainView = mainView;
            this.textView = textView;
        }
    }

    private final List<TaskPage> pages;

    public TaskPagerAdapter(List<TaskPage> pages) {
        this.pages = pages;
    }

    @Override
    public int getCount() {
        return pages.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        TaskPage currentPage = pages.get(position);

        View view = LayoutInflater.from(container.getContext()).inflate(R.layout.task_page, container, false);
        TextView textView = (TextView) view.findViewById(R.id.task_text);
        textView.setText(currentPage.getText());
        container.addView(view);

        return new TaskPageViewHolder(view, textView);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        TaskPageViewHolder viewHolder = (TaskPageViewHolder) object;
        viewHolder.textView.setText(null);
        container.removeView(viewHolder.mainView);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return ((TaskPageViewHolder) object).mainView == view;
    }

    public TaskPage getTaskPage(int position) {
        return pages.get(position);
    }

    public Task getTask(int position) {
        return pages.get(position).getTask();
    }
}
