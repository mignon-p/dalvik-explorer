package org.jessies.dalvikexplorer;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import java.io.*;
import java.util.*;

public class Utils {
    // Original in salma-hayek "DebugMenu.java".
    public static String sortedStringOfMap(Map<String, String> hash) {
        StringBuilder builder = new StringBuilder();
        String[] keys = hash.keySet().toArray(new String[hash.size()]);
        Arrays.sort(keys);
        for (String key : keys) {
            builder.append(key + "=" + hash.get(key) + "\n");
        }
        return builder.toString();
    }
    
    // Original in salma-hayek "StringUtilities.java".
    public static String escapeForJava(CharSequence s) {
        final int sLength = s.length();
        final StringBuilder result = new StringBuilder(sLength);
        for (int i = 0; i < sLength; ++i) {
            final char c = s.charAt(i);
            if (c == '\\') {
                result.append("\\\\");
            } else if (c == '\n') {
                result.append("\\n");
            } else if (c == '\r') {
                result.append("\\r");
            } else if (c == '\t') {
                result.append("\\t");
            } else if (c < ' ' || c > '~') {
                result.append(String.format("\\u%04x", c)); // android-changed.
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    public static String offsetString(int ms, boolean showHours, boolean showMinutes) {
        int minutes = ms/1000/60;
        String result = "";
        if (showHours) {
            result += String.format(Locale.US, "%+03d:%02d", minutes / 60, Math.abs(minutes % 60));
        }
        if (showMinutes) {
            result += String.format(Locale.US, "%s%+d minutes%s", showHours ? " (" : "", minutes, showHours ? ")" : "");
        }
        return result;
    }
    
    public static String appVersion(Context context) {
        String version = "unknown";
        try {
            String packageName = context.getPackageName();
            version = context.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (NameNotFoundException ignored) {
        }
        return version;
    }
    
    public static String readFile(String path) {
        StringBuilder sb = new StringBuilder();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(path));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        } catch (IOException ex) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
