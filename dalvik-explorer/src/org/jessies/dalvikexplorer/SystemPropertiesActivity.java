package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.util.*;

public class SystemPropertiesActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(getSystemPropertiesAsString());
    }
    
    // Original in salma-hayek "DebugMenu.java".
    private String getSystemPropertiesAsString() {
        return Utils.sortedStringOfMap(getSystemProperties());
    }
    
    // Original in salma-hayek "DebugMenu.java".
    private Map<String, String> getSystemProperties() {
        HashMap<String, String> result = new HashMap<String, String>();
        Properties properties = System.getProperties();
        Enumeration<?> propertyNames = properties.propertyNames();
        while (propertyNames.hasMoreElements()) {
            String key = (String) propertyNames.nextElement();
            result.put(key, Utils.escapeForJava(properties.getProperty(key)));
        }
        return result;
    }
}
