package com.mnemonic;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mnemonic.db.Test;

import java.util.List;


public class TestListAdapter extends ArrayAdapter<Test> {

    private final static int DEFAULT_VIEW_TYPE = 0;

    private final static int NODESC_VIEW_TYPE = 1;

    // the same holder type used for both view types
    // one is a subset of the other with the same ids
    private static class TestItemViewHolder {

        final TextView nameTextView;

        final TextView descriptionTextView;

        TestItemViewHolder(View itemView, int viewType) {
            this.nameTextView = (TextView) itemView.findViewById(R.id.test_list_item_name_label);
            this.descriptionTextView = viewType == DEFAULT_VIEW_TYPE ?
                    (TextView) itemView.findViewById(R.id.test_list_item_desc_label) : null;
        }
    }

    private final String defaultTestName;

    private final LayoutInflater inflater;

    public TestListAdapter(Context ctx, List<Test> tests, String defaultTestName) {
        super(ctx, R.layout.test_list_item, tests);

        this.defaultTestName = defaultTestName;
        inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getDescription() != null ? DEFAULT_VIEW_TYPE : NODESC_VIEW_TYPE;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        TestItemViewHolder holder;

        int viewType = getItemViewType(position);
        if (convertView == null) {
            int viewResource = viewType == DEFAULT_VIEW_TYPE ? R.layout.test_list_item : R.layout.test_list_item_nodesc;
            view = inflater.inflate(viewResource, parent, false);
            holder = new TestItemViewHolder(view, viewType);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (TestItemViewHolder) convertView.getTag();
        }

        Test test = getItem(position);
        holder.nameTextView.setText(test.getName() != null ? test.getName() : defaultTestName);
        if (viewType == DEFAULT_VIEW_TYPE) {
            holder.descriptionTextView.setText(test.getDescription());
        }

        return view;
    }
}
