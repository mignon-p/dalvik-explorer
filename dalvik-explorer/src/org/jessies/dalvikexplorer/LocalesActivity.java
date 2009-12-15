package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.util.*;

public class LocalesActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(getLocalesAsString());
    }
    
    private String getLocalesAsString() {
        final StringBuilder result = new StringBuilder();
        final Locale defaultLocale = Locale.getDefault();
        for (Locale locale : Locale.getAvailableLocales()) {
            result.append(locale.toString());
            if (locale.equals(defaultLocale)) {
                result.append(" (default)");
            }
            result.append('\n');
            /*
             * final String displayCountry = locale.getDisplayCountry();
             * if (displayCountry.length() > 0) {
             * result.append(" country: " + displayCountry + " (" + locale.getISO3Country() + ")\n");
             * }
             * final String displayLanguage = locale.getDisplayLanguage();
             * if (displayLanguage.length() > 0) {
             * result.append(" language: " + displayLanguage + " (" + locale.getISO3Language() + ")\n");
             * }
             */
        }
        result.append("    " + Locale.getAvailableLocales().length);
        return result.toString();
    }
}
