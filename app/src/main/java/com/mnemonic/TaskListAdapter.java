package com.mnemonic;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mnemonic.db.Task;
import com.mnemonic.view.recycler.ListAdapter;
import com.mnemonic.view.recycler.ViewHolder;

import java.util.List;


public class TaskListAdapter extends ListAdapter<Task, TaskListAdapter.TaskViewHolder, Void> {

    static class TaskViewHolder extends ViewHolder<Task, Void> {

        private final TextView taskQuestionTextView;

        private final TextView taskAnswerTextView;

        TaskViewHolder(View itemView) {
            super(itemView);

            taskQuestionTextView = (TextView) itemView.findViewById(R.id.task_list_item_question_label);
            taskAnswerTextView = (TextView) itemView.findViewById(R.id.task_list_item_answer_label);
        }

        @Override
        protected void onBound(int position) {
            taskQuestionTextView.setText(item.getQuestion());
            if (item.getPagesCount() > 1) {
                taskAnswerTextView.setText(item.getAnswer());
                taskAnswerTextView.setVisibility(View.VISIBLE);
            } else {
                taskAnswerTextView.setText(null);
                taskAnswerTextView.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onUnbound() {
            taskQuestionTextView.setText(null);
            taskAnswerTextView.setText(null);
            taskAnswerTextView.setVisibility(View.VISIBLE);
        }
    }

    public TaskListAdapter(List<Task> items) {
        super(items);

        setHasStableIds(true);
    }

    @Override
    public TaskViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View taskItemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.task_list_item, viewGroup, false);

        return new TaskViewHolder(taskItemView);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }
}
