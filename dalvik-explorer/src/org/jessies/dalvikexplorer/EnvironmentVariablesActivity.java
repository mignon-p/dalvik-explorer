package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.util.*;

public class EnvironmentVariablesActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(getEnvironmentAsString());
        setTitle("Environment Variables (" + System.getenv().size() + ")");
    }
    
    // Original in salma-hayek "DebugMenu.java".
    private String getEnvironmentAsString() {
        return Utils.sortedStringOfMap(System.getenv());
    }
}
