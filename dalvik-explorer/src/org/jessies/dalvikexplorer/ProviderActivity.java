package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.pm.*;
import android.os.*;
import android.text.*;
import android.text.format.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.*;

public class ProviderActivity extends TextViewActivity {
    protected CharSequence title(String unused) {
        return "Security Providers";
    }

    protected String content(String unused) {
        return getProviderDetailsAsString(this, getWindowManager());
    }

    private static String getProviderDetailsAsString(Activity context,
                                                     WindowManager wm) {
        final StringBuilder result = new StringBuilder();

        result.append("<html>");

        Provider[] providers = Security.getProviders();
        for (Provider p : providers) {
            final String name = p.getName();
            final double version = p.getVersion();
            final String info = p.getInfo();
            result.append("<h1>");
            quote(result, name);
            result.append(" ");
            quote(result, String.valueOf(version));
            result.append("</h1><p>");
            quote(result, info);
            result.append("</p>");

            for (Object o : new TreeSet<Object>(p.keySet())) {
                final String key = (String) o;
                if (key.contains(" ")  ||  key.startsWith("Alg.Alias."))
                    continue;
                quote(result, key);
                result.append("<br>");
            }
        }

        result.append("</html>");
        return result.toString();
    }

    private static void quote(StringBuilder result, String s) {
        result.append(TextUtils.htmlEncode(s));
    }
}
