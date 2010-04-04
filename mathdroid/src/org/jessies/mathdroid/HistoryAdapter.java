package org.jessies.mathdroid;

import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/**
 * A simple ListAdapter for the ListView that shows the history transcript.
 */
public class HistoryAdapter extends BaseAdapter {
    private final Context mContext;
    private final ArrayList<HistoryItem> mItems;
    
    public HistoryAdapter(Context context) {
        mContext = context;
        mItems = new ArrayList<HistoryItem>();
    }
    
    public void add(HistoryItem item) {
        mItems.add(item);
        notifyDataSetChanged();
    }
    
    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }
    
    public int getCount() {
        return mItems.size();
    }
    
    public Object getItem(int index) {
        return mItems.get(index);
    }
    
    public long getItemId(int index) {
        return index;
    }
    
    public View getView(int index, View convertView, ViewGroup parent) {
        if (convertView == null) {
            return new HistoryItemView(mContext, mItems.get(index));
        }
        
        HistoryItemView historyView = (HistoryItemView) convertView;
        historyView.setItem(mItems.get(index));
        return historyView;
    }
    
    public void fromString(String serializedHistory) {
        if (serializedHistory.length() == 0) {
            return;
        }
        String[] historyLines = serializedHistory.split("\n");
        for (int i = 0; i < historyLines.length; i += 2) {
            add(new HistoryItem(historyLines[i], historyLines[i + 1].substring(" = ".length())));
        }
    }
    
    // Used both as the serialized form when we save the current state and as what "copy all" copies to the clipboard.
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (HistoryItem item : mItems) {
            sb.append(item.question);
            sb.append("\n = ");
            sb.append(item.answer);
            sb.append("\n");
        }
        return sb.toString();
    }
}
