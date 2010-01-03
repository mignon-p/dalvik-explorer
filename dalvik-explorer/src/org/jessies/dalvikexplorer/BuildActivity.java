package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public class BuildActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setTitle("Build/Device Details");
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(getBuildDetailsAsString());
    }
    
    static String getBuildDetailsAsString() {
        final Build build = new Build();
        
        // These two fields were added to Build at API level 4 (Android 1.6).
        // Since currently (2010-01-03) about 25% of devices are running 1.5, we use reflection.
        // http://developer.android.com/resources/dashboard/platform-versions.html
        final String cpuAbi = getFieldReflectively(build, "CPU_ABI");
        final String manufacturer = getFieldReflectively(build, "MANUFACTURER");
        
        final StringBuilder result = new StringBuilder();
        result.append("Manufacturer: " + manufacturer + "\n"); // "Motorola"
        result.append("Model: " + build.MODEL + "\n"); // "Droid"
        result.append("CPU ABI: " + cpuAbi + "\n"); // "armeabi-v7a"
        result.append('\n');
        result.append("Brand: " + build.BRAND + "\n"); // "verizon"
        result.append("Board: " + build.BOARD + "\n"); // "sholes"
        result.append("Device: " + build.DEVICE + "\n"); // "sholes"
        result.append('\n');
        result.append("Build Fingerprint: " + build.FINGERPRINT + "\n"); // "verizon/voles/sholes/sholes:2.1/ERD76/22321:userdebug/test-keys"
        return result.toString();
    }
    
    private static String getFieldReflectively(Build build, String fieldName) {
        try {
            final Field field = Build.class.getField(fieldName);
            return field.get(build).toString();
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
