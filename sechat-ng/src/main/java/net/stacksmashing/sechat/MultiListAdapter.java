package net.stacksmashing.sechat;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiListAdapter extends BaseAdapter {

    private final List<Object> objects = new ArrayList<>();

    private final Map<Object, Boolean> visibility = new HashMap<>();

    public void addAdapter(ListAdapter adapter) {
        objects.add(adapter);
    }

    public void addView(View view) {
        objects.add(view);
    }

    public void setViewVisibility(View view, boolean visible) {
        visibility.put(view, visible);
        notifyDataSetChanged();
    }

    private boolean isInvisible(Object o) {
        return visibility.containsKey(o) && !visibility.get(o);
    }

    @Override
    public int getCount() {
        int count = 0;
        for (Object object : objects) {
            if (object instanceof ListAdapter) {
                count += ((ListAdapter) object).getCount();
            }
            else if (object instanceof View) {
                if (isInvisible(object)) {
                    continue;
                }
                count += 1;
            }
        }
        return count;
    }

    @Override
    public Object getItem(int position) {
        Wrapper wrapper = getWrapperForPosition(position);
        return wrapper != null ? wrapper.getItem() : null;
    }

    @Override
    public long getItemId(int position) {
        Wrapper wrapper = getWrapperForPosition(position);
        return wrapper != null ? wrapper.getItemId() : -1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getViewTypeCount() {
        int typeCount = 0;
        for (Object object : objects) {
            if (object instanceof ListAdapter) {
                typeCount += ((ListAdapter) object).getViewTypeCount();
            }
            else if (object instanceof View) {
                typeCount += 1;
            }
        }
        return typeCount;
    }

    @Override
    public int getItemViewType(int position) {
        Wrapper wrapper = getWrapperForPosition(position);
        return wrapper != null ? wrapper.getItemViewType() : -1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Wrapper wrapper = getWrapperForPosition(position);
        return wrapper != null ? wrapper.getView(convertView, parent) : null;
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
        for (Object object : objects) {
            if (object instanceof ListAdapter) {
                ((ListAdapter) object).unregisterDataSetObserver(observer);
            }
        }
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
        for (Object object : objects) {
            if (object instanceof ListAdapter) {
                ((ListAdapter) object).registerDataSetObserver(observer);
            }
        }
    }

    private Wrapper getWrapperForPosition(int position) {
        int offset = 0, typeCountOffset = 0;
        for (Object object : objects) {
            if (object instanceof ListAdapter) {
                ListAdapter adapter = (ListAdapter) object;
                final int count = adapter.getCount();
                if (position - offset < count) {
                    return new AdapterWrapper(adapter, position - offset, typeCountOffset);
                }
                offset += count;
                typeCountOffset += adapter.getViewTypeCount();
            }
            else if (object instanceof View) {
                if (!isInvisible(object)) {
                    View view = (View) object;
                    if (position - offset == 0) {
                        return new ViewWrapper(view, typeCountOffset);
                    }
                    offset += 1;
                }
                typeCountOffset += 1;
            }
        }
        return null;
    }

    private static class AdapterWrapper implements Wrapper {
        final ListAdapter adapter;
        final int position;
        final int typeCountOffset;

        AdapterWrapper(ListAdapter adapter, int position, int typeCountOffset) {
            this.adapter = adapter;
            this.position = position;
            this.typeCountOffset = typeCountOffset;
        }

        public Object getItem() {
            return adapter.getItem(position);
        }

        public long getItemId() {
            return adapter.getItemId(position);
        }

        public int getItemViewType() {
            return typeCountOffset + adapter.getItemViewType(position);
        }

        public View getView(View convertView, ViewGroup parent) {
            return adapter.getView(position, convertView, parent);
        }
    }

    private static class ViewWrapper implements Wrapper {
        final View view;
        final int typeCountOffset;

        ViewWrapper(View view, int typeCountOffset) {
            this.view = view;
            this.typeCountOffset = typeCountOffset;
        }

        public Object getItem() {
            return null;
        }

        public long getItemId() {
            return -1;
        }

        public int getItemViewType() {
            return typeCountOffset;
        }

        public View getView(View convertView, ViewGroup parent) {
            return view;
        }
    }

    private interface Wrapper {
        Object getItem();

        long getItemId();

        int getItemViewType();

        View getView(View convertView, ViewGroup parent);
    }
}
