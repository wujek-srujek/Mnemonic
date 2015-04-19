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

        TextView textView = (TextView) LayoutInflater.from(container.getContext()).inflate(R.layout.task_page, container, false);
        textView.setText(currentPage.getText());
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

    public TaskPage getTaskPage(int position) {
        return pages.get(position);
    }

    public Task getTask(int position) {
        return pages.get(position).getTask();
    }
}
