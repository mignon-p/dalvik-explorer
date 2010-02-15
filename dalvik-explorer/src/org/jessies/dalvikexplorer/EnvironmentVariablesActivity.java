package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.util.*;

public class EnvironmentVariablesActivity extends TextViewActivity {
    protected CharSequence title(String unused) {
        return "Environment Variables (" + System.getenv().size() + ")";
    }
    
    protected CharSequence content(String unused) {
        return getEnvironmentAsString();
    }
    
    // Original in salma-hayek "DebugMenu.java".
    static String getEnvironmentAsString() {
        return Utils.sortedStringOfMap(System.getenv());
    }
}
