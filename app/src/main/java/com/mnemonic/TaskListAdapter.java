package com.mnemonic;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mnemonic.db.Task;

import java.util.List;


public class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.TaskItemViewHolder> {

    public interface OnTaskClickListener {

        void onTaskClick(int position);
    }

    class TaskItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView taskQuestionTextView;

        final TextView taskAnswerTextView;

        TaskItemViewHolder(View itemView) {
            super(itemView);

            taskQuestionTextView = (TextView) itemView.findViewById(R.id.task_list_item_question_label);
            taskAnswerTextView = (TextView) itemView.findViewById(R.id.task_list_item_answer_label);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onTaskClickListener.onTaskClick(getLayoutPosition());
        }

        public void bind(Task task) {
            taskQuestionTextView.setText(task.getQuestion());
            if (task.getPagesCount() > 1) {
                taskAnswerTextView.setText(task.getAnswer());
                taskAnswerTextView.setVisibility(View.VISIBLE);
            } else {
                taskAnswerTextView.setText(null);
                taskAnswerTextView.setVisibility(View.GONE);
            }
        }
    }

    private final List<Task> tasks;

    private OnTaskClickListener onTaskClickListener;

    public TaskListAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @Override
    public TaskItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View taskItemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.task_list_item, viewGroup, false);

        return new TaskItemViewHolder(taskItemView);
    }

    @Override
    public void onBindViewHolder(TaskItemViewHolder holder, int position) {
        holder.bind(tasks.get(position));
    }

    public void setOnTestClickListener(OnTaskClickListener onTaskClickListener) {
        this.onTaskClickListener = onTaskClickListener;
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }
}
