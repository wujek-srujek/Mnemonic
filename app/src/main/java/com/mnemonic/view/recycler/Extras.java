package com.mnemonic.view.recycler;


import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;


public class Extras<E> {

    public interface ExtrasListener<E> {

        void extraSet(int position, E extra);

        void extrasExpanded(int position);

        void extrasContracted(int position);

        void extraCleared(int position);

        void extrasCleared(int... positions);
    }

    private final SparseArray<E> extras;

    private final List<ExtrasListener<E>> listeners;

    public Extras(int initialCapacity) {
        extras = new SparseArray<>(initialCapacity);
        listeners = new ArrayList<>();
    }

    public int getCount() {
        return extras.size();
    }

    public int[] getPositions() {
        int[] positions = new int[extras.size()];
        for (int i = 0; i < positions.length; ++i) {
            positions[i] = extras.keyAt(i);
        }

        return positions;
    }

    public void set(int position, E extra) {
        extras.put(position, extra);
        for (ExtrasListener<E> listener : listeners) {
            listener.extraSet(position, extra);
        }
    }

    public void setExclusive(int position, E extra) {
        clearAll();
        set(position, extra);
    }

    public boolean isSet(int position) {
        return extras.indexOfKey(position) >= 0;
    }

    public E get(int position) {
        return extras.get(position);
    }

    public void clear(int position) {
        extras.remove(position);
        for (ExtrasListener<E> listener : listeners) {
            listener.extraCleared(position);
        }
    }

    public void clearAll() {
        if (extras.size() == 0) {
            return;
        }

        int[] positions = getPositions();

        extras.clear();
        for (ExtrasListener<E> listener : listeners) {
            listener.extrasCleared(positions);
        }
    }

    public void expand(int position) {
        // do in reverse to prevent earlier extras from overwriting consecutive ones
        for (int i = extras.size() - 1; i >= 0; --i) {
            int oldPosition = extras.keyAt(i);
            if (oldPosition >= position) {
                E extra = extras.valueAt(i);
                extras.append(oldPosition + 1, extra);
                extras.removeAt(i);
            }
        }

        for (ExtrasListener<E> listener : listeners) {
            listener.extrasExpanded(position);
        }
    }

    public void contract(int position) {
        extras.remove(position);

        int size = extras.size();
        for (int i = 0; i < size; ++i) {
            int oldPosition = extras.keyAt(i);
            if (oldPosition > position) {
                E extra = extras.valueAt(i);
                extras.put(oldPosition - 1, extra);
                // another item was put, hence + 1
                extras.removeAt(i + 1);
            }
        }

        for (ExtrasListener<E> listener : listeners) {
            listener.extrasContracted(position);
        }
    }

    public void addListener(ExtrasListener<E> listener) {
        listeners.add(listener);
    }

    public void removeListener(ExtrasListener<E> listener) {
        listeners.remove(listener);
    }

    @Override
    public String toString() {
        return extras.toString();
    }
}
