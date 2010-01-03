package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.widget.*;
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
        Build build = new Build();
        StringBuilder result = new StringBuilder();
        result.append("Manufacturer: " + build.MANUFACTURER + "\n"); // "Motorola"
        result.append("Model: " + build.MODEL + "\n"); // "Droid"
        result.append("CPU ABI: " + build.CPU_ABI + "\n"); // "armeabi-v7a"
        result.append('\n');
        result.append("Brand: " + build.BRAND + "\n"); // "verizon"
        result.append("Board: " + build.BOARD + "\n"); // "sholes"
        result.append("Device: " + build.DEVICE + "\n"); // "sholes"
        result.append('\n');
        result.append("Build Fingerprint: " + build.FINGERPRINT + "\n"); // "verizon/voles/sholes/sholes:2.1/ERD76/22321:userdebug/test-keys"
        return result.toString();
    }
}
