package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.pm.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class BuildActivity extends TextViewActivity {
    protected CharSequence title(String unused) {
        return "Build Details";
    }
    
    protected CharSequence content(String unused) {
        return getBuildDetailsAsString(this, getWindowManager());
    }
    
    static String getBuildDetailsAsString(Activity context, WindowManager wm) {
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
        result.append("Brand: " + build.BRAND + "\n"); // "verizon"
        result.append("Board: " + build.BOARD + "\n"); // "sholes"
        result.append("Device: " + build.DEVICE + "\n"); // "sholes"
        result.append('\n');
        result.append("CPU ABI: " + cpuAbi + "\n"); // "armeabi-v7a"
        result.append('\n');
        result.append("Build Fingerprint: " + build.FINGERPRINT + "\n"); // "verizon/voles/sholes/sholes:2.1/ERD76/22321:userdebug/test-keys"
        result.append('\n');
        result.append("Kernel Version: " + Utils.readFile("/proc/version")); // "Linux version 3.0.8-g034fec9 (android-build@vpbs1.mtv.corp.google.com) (gcc version 4.4.3 (GCC) ) #1 SMP PREEMPT Tue Mar 13 15:46:20 PDT 2012"
        result.append('\n');
        result.append("DalvikVM Heap Size: " + Runtime.getRuntime().maxMemory() / (1024*1024) + " MiB\n"); // "24 MiB"
        result.append('\n');
        
        try {
            Class<?> vmDebugClass = Class.forName("dalvik.system.VMDebug");
            Method getVmFeatureListMethod = vmDebugClass.getDeclaredMethod("getVmFeatureList");
            String[] features = (String[]) getVmFeatureListMethod.invoke(null);
            result.append("DalvikVM features:\n");
            for (String feature : features) {
                result.append("  " + feature + "\n");
            }
            result.append('\n');
        } catch (Throwable ignored) {
        }
        
        result.append("Features:\n");
        String openGlEsVersion = null;
        for (FeatureInfo feature : context.getPackageManager().getSystemAvailableFeatures()) {
            if (feature.name != null) {
                result.append("  " + feature.name + "\n");
            } else {
                openGlEsVersion = feature.getGlEsVersion();
            }
        }
        result.append('\n');
        
        if (openGlEsVersion != null) {
            result.append("OpenGL ES version: " + openGlEsVersion + "\n");
            result.append('\n');
        }
        
        result.append("Shared Java libraries:\n");
        for (String library : context.getPackageManager().getSystemSharedLibraryNames()) {
            result.append("  " + library + "\n");
        }
        
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
