package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class TimeZoneActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final String timeZoneId = getIntent().getStringExtra("org.jessies.dalvikexplorer.TimeZone");
        setTitle("Time Zone \"" + timeZoneId + "\"");
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(describeTimeZone(timeZoneId));
    }
    
    private static String describeTimeZone(String id) {
        final StringBuilder result = new StringBuilder();
        
        final TimeZone timeZone = TimeZone.getTimeZone(id);
        
        result.append("Long Display Name: " + timeZone.getDisplayName(false, TimeZone.LONG) + "\n");
        if (timeZone.useDaylightTime()) {
            result.append("Long Display Name (DST): " + timeZone.getDisplayName(true, TimeZone.LONG) + "\n");
        }
        result.append('\n');
        
        result.append("Short Display Name: " + timeZone.getDisplayName(false, TimeZone.SHORT) + "\n");
        if (timeZone.useDaylightTime()) {
            result.append("Short Display Name (DST): " + timeZone.getDisplayName(true, TimeZone.SHORT) + "\n");
        }
        result.append('\n');
        
        result.append("Raw Offset: " + timeZone.getRawOffset()/1000/60 + " minutes\n");
        result.append("Current Offset: " + timeZone.getOffset(System.currentTimeMillis())/1000/60 + " minutes\n");
        result.append('\n');
        
        result.append("Uses DST: " + timeZone.useDaylightTime() + "\n");
        if (timeZone.useDaylightTime()) {
            result.append("DST Savings: " + timeZone.getDSTSavings()/1000/60 + " minutes\n");
            result.append("In DST Now: " + timeZone.inDaylightTime(new Date()) + "\n");
        }
        
        return result.toString();
    }
}
