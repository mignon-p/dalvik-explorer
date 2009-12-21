package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import java.util.*;

public class LocalesActivity extends ListActivity {
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
    private static final List<LocaleListItem> LOCALES = gatherLocales();
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ArrayAdapter<LocaleListItem>(this, android.R.layout.simple_list_item_1, LOCALES));
        setTitle("Locales (" + LOCALES.size() + ")");
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final Intent intent = new Intent(this, LocaleActivity.class);
        intent.putExtra("org.jessies.dalvikexplorer.Locale", LOCALES.get(position).locale.toString());
        startActivity(intent);
    }
    
    private static List<LocaleListItem> gatherLocales() {
        final Locale[] availableLocales = Locale.getAvailableLocales();
        final Locale defaultLocale = Locale.getDefault();
        // Put the default locale at the top of the list...
        final List<LocaleListItem> result = new ArrayList<LocaleListItem>(availableLocales.length);
        result.add(new LocaleListItem(defaultLocale));
        // ...followed by all the others.
        for (Locale locale : availableLocales) {
            if (!locale.equals(defaultLocale)) {
                result.add(new LocaleListItem(locale));
            }
        }
        return result;
    }
}
