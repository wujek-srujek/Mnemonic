package com.mnemonic;


import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mnemonic.db.TestGroup;
import com.mnemonic.view.recycler.Extras;
import com.mnemonic.view.recycler.ListAdapter;
import com.mnemonic.view.recycler.ViewHolder;

import java.util.List;


public class TestGroupListAdapter extends ListAdapter<TestGroup, TestGroupListAdapter.TestGroupViewHolder, Boolean> {

    static class TestGroupViewHolder extends ViewHolder<TestGroup, Boolean> {

        private final Context context;

        private final String defaultTestGroupName;

        private final TextView nameTextView;

        private final TextView dateTextView;

        TestGroupViewHolder(View itemView, String defaultTestGroupName, Context context) {
            super(itemView);

            this.context = context;
            this.defaultTestGroupName = defaultTestGroupName;

            nameTextView = (TextView) itemView.findViewById(R.id.test_group_list_item_name_label);
            dateTextView = (TextView) itemView.findViewById(R.id.test_group_list_item_date_label);
        }

        @Override
        protected void onBound(int position) {
            nameTextView.setText(item.getName() != null ? item.getName() : defaultTestGroupName);
            dateTextView.setText(DateUtils.formatDateTime(context, item.getCreationTimestamp(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME));

            itemView.setActivated(getExtra(position) == Boolean.TRUE);
        }

        @Override
        protected void onUnbound() {
            nameTextView.setText(null);
            dateTextView.setText(null);
            itemView.setActivated(false);
        }
    }

    private final String defaultGroupTestName;

    private final Context context;

    public TestGroupListAdapter(List<TestGroup> items, Extras<Boolean> extras, String defaultGroupTestName, Context context) {
        super(items, extras);

        this.defaultGroupTestName = defaultGroupTestName;
        this.context = context;
    }

    @Override
    public TestGroupViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View testGroupItemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.test_group_list_item, viewGroup, false);

        return new TestGroupViewHolder(testGroupItemView, defaultGroupTestName, context);
    }
}
