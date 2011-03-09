package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class TimeZonesActivity extends BetterListActivity {
    private static class TimeZoneListItem {
        private final TimeZone timeZone;
        private TimeZoneListItem(TimeZone timeZone) {
            this.timeZone = timeZone;
        }
        @Override public String toString() {
            String result = timeZone.getID();
            if (timeZone.equals(TimeZone.getDefault())) {
                result += " (default)";
            }
            return result;
        }
    }
    private static final List<TimeZoneListItem> TIME_ZONES = gatherTimeZones();
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new BetterArrayAdapter<TimeZoneListItem>(this, TIME_ZONES));
        setTitle("Time Zones (" + TIME_ZONES.size() + ")");
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final Intent intent = new Intent(this, TimeZoneActivity.class);
        final TimeZoneListItem item = (TimeZoneListItem) l.getAdapter().getItem(position);
        intent.putExtra("org.jessies.dalvikexplorer.TimeZone", item.timeZone.getID());
        startActivity(intent);
    }
    
    private static List<TimeZoneListItem> gatherTimeZones() {
        final String[] availableIds = TimeZone.getAvailableIDs();
        final TimeZone defaultTimeZone = TimeZone.getDefault();
        // Put the default time zone at the top of the list...
        final List<TimeZoneListItem> result = new ArrayList<TimeZoneListItem>(availableIds.length);
        result.add(new TimeZoneListItem(defaultTimeZone));
        // ...followed by all the others.
        for (String id : availableIds) {
            if (Thread.currentThread().isInterrupted()) return null;
            final TimeZone timeZone = TimeZone.getTimeZone(id);
            if (!timeZone.equals(defaultTimeZone)) {
                result.add(new TimeZoneListItem(timeZone));
            }
        }
        return result;
    }
    
    static String describeTimeZones() {
        StringBuilder result = new StringBuilder();
        for (TimeZoneListItem item : gatherTimeZones()) {
            if (Thread.currentThread().isInterrupted()) return null;
            result.append(TimeZoneActivity.describeTimeZone(item.timeZone.getID()));
            result.append('\n');
        }
        return result.toString();
    }
}
