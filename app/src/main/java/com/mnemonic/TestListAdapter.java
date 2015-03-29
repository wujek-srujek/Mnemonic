package com.mnemonic;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;
import com.mnemonic.view.recycler.Extras;
import com.mnemonic.view.recycler.ListAdapter;
import com.mnemonic.view.recycler.ViewHolder;

import java.util.List;


public class TestListAdapter extends ListAdapter<Test, TestListAdapter.TestViewHolder, TaskFilter> {

    static class TestViewHolder extends ViewHolder<Test, TaskFilter> {

        private final String defaultTestName;

        private final TextView taskCountTextView;

        private final TextView nameTextView;

        private final TextView descriptionTextView;

        private final ImageView favoriteImage;

        private final ImageView commentedImage;

        TestViewHolder(View itemView, String defaultTestName) {
            super(itemView);

            this.defaultTestName = defaultTestName;

            taskCountTextView = (TextView) itemView.findViewById(R.id.test_list_item_icon_count_label);
            nameTextView = (TextView) itemView.findViewById(R.id.test_list_item_name_label);
            descriptionTextView = (TextView) itemView.findViewById(R.id.test_list_item_desc_label);
            favoriteImage = (ImageView) itemView.findViewById(R.id.test_list_item_favorite_image);
            commentedImage = (ImageView) itemView.findViewById(R.id.test_list_item_commented_image);
        }

        @Override
        protected void onBound(int position) {
            favoriteImage.setOnClickListener(this);
            favoriteImage.setOnLongClickListener(this);

            commentedImage.setOnClickListener(this);
            commentedImage.setOnLongClickListener(this);

            taskCountTextView.setText(String.valueOf(item.getTaskCount()));
            nameTextView.setText(item.getName() != null ? item.getName() : defaultTestName);
            if (item.getDescription() != null) {
                descriptionTextView.setVisibility(View.VISIBLE);
                descriptionTextView.setText(item.getDescription());
            } else {
                descriptionTextView.setVisibility(View.GONE);
                descriptionTextView.setText(null);
            }
            favoriteImage.setVisibility(item.hasTaskFilter(TaskFilter.FAVORITE) ? View.VISIBLE : View.GONE);
            commentedImage.setVisibility(item.hasTaskFilter(TaskFilter.COMMENTED) ? View.VISIBLE : View.GONE);

            nameTextView.setActivated(false);
            favoriteImage.setActivated(false);
            commentedImage.setActivated(false);

            TaskFilter taskFilter = getExtra(position);
            if (taskFilter != null) {
                viewForTaskFilter(taskFilter).setActivated(true);
            }
        }

        @Override
        protected void onUnbound() {
            favoriteImage.setOnClickListener(null);
            favoriteImage.setOnLongClickListener(null);

            commentedImage.setOnClickListener(null);
            commentedImage.setOnLongClickListener(null);

            taskCountTextView.setText(null);
            nameTextView.setText(null);
            descriptionTextView.setText(null);
            descriptionTextView.setVisibility(View.VISIBLE);

            favoriteImage.setVisibility(View.VISIBLE);
            commentedImage.setVisibility(View.VISIBLE);

            nameTextView.setActivated(false);
            favoriteImage.setActivated(false);
            commentedImage.setActivated(false);
        }

        public static TaskFilter taskFilterForView(View view) {
            switch (view.getId()) {
                case R.id.test_list_item_favorite_image:
                    return TaskFilter.FAVORITE;

                case R.id.test_list_item_commented_image:
                    return TaskFilter.COMMENTED;

                case R.id.test_list_item:
                    return TaskFilter.ALL;
            }

            throw new IllegalArgumentException("unknown view");
        }

        private View viewForTaskFilter(TaskFilter taskFilter) {
            switch (taskFilter) {
                case ALL:
                    return nameTextView;

                case FAVORITE:
                    return favoriteImage;

                case COMMENTED:
                    return commentedImage;
            }

            throw new IllegalArgumentException("unknown filter");
        }
    }

    private final String defaultTestName;

    public TestListAdapter(List<Test> items, Extras<TaskFilter> extras, String defaultTestName) {
        super(items, extras);

        this.defaultTestName = defaultTestName;
    }

    @Override
    public TestViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View testItemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.test_list_item, viewGroup, false);

        return new TestViewHolder(testItemView, defaultTestName);
    }
}
