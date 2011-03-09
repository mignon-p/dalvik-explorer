package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public abstract class Compatibility {
    public static Compatibility get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return new HoneycombCompatibility();
        } else {
            return new PreHoneycombCompatibility();
        }
    }
    
    public abstract void configureSearchView(ListActivity listActivity, Menu menu);
    public abstract void configureSearchView(TextViewActivity textViewActivity, Menu menu);
    
    public static class PreHoneycombCompatibility extends Compatibility {
        public void configureSearchView(ListActivity listActivity, Menu menu) {
            // Nothing to do, since a SearchView couldn't possibly exist pre-honeycomb.
        }
        public void configureSearchView(TextViewActivity textViewActivity, Menu menu) {
            // Nothing to do, since a SearchView couldn't possibly exist pre-honeycomb.
        }
    }
    
    public static class HoneycombCompatibility extends Compatibility {
        public void configureSearchView(final ListActivity listActivity, Menu menu) {
            SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
            searchView.setSubmitButtonEnabled(false);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                public boolean onQueryTextChange(String newText) {
                    listActivity.getListView().setFilterText(newText);
                    return true;
                }
                public boolean onQueryTextSubmit(String query) {
                    // We've been filtering as we go, so there's nothing to do here.
                    return true;
                }
            });
            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                public boolean onClose() {
                    listActivity.getListView().clearTextFilter();
                    return false;
                }
            });
        }
        
        public void configureSearchView(final TextViewActivity textViewActivity, Menu menu) {
            SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
            searchView.setSubmitButtonEnabled(false);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                public boolean onQueryTextChange(String newText) {
                    textViewActivity.setSearchString(newText);
                    return true;
                }
                public boolean onQueryTextSubmit(String query) {
                    // We've been filtering as we go, so there's nothing to do here.
                    return true;
                }
            });
            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                public boolean onClose() {
                    textViewActivity.clearSearch();
                    return false;
                }
            });
        }
    }
}
