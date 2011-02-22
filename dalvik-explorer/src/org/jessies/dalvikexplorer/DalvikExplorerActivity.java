package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class DalvikExplorerActivity extends ListActivity {
    // Constants identifying the options menu items.
    private static final int OPTIONS_MENU_SEND = 0;
    private static final int OPTIONS_MENU_HELP = 1;
    
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
        new NamedActivity("Build/Device Details", BuildActivity.class),
        new NamedActivity("Charsets", CharsetsActivity.class),
        new NamedActivity("Environment Variables", EnvironmentVariablesActivity.class),
        new NamedActivity("File System", FileSystemActivity.class),
        new NamedActivity("Locales", LocalesActivity.class),
        new NamedActivity("System Properties", SystemPropertiesActivity.class),
        new NamedActivity("Time Zones", TimeZonesActivity.class)
    };
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new BetterArrayAdapter<NamedActivity>(this, ACTIVITIES));
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final NamedActivity destinationActivity = ACTIVITIES[position];
        startActivity(new Intent(this, destinationActivity.activityClass));
    }
    
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, OPTIONS_MENU_SEND, 0, "Mail Report").setIcon(android.R.drawable.ic_menu_send);
        menu.add(0, OPTIONS_MENU_HELP, 0, "Help").setIcon(android.R.drawable.ic_menu_help);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case OPTIONS_MENU_SEND:
            new MailReportTask(this).execute();
            return true;
        case OPTIONS_MENU_HELP:
            showHelp();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void showHelp() {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://code.google.com/p/enh/wiki/DalvikExplorer"));
        startActivity(intent);
    }
}
