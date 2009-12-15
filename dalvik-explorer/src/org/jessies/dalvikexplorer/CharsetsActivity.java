package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.nio.charset.*;
import java.util.*;

public class CharsetsActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(getCharsetsAsString());
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
             * Set<String> aliases = charset.aliases();
             * for (String alias : aliases) {
             * result.append("  alias: " + alias + "\n");
             * }
             */
        }
        return result.toString();
    }
}
