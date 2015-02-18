package com.mnemonic;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mnemonic.db.TestGroup;

import java.util.List;


public class TestGroupListAdapter extends RecyclerView.Adapter<TestGroupListAdapter.TestGroupItemViewHolder> {

    public interface OnTestGroupClickListener {

        void onTestGroupClick(int position, TestGroup testGroup);
    }

    class TestGroupItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView nameTextView;

        final TextView dateTextView;

        TestGroup testGroup;

        TestGroupItemViewHolder(View itemView) {
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.test_group_list_item_name_label);
            dateTextView = (TextView) itemView.findViewById(R.id.test_group_list_item_date_label);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (onTestGroupClickListener != null) {
                onTestGroupClickListener.onTestGroupClick(getPosition(), testGroup);
            }
        }

        public void bind(TestGroup testGroup) {
            this.testGroup = testGroup;

            nameTextView.setText(testGroup.getName() != null ? testGroup.getName() : defaultTestGroupName);
            dateTextView.setText(DateUtils.formatDateTime(context, testGroup.getCreationTimestamp(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME));

            if (testGroup.isCurrent()) {
                itemView.setActivated(true);
            } else {
                itemView.setActivated(false);
            }
        }
    }

    private final Context context;

    private final List<TestGroup> testGroups;

    private final String defaultTestGroupName;

    private int selectedPosition;

    private OnTestGroupClickListener onTestGroupClickListener;

    public TestGroupListAdapter(Context context, List<TestGroup> testGroups) {
        this.context = context;
        this.testGroups = testGroups;
        this.defaultTestGroupName = context.getString(R.string.default_test_group_name);

        selectedPosition = RecyclerView.NO_POSITION;
        int i = 0;
        for (TestGroup testGroup : testGroups) {
            if (testGroup.isCurrent()) {
                selectedPosition = i;
                break;
            }
            ++i;
        }
    }

    @Override
    public TestGroupItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View testGroupItemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.test_group_list_item, viewGroup, false);

        return new TestGroupItemViewHolder(testGroupItemView);
    }

    @Override
    public void onBindViewHolder(TestGroupItemViewHolder holder, int position) {
        holder.bind(testGroups.get(position));
    }

    public void setOnTestGroupClickListener(OnTestGroupClickListener onTestGroupClickListener) {
        this.onTestGroupClickListener = onTestGroupClickListener;
    }

    @Override
    public int getItemCount() {
        return testGroups.size();
    }

    public TestGroup getItem(int position) {
        return testGroups.get(position);
    }

    public void addItem(int position, TestGroup testGroup) {
        testGroups.add(position, testGroup);

        if (position <= selectedPosition) {
            ++selectedPosition;
        }

        notifyItemInserted(position);
    }

    public void deleteItem(int position) {
        testGroups.remove(position);

        if (position == selectedPosition) {
            selectedPosition = RecyclerView.NO_POSITION;
        } else if (position < selectedPosition) {
            --selectedPosition;
        }

        notifyItemRemoved(position);
    }

    public int getSelection() {
        return selectedPosition;
    }

    public void setSelection(int position) {
        if (selectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(selectedPosition);
        }

        selectedPosition = position;
        notifyItemChanged(selectedPosition);
    }
}
