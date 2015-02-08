package com.mnemonic;


import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;

import java.util.List;


public class TestListAdapter extends RecyclerView.Adapter<TestListAdapter.TestItemViewHolder> {

    public interface OnTestClickListener {

        void onTestClick(int position, Test test, TaskFilter taskFilter);
    }

    public interface OnTestLongClickListener {

        void onTestLongClick(int position, Test test, TaskFilter taskFilter);
    }

    class TestItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        final View testItemView;

        final TextView taskCountTextView;

        final TextView nameTextView;

        final TextView descriptionTextView;

        final ImageView favoriteImage;

        final ImageView commentedImage;

        Test test;

        TestItemViewHolder(View itemView) {
            super(itemView);

            testItemView = itemView;
            taskCountTextView = (TextView) itemView.findViewById(R.id.test_list_item_icon_count_label);
            nameTextView = (TextView) itemView.findViewById(R.id.test_list_item_name_label);
            descriptionTextView = (TextView) itemView.findViewById(R.id.test_list_item_desc_label);
            favoriteImage = (ImageView) itemView.findViewById(R.id.test_list_item_favorite_image);
            commentedImage = (ImageView) itemView.findViewById(R.id.test_list_item_commented_image);

            testItemView.setOnClickListener(this);
            testItemView.setOnLongClickListener(this);

            favoriteImage.setOnClickListener(this);
            favoriteImage.setOnLongClickListener(this);

            commentedImage.setOnClickListener(this);
            commentedImage.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (onTestClickListener != null) {
                onTestClickListener.onTestClick(getPosition(), test, taskFilterForView(v));
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (onTestLongClickListener != null) {
                onTestLongClickListener.onTestLongClick(getPosition(), test, taskFilterForView(v));

                return true;
            }

            return false;
        }

        public void bind(Test test) {
            this.test = test;

            taskCountTextView.setText("" + test.getTaskCount());
            nameTextView.setText(test.getName() != null ? test.getName() : defaultTestName);
            if (test.getDescription() != null) {
                descriptionTextView.setVisibility(View.VISIBLE);
                descriptionTextView.setText(test.getDescription());
            } else {
                descriptionTextView.setVisibility(View.GONE);
                descriptionTextView.setText(null);
            }
            favoriteImage.setVisibility(test.hasTaskFilter(TaskFilter.FAVORITE) ? View.VISIBLE : View.GONE);
            commentedImage.setVisibility(test.hasTaskFilter(TaskFilter.COMMENTED) ? View.VISIBLE : View.GONE);

            // reset any selections
            nameTextView.setActivated(false);
            favoriteImage.setActivated(false);
            commentedImage.setActivated(false);

            // mark the selection for the current test
            TaskFilter taskFilter = getSelection(getPosition());
            if (taskFilter != null) {
                viewForTaskFilter(taskFilter).setActivated(true);
            }
        }

        TaskFilter taskFilterForView(View view) {
            switch (view.getId()) {
                case R.id.test_list_item_favorite_image:
                    return TaskFilter.FAVORITE;

                case R.id.test_list_item_commented_image:
                    return TaskFilter.COMMENTED;

                case R.id.test_list_item:
                    return TaskFilter.ALL;
            }

            return null;
        }

        View viewForTaskFilter(TaskFilter taskFilter) {
            switch (taskFilter) {
                case ALL:
                    return nameTextView;

                case FAVORITE:
                    return favoriteImage;

                case COMMENTED:
                    return commentedImage;
            }

            return null;
        }
    }

    private final List<Test> tests;

    private final String defaultTestName;

    private final SparseArray<TaskFilter> selections;

    private OnTestClickListener onTestClickListener;

    private OnTestLongClickListener onTestLongClickListener;

    public TestListAdapter(List<Test> tests, String defaultTestName) {
        this.tests = tests;
        this.defaultTestName = defaultTestName;

        selections = new SparseArray<>(tests.size());
    }

    @Override
    public TestItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View testItemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.test_list_item, viewGroup, false);

        return new TestItemViewHolder(testItemView);
    }

    @Override
    public void onBindViewHolder(TestItemViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public void setOnTestClickListener(OnTestClickListener onTestClickListener) {
        this.onTestClickListener = onTestClickListener;
    }

    public void setOnTestLongClickListener(OnTestLongClickListener onTestLongClickListener) {
        this.onTestLongClickListener = onTestLongClickListener;
    }

    @Override
    public int getItemCount() {
        return tests.size();
    }

    public Test getItem(int position) {
        return tests.get(position);
    }

    public void removeItem(int position) {
        tests.remove(position);
        selections.remove(position);

        // move up all selections whose index > position as position doesn't exist any longer
        for (int i = 0; i < selections.size(); ++i) {
            int index = selections.keyAt(i);
            if (index > position) {
                TaskFilter taskFilter = selections.get(index);
                selections.put(index - 1, taskFilter);
                selections.remove(index);
            }
        }

        notifyItemRemoved(position);
    }

    public int getSelectionCount() {
        return selections.size();
    }

    public int[] getSelectionPositions() {
        int[] positions = new int[selections.size()];
        for (int i = 0; i < positions.length; ++i) {
            positions[i] = selections.keyAt(i);
        }

        return positions;
    }

    public TaskFilter getSelection(int position) {
        return selections.get(position);
    }

    public void setSelection(int position, TaskFilter taskFilter) {
        selections.put(position, taskFilter);

        notifyItemChanged(position);
    }

    public void clearSelection(int position) {
        selections.remove(position);

        notifyItemChanged(position);
    }

    public void clearSelections() {
        if (selections.size() == 0) {
            return;
        }

        int firstIndex = selections.keyAt(0);
        int lastIndex = selections.keyAt(selections.size() - 1);

        selections.clear();

        notifyItemRangeChanged(firstIndex, lastIndex - firstIndex + 1);
    }
}
