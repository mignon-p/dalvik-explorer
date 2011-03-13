/*
 * Copyright (C) 2011 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jessies.dalvikexplorer;

import android.content.Context;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.util.*;

/**
 * A ListAdapter that manages a ListView backed by an array of arbitrary
 * objects.  By default this class expects that the provided resource id references
 * a single TextView.  If you want to use a more complex layout, use the constructors that
 * also takes a field id.  That field id should reference a TextView in the larger layout
 * resource.
 * 
 * However the TextView is referenced, it will be filled with the toString() of each object in
 * the array. You can add lists or arrays of custom objects. Override the toString() method
 * of your objects to determine what text will be displayed for the item in the list.
 * 
 * To use something other than TextViews for the array display, for instance, ImageViews,
 * or to have some of data besides toString() results fill the views,
 * override {@link #getView(int, View, ViewGroup)} to return the type of view you want.
 */
public class BetterArrayAdapter<T> extends BaseAdapter implements Filterable {
    /**
     * Contains the list of objects that represent the data of this adapter.
     * The content of this list is referred to as "the array" in the documentation.
     */
    private List<T> mObjects;
    
    /**
     * Lock used to modify the content of {@link #mObjects}. Any write operation
     * performed on the array should be synchronized on this lock. This lock is also
     * used by the filter (see {@link #getFilter()} to make a synchronized copy of
     * the original array of data.
     */
    private final Object mLock = new Object();
    
    /**
     * The resource indicating what views to inflate to display the content of this
     * array adapter.
     */
    private int mResource;
    
    /**
     * The resource indicating what views to inflate to display the content of this
     * array adapter in a drop down widget.
     */
    private int mDropDownResource;
    
    /**
     * If the inflated resource is not a TextView, {@link #mFieldId} is used to find
     * a TextView inside the inflated views hierarchy. This field must contain the
     * identifier that matches the one defined in the resource file.
     */
    private int mFieldId = 0;
    
    /**
     * Indicates whether or not {@link #notifyDataSetChanged()} must be called whenever
     * {@link #mObjects} is modified.
     */
    private boolean mNotifyOnChange = true;
    
    private Context mContext;
    
    private ArrayList<T> mOriginalValues;
    private ArrayFilter mFilter;
    
    private LayoutInflater mInflater;
    
    /**
     * Constructor
     * 
     * @param context The current context.
     * @param objects The objects to represent in the ListView.
     */
    public BetterArrayAdapter(Context context, List<T> objects) {
        init(context, android.R.layout.simple_list_item_1, 0, objects);
    }
    
    @Override public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mNotifyOnChange = true;
    }
    
    /**
     * Control whether methods that change the list ({@link #add},
     * {@link #insert}, {@link #remove}, {@link #clear}) automatically call
     * {@link #notifyDataSetChanged}.  If set to false, caller must
     * manually call notifyDataSetChanged() to have the changes
     * reflected in the attached view.
     * 
     * The default is true, and calling notifyDataSetChanged()
     * resets the flag to true.
     * 
     * @param notifyOnChange if true, modifications to the list will
     *                       automatically call {@link
     *                       #notifyDataSetChanged}
     */
    public void setNotifyOnChange(boolean notifyOnChange) {
        mNotifyOnChange = notifyOnChange;
    }
    
    private void init(Context context, int resource, int textViewResourceId, List<T> objects) {
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = mDropDownResource = resource;
        mObjects = objects;
        mFieldId = textViewResourceId;
    }
    
    /**
     * Returns the context associated with this array adapter. The context is used
     * to create views from the resource passed to the constructor.
     * 
     * @return The Context associated with this adapter.
     */
    public Context getContext() {
        return mContext;
    }
    
    public int getCount() {
        return mObjects.size();
    }
    
    public T getItem(int position) {
        return mObjects.get(position);
    }
    
    /**
     * Returns the position of the specified item in the array.
     * 
     * @param item The item to retrieve the position of.
     * 
     * @return The position of the specified item.
     */
    public int getPosition(T item) {
        return mObjects.indexOf(item);
    }
    
    public long getItemId(int position) {
        return position;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mResource);
    }
    
    private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource) {
        View view;
        TextView text;
        
        if (convertView == null) {
            view = mInflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }
        
        try {
            if (mFieldId == 0) {
                //  If no custom field is assigned, assume the whole resource is a TextView
                text = (TextView) view;
            } else {
                //  Otherwise, find the TextView field within the layout
                text = (TextView) view.findViewById(mFieldId);
            }
        } catch (ClassCastException e) {
            Log.e("BetterArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException( "BetterArrayAdapter requires the resource ID to be a TextView", e);
        }
        
        T item = getItem(position);
        if (item instanceof CharSequence) {
            text.setText((CharSequence)item);
        } else {
            text.setText(item.toString());
        }
        
        return view;
    }
    
    /**
     * <p>Sets the layout resource to create the drop down views.</p>
     * 
     * @param resource the layout resource defining the drop down views
     * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
     */
    public void setDropDownViewResource(int resource) {
        this.mDropDownResource = resource;
    }
    
    @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createViewFromResource(position, convertView, parent, mDropDownResource);
    }
    
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }
    
    /**
     * <p>An array filter constrains the content of the array adapter with
     * a prefix. Each item that does not start with the supplied prefix
     * is removed from the list.</p>
     */
    private class ArrayFilter extends Filter {
        @Override protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            
            if (mOriginalValues == null) {
                synchronized (mLock) {
                    mOriginalValues = new ArrayList<T>(mObjects);
                }
            }
            
            if (prefix == null || prefix.length() == 0) {
                synchronized (mLock) {
                    ArrayList<T> list = new ArrayList<T>(mOriginalValues);
                    results.values = list;
                    results.count = list.size();
                }
            } else {
                String needle = prefix.toString().toLowerCase();
                
                final ArrayList<T> values = mOriginalValues;
                final int count = values.size();
                
                final ArrayList<T> newValues = new ArrayList<T>(count);
                
                for (int i = 0; i < count; i++) {
                    final T value = values.get(i);
                    final String valueText = value.toString().toLowerCase();
                    
                    // TODO: factor this out into a protected performFiltering method on the adapter. or maybe an enum PREFIX, SUBSTRING, REGEX (all with smart casing)?
                    if (valueText.indexOf(needle) != -1) {
                        newValues.add(value);
                    }
                    
                    /*
                     * // First match against the whole, non-splitted value
                     * if (valueText.startsWith(needle)) {
                     *     newValues.add(value);
                     * } else {
                     *     final String[] words = valueText.split(" ");
                     *     final int wordCount = words.length;
                     * 
                     *     for (int k = 0; k < wordCount; k++) {
                     *         if (words[k].startsWith(needle)) {
                     *             newValues.add(value);
                     *             break;
                     *         }
                     *     }
                     * }
                     */
                }
                
                results.values = newValues;
                results.count = newValues.size();
            }
            
            return results;
        }
        
        @Override protected void publishResults(CharSequence constraint, FilterResults results) {
            @SuppressWarnings("unchecked")
            List<T> typedValues = (List<T>) results.values;
            mObjects = typedValues;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
