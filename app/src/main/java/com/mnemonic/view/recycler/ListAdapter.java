package com.mnemonic.view.recycler;


import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.List;


public abstract class ListAdapter<I, VH extends ViewHolder<I, E>, E> extends RecyclerView.Adapter<VH> {

    public interface OnItemClickListener<I, E> {

        void onItemClick(int position, I item, E extra, View view);
    }

    public interface OnItemLongClickListener<I, E> {

        void onItemLongClick(int position, I item, E extra, View view);
    }

    protected final List<I> items;

    protected final Extras<E> extras;

    protected OnItemClickListener<I, E> onItemClickListener;

    protected OnItemLongClickListener<I, E> onItemLongClickListener;

    public ListAdapter(List<I> items) {
        this(items, null);
    }

    public ListAdapter(List<I> items, Extras<E> extras) {
        this.items = items;
        this.extras = extras;

        if (extras != null) {
            extras.addListener(new Extras.ExtrasListener<E>() {

                @Override
                public void extraSet(int position, E extra) {
                    notifyItemChanged(position);
                }

                @Override
                public void extrasExpanded(int position) {
                    // nothing
                }

                @Override
                public void extrasContracted(int position) {
                    // nothing
                }

                @Override
                public void extraCleared(int position) {
                    notifyItemChanged(position);
                }

                @Override
                public void extrasCleared(int... positions) {
                    for (int position : positions) {
                        notifyItemChanged(position);
                    }
                }
            });
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        holder.bind(position, items.get(position), extras, onItemClickListener, onItemLongClickListener);
    }

    @Override
    public void onViewRecycled(VH holder) {
        holder.unbind();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setOnItemClickListener(OnItemClickListener<I, E> onItemClickListener) {
        this.onItemClickListener = onItemClickListener;

        // need to rebind all holders so that they get the new listener
        notifyDataSetChanged();
    }

    public void setOnItemLongClickListener(OnItemLongClickListener<I, E> onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;

        // need to rebind all holders so that they get the new listener
        notifyDataSetChanged();
    }

    public I getItem(int position) {
        return items.get(position);
    }

    public void addItem(int position, I item) {
        items.add(position, item);
        if (extras != null) {
            extras.expand(position);
        }

        notifyItemInserted(position);
    }

    public void deleteItem(int position) {
        items.remove(position);
        if (extras != null) {
            extras.contract(position);
        }

        notifyItemRemoved(position);
    }

    public Extras<E> getExtras() {
        return extras;
    }
}
