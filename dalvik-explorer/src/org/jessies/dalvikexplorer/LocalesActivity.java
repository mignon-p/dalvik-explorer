package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import java.util.*;

public class LocalesActivity extends BetterListActivity {
    private static final List<LocaleListItem> LANGUAGES = gatherLanguages();
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new BetterArrayAdapter<LocaleListItem>(this, LANGUAGES, LocaleListItem.class, "toSubtitle"));
        int languageCount = LANGUAGES.size() - 1; // Don't count the extra entry for the default locale.
        setTitle("Languages (" + languageCount + ")");
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final LocaleListItem item = (LocaleListItem) l.getAdapter().getItem(position);
        String languageName = item.locale().toString();
        final Intent intent;
        if (languageName.contains("_")) {
            intent = new Intent(this, LocaleActivity.class);
            final String localeName = languageName.replace(" (default)", "");
            intent.putExtra("org.jessies.dalvikexplorer.Locale", localeName);
        } else {
            intent = new Intent(this, LocaleCountriesActivity.class);
            intent.putExtra("org.jessies.dalvikexplorer.Language", languageName);
        }
        startActivity(intent);
    }
    
    private static List<LocaleListItem> gatherLanguages() {
        final Locale[] availableLocales = Locale.getAvailableLocales();
        final Locale defaultLocale = Locale.getDefault();
        // Put the default locale at the top of the list...
        final ArrayList<LocaleListItem> result = new ArrayList<LocaleListItem>(availableLocales.length);
        result.add(new LocaleListItem(defaultLocale));
        // ...followed by all the distinct languages...
        TreeSet<String> languages = new TreeSet<String>();
        for (Locale locale : availableLocales) {
            languages.add(locale.getLanguage());
        }
        for (String language : languages) {
            result.add(new LocaleListItem(new Locale(language)));
        }
        return result;
    }
}
