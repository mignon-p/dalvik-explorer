package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import java.util.*;

public class LocaleCountriesActivity extends ListActivity {
    private static class LocaleListItem {
        private final Locale locale;
        private LocaleListItem(Locale locale) {
            this.locale = locale;
        }
        @Override public String toString() {
            String result = locale.toString();
            if (locale.equals(Locale.getDefault())) {
                result += " (default)";
            }
            return result;
        }
    }
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        List<LocaleListItem> locales = gatherLocales();
        setListAdapter(new ArrayAdapter<LocaleListItem>(this, android.R.layout.simple_list_item_1, locales));
        setTitle("Locales (" + locales.size() + ")");
        getListView().setTextFilterEnabled(true);
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final Intent intent = new Intent(this, LocaleActivity.class);
        final LocaleListItem item = (LocaleListItem) l.getAdapter().getItem(position);
        intent.putExtra("org.jessies.dalvikexplorer.Locale", item.locale.toString());
        startActivity(intent);
    }
    
    private List<LocaleListItem> gatherLocales() {
        String language = getIntent().getStringExtra("org.jessies.dalvikexplorer.Language");
        // List all the locales for this language.
        final ArrayList<LocaleListItem> result = new ArrayList<LocaleListItem>();
        for (Locale locale : Locale.getAvailableLocales()) {
            if (locale.getLanguage().equals(language)) {
                result.add(new LocaleListItem(locale));
            }
        }
        return result;
    }
}
