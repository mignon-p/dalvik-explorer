package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public class BuildActivity extends TextViewActivity {
    protected CharSequence title(String unused) {
        return "Build/Device Details";
    }
    
    protected CharSequence content(String unused) {
        return getBuildDetailsAsString(getWindowManager());
    }
    
    static String getBuildDetailsAsString(WindowManager wm) {
        final Build build = new Build();
        
        // These two fields were added to Build at API level 4 (Android 1.6).
        // On 2011-02-02 about 4% of devices were still running 1.5, so we use reflection.
        // http://developer.android.com/resources/dashboard/platform-versions.html
        final String cpuAbi = getFieldReflectively(build, "CPU_ABI");
        final String manufacturer = getFieldReflectively(build, "MANUFACTURER");
        
        final StringBuilder result = new StringBuilder();
        result.append("Manufacturer: " + manufacturer + "\n"); // "Motorola"
        result.append("Model: " + build.MODEL + "\n"); // "Droid"
        result.append('\n');
        result.append("CPU ABI: " + cpuAbi + "\n"); // "armeabi-v7a"
        result.append("Cores: " + Runtime.getRuntime().availableProcessors() + "\n"); // 1
        result.append('\n');
        result.append("Brand: " + build.BRAND + "\n"); // "verizon"
        result.append("Board: " + build.BOARD + "\n"); // "sholes"
        result.append("Device: " + build.DEVICE + "\n"); // "sholes"
        result.append('\n');
        result.append("Build Fingerprint: " + build.FINGERPRINT + "\n"); // "verizon/voles/sholes/sholes:2.1/ERD76/22321:userdebug/test-keys"
        result.append('\n');
        result.append("DalvikVM Heap Size: " + Runtime.getRuntime().maxMemory() / (1024*1024) + " MiB\n"); // "24 MiB"
        result.append('\n');
        
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        result.append("Screen Density: " + metrics.densityDpi + "dpi (" + metrics.density + "x DIP)\n");
        result.append("Screen Size: " + metrics.widthPixels + " x " + metrics.heightPixels + " pixels\n");
        result.append("Exact DPI: " + metrics.xdpi + " x " + metrics.ydpi + "\n");
        double widthInches = metrics.widthPixels/metrics.xdpi;
        double heightInches = metrics.heightPixels/metrics.ydpi;
        double diagonalInches = Math.sqrt(widthInches*widthInches + heightInches*heightInches);
        result.append(String.format("Approximate Dimensions: %.1f\" x %.1f\" (%.1f\" diagonal)\n", widthInches, heightInches, diagonalInches));
        
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
