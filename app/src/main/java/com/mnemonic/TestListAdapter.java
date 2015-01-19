package com.mnemonic;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;

import java.util.List;


public class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.TestItemViewHolder> {

    static class TestItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final String defaultTestName;

        final OnTestChoiceListener onTestChoiceListener;

        final View testItemView;

        final TextView nameTextView;

        final TextView descriptionTextView;

        final ImageView favoriteImage;

        Test test;

        TestItemViewHolder(View itemView, String defaultTestName, OnTestChoiceListener onTestChoiceListener) {
            super(itemView);

            this.defaultTestName = defaultTestName;
            this.onTestChoiceListener = onTestChoiceListener;

            testItemView = itemView;
            nameTextView = (TextView) itemView.findViewById(R.id.test_list_item_name_label);
            descriptionTextView = (TextView) itemView.findViewById(R.id.test_list_item_desc_label);
            favoriteImage = (ImageView) itemView.findViewById(R.id.test_list_item_favorite_image);

            testItemView.setOnClickListener(this);
            favoriteImage.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            TaskFilter taskFilter;
            switch (v.getId()) {
                case R.id.test_list_item_favorite_image:
                    taskFilter = TaskFilter.FAVORITE;
                    break;

                default:
                    taskFilter = TaskFilter.ALL;
                    break;

            }

            onTestChoiceListener.onTestChoice(new TestChoiceInfo(getPosition(), test, taskFilter));
        }

        public void bind(Test test) {
            this.test = test;

            nameTextView.setText(test.getName() != null ? test.getName() : defaultTestName);
            if (test.getDescription() != null) {
                descriptionTextView.setVisibility(View.VISIBLE);
                descriptionTextView.setText(test.getDescription());
            } else {
                descriptionTextView.setVisibility(View.GONE);
                descriptionTextView.setText(null);
            }
            favoriteImage.setVisibility(test.hasFavorite() ? View.VISIBLE : View.GONE);
        }
    }

    private final List<Test> tests;

    private final String defaultTestName;

    private final OnTestChoiceListener onTestChoiceListener;

    public TestListAdapter(List<Test> tests, String defaultTestName, OnTestChoiceListener onTestChoiceListener) {
        this.tests = tests;
        this.defaultTestName = defaultTestName;
        this.onTestChoiceListener = onTestChoiceListener;
    }

    @Override
    public int getItemCount() {
        return tests.size();
    }

    @Override
    public TestItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View testItemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.test_list_item, viewGroup, false);

        return new TestItemViewHolder(testItemView, defaultTestName, onTestChoiceListener);
    }

    @Override
    public void onBindViewHolder(TestItemViewHolder holder, int position) {
        holder.bind(tests.get(position));
    }
}
