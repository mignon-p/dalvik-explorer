package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.util.*;

public class TimeZonesActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(getTimeZonesAsString());
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
}
