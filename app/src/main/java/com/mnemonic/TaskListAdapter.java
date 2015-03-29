package com.mnemonic;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mnemonic.db.Task;
import com.mnemonic.view.recycler.ListAdapter;
import com.mnemonic.view.recycler.ViewHolder;

import java.util.List;


public class TaskListAdapter extends ListAdapter<Task, TaskListAdapter.TaskiewHolder, Void> {

    static class TaskiewHolder extends ViewHolder<Task, Void> {

        private final TextView taskQuestionTextView;

        private final TextView taskAnswerTextView;

        TaskiewHolder(View itemView) {
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
    }

    @Override
    public TaskiewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View taskItemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.task_list_item, viewGroup, false);

        return new TaskiewHolder(taskItemView);
    }
}
