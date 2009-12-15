package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;

public class DalvikExplorerActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // TODO: break this up into a top-level ListView that lets you choose what you're interested in and drill down.
        String output = "";
        output += "System Properties:\n\n";
        output += getSystemPropertiesAsString() + "\n\n";
        
        output += "Environment Variables:\n\n";
        output += getEnvironmentAsString() + "\n\n";
        
        output += "Charsets:\n\n";
        output += getCharsetsAsString() + "\n\n";
        
        output += "Locales:\n\n";
        output += getLocalesAsString() + "\n\n";
        
        output += "Time Zones:\n\n";
        output += getTimeZonesAsString() + "\n\n";
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(output);
    }
    
    private String getTimeZonesAsString() {
        final StringBuilder result = new StringBuilder();
        final TimeZone defaultTimeZone = TimeZone.getDefault();
        for (String id : TimeZone.getAvailableIDs()) {
            final TimeZone timeZone = TimeZone.getTimeZone(id);
            result.append(id);
            if (timeZone.equals(defaultTimeZone)) {
                result.append(" (default)");
            }
            result.append('\n');
            // TODO: add summary details
        }
        result.append("    " + TimeZone.getAvailableIDs().length);
        return result.toString();
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
            final String displayCountry = locale.getDisplayCountry();
            if (displayCountry.length() > 0) {
                result.append(" country: " + displayCountry + " (" + locale.getISO3Country() + ")\n");
            }
            final String displayLanguage = locale.getDisplayLanguage();
            if (displayLanguage.length() > 0) {
                result.append(" language: " + displayLanguage + " (" + locale.getISO3Language() + ")\n");
            }
            */
        }
        result.append("    " + Locale.getAvailableLocales().length);
        return result.toString();
    }
    
    private String getCharsetsAsString() {
        final StringBuilder result = new StringBuilder();
        final SortedMap<String,Charset> charsets = Charset.availableCharsets();
        final Charset defaultCharset = Charset.defaultCharset();
        for (String name : charsets.keySet()) {
            final Charset charset = charsets.get(name);
            result.append(name);
            final String displayName = charset.displayName();
            if (!displayName.equals(name)) {
                result.append(" \"" + displayName + "\"");
            }
            if (!charset.canEncode()) {
                result.append(" (decode only)");
            }
            if (charset.equals(defaultCharset)) {
                result.append(" (default)");
            }
            if (!charset.isRegistered()) {
                result.append(" (non-IANA)");
            }
            result.append('\n');
            /*
            Set<String> aliases = charset.aliases();
            for (String alias : aliases) {
                result.append("  alias: " + alias + "\n");
            }
            */
        }
        return result.toString();
    }
    
    // Original in salma-hayek "DebugMenu.java".
    private String getEnvironmentAsString() {
        return sortedStringOfMap(System.getenv());
    }
    
    // Original in salma-hayek "DebugMenu.java".
    private String getSystemPropertiesAsString() {
        return sortedStringOfMap(getSystemProperties());
    }
    
    // Original in salma-hayek "DebugMenu.java".
    private Map<String, String> getSystemProperties() {
        HashMap<String, String> result = new HashMap<String, String>();
        Properties properties = System.getProperties();
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String key = (String) propertyNames.nextElement();
            result.put(key, escapeForJava(properties.getProperty(key)));
        }
        return result;
    }
    
    // Original in salma-hayek "DebugMenu.java".
    private static String sortedStringOfMap(Map<String, String> hash) {
        StringBuilder builder = new StringBuilder();
        String[] keys = hash.keySet().toArray(new String[hash.size()]);
        Arrays.sort(keys);
        for (String key : keys) {
            builder.append(key + "=" + hash.get(key) + "\n");
        }
        return builder.toString();
    }
    
    // Original in salma-hayek "StringUtilities.java".
    public static String escapeForJava(CharSequence s) {
        final int sLength = s.length();
        final StringBuilder result = new StringBuilder(sLength);
        for (int i = 0; i < sLength; ++i) {
            final char c = s.charAt(i);
            if (c == '\\') {
                result.append("\\\\");
            } else if (c == '\n') {
                result.append("\\n");
            } else if (c == '\r') {
                result.append("\\r");
            } else if (c == '\t') {
                result.append("\\t");
            } else if (c < ' ' || c > '~') {
                result.append(String.format("\\u%04x", c)); // android-changed.
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
