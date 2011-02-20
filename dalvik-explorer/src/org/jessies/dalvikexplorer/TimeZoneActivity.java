package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class TimeZoneActivity extends TextViewActivity {
    protected String extraName() {
        return "org.jessies.dalvikexplorer.TimeZone";
    }
    
    protected CharSequence title(String timeZoneId) {
        return "Time Zone \"" + timeZoneId + "\"";
    }
    
    protected CharSequence content(String timeZoneId) {
        return describeTimeZone(timeZoneId);
    }
    
    static String describeTimeZone(String id) {
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
        result.append('\n');
        
        result.append("Source: tzdata" + TimeUtils.getTimeZoneDatabaseVersion() + "\n");
        
        return result.toString();
    }
}
