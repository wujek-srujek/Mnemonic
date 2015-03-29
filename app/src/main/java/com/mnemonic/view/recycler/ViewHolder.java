package com.mnemonic.view.recycler;


import android.support.v7.widget.RecyclerView;
import android.view.View;


public abstract class ViewHolder<I, E> extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

    protected I item;

    protected Extras<E> extras;

    protected ListAdapter.OnItemClickListener<I, E> onItemClickListener;

    protected ListAdapter.OnItemLongClickListener<I, E> onItemLongClickListener;

    public ViewHolder(View itemView) {
        super(itemView);
    }

    @Override
    public void onClick(View v) {
        if (onItemClickListener != null) {
            int position = getAdapterPosition();
            onItemClickListener.onItemClick(position, item, getExtra(position), v);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (onItemLongClickListener != null) {
            int position = getAdapterPosition();
            onItemLongClickListener.onItemLongClick(position, item, getExtra(position), v);

            return true;
        }

        return false;
    }

    public void bind(int position, I item, Extras<E> extras,
                     ListAdapter.OnItemClickListener<I, E> onItemClickListener,
                     ListAdapter.OnItemLongClickListener<I, E> onItemLongClickListener) {
        this.item = item;
        this.extras = extras;
        this.onItemClickListener = onItemClickListener;
        this.onItemLongClickListener = onItemLongClickListener;

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);

        onBound(position);
    }

    protected abstract void onBound(int position);

    public void unbind() {
        item = null;
        extras = null;
        onItemClickListener = null;
        onItemLongClickListener = null;

        itemView.setOnClickListener(null);
        itemView.setOnLongClickListener(null);

        onUnbound();
    }

    protected abstract void onUnbound();

    protected E getExtra(int position) {
        return extras != null ? extras.get(position) : null;
    }
}