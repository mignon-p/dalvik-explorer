package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class DalvikExplorerActivity extends ListActivity {
    // A (list view label, activity class) pair.
    private static class NamedActivity {
        private final String name;
        private final Class<?> activityClass;
        private NamedActivity(String name, Class<?> activityClass) {
            this.name = name;
            this.activityClass = activityClass;
        }
        @Override public String toString() {
            return name;
        }
    }
    
    private static final NamedActivity[] ACTIVITIES = new NamedActivity[] {
        new NamedActivity("Charsets", CharsetsActivity.class),
        new NamedActivity("Environment Variables", EnvironmentVariablesActivity.class),
        new NamedActivity("Locales", LocalesActivity.class),
        new NamedActivity("System Properties", SystemPropertiesActivity.class),
        new NamedActivity("Time Zones", TimeZonesActivity.class)
    };
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ArrayAdapter<NamedActivity>(this, android.R.layout.simple_list_item_1, ACTIVITIES));
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final NamedActivity destinationActivity = ACTIVITIES[position];
        startActivity(new Intent(this, destinationActivity.activityClass));
    }
}
