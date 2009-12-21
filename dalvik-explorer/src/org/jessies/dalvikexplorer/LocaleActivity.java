package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class LocaleActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final String localeName = getIntent().getStringExtra("org.jessies.dalvikexplorer.Locale");
        setTitle("Locale \"" + localeName + "\"");
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(describeLocale(localeName));
    }
    
    private static Locale localeByName(String name) {
        if (name.length() == 0) {
            return new Locale("", "", "");
        }
        
        int languageEnd = name.indexOf('_');
        if (languageEnd == -1) {
            return new Locale(name, "", "");
        }
        
        String language = name.substring(0, languageEnd);
        name = name.substring(languageEnd + 1);
        
        int countryEnd = name.indexOf('_');
        if (countryEnd == -1) {
            return new Locale(language, name, "");
        }
        
        String country = name.substring(0, countryEnd);
        String variant = name.substring(countryEnd + 1);
        
        return new Locale(language, country, variant);
    }
    
    private static String describeLocale(String name) {
        final StringBuilder result = new StringBuilder();
        
        final Locale locale = localeByName(name);
        
        result.append("Display Name: " + locale.getDisplayName() + "\n");
        result.append('\n');
        
        if (locale.getLanguage().length() > 0) {
            result.append("Display Language: " + locale.getDisplayLanguage() + "\n");
            result.append("2-Letter Language Code: " + locale.getLanguage() + "\n");
            result.append("3-Letter Language Code: " + locale.getISO3Language() + "\n");
            result.append('\n');
        }
        if (locale.getCountry().length() > 0) {
            //result.append("Country: " + locale.getDisplayCountry() + " (" + locale.getCountry() + "/" + locale.getISO3Country() + ")\n");
            result.append("Display Country: " + locale.getDisplayCountry() + "\n");
            result.append("2-Letter Country Code: " + locale.getCountry() + "\n");
            result.append("3-Letter Country Code: " + locale.getISO3Country() + "\n");
            result.append('\n');
        }
        if (locale.getVariant().length() > 0) {
            //result.append("Display Variant: " + locale.getDisplayVariant() + " (" + locale.getVariant() + ")\n");
            result.append("Display Variant: " + locale.getDisplayVariant() + "\n");
            result.append("Variant Code: " + locale.getVariant() + "\n");
        }
        
        return result.toString();
    }
}
