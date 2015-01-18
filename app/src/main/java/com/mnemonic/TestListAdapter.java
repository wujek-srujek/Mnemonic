package com.mnemonic;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mnemonic.db.TaskFilter;
import com.mnemonic.db.Test;

import java.util.List;


public class TestListAdapter extends ArrayAdapter<Test> {

    private static class TestItemViewHolder {

        final TextView nameTextView;

        final TextView descriptionTextView;

        final ImageView favoriteImage;

        TestItemViewHolder(View itemView) {
            this.nameTextView = (TextView) itemView.findViewById(R.id.test_list_item_name_label);
            this.descriptionTextView = (TextView) itemView.findViewById(R.id.test_list_item_desc_label);
            this.favoriteImage = (ImageView) itemView.findViewById(R.id.test_list_item_favorite_image);
        }
    }

    private final String defaultTestName;

    private final LayoutInflater inflater;

    private final View.OnClickListener clickListener;

    public TestListAdapter(Context ctx, List<Test> tests, String defaultTestName, final TestStarter testStarter) {
        super(ctx, R.layout.test_list_item, tests);

        this.defaultTestName = defaultTestName;

        inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        clickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                testStarter.startTest((Test) v.getTag(), TaskFilter.FAVORITE);
            }
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        TestItemViewHolder holder;

        if (convertView == null) {
            view = inflater.inflate(R.layout.test_list_item, parent, false);

            holder = new TestItemViewHolder(view);
            holder.favoriteImage.setOnClickListener(clickListener);

            view.setTag(holder);
        } else {
            view = convertView;
            holder = (TestItemViewHolder) convertView.getTag();
        }

        Test test = getItem(position);
        holder.nameTextView.setText(test.getName() != null ? test.getName() : defaultTestName);
        if (test.getDescription() != null) {
            holder.descriptionTextView.setVisibility(View.VISIBLE);
            holder.descriptionTextView.setText(test.getDescription());
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }
        holder.favoriteImage.setVisibility(test.hasFavorite() ? View.VISIBLE : View.GONE);
        holder.favoriteImage.setTag(test);

        return view;
    }
}
