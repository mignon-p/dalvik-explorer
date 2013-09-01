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
import javax.net.ssl.*;

public class TlsActivity extends TextViewActivity {
    protected CharSequence title(String unused) {
        return "Transport Layer Security";
    }

    protected String content(String unused) {
        return getTlsDetailsAsString(this, getWindowManager());
    }

    private static String getTlsDetailsAsString(Activity context,
                                                WindowManager wm) {
        final StringBuilder result = new StringBuilder();

        result.append("<html>");

        try {
            final SSLContext ctx = SSLContext.getDefault();
            final SSLParameters dflt = ctx.getDefaultSSLParameters();
            final SSLParameters supp = ctx.getSupportedSSLParameters();

            result.append("<p>All supported protocols and cipher suites, ");
            result.append("with <font color=red>red</font> indicating ");
            result.append("ones enabled by default.</p>");

            result.append("<h1>Protocols</h1>");
            makeList (supp.getProtocols(), dflt.getProtocols(), result);

            result.append("<h1>Cipher suites</h1>");
            makeList (supp.getCipherSuites(), dflt.getCipherSuites(), result);
        } catch (NoSuchAlgorithmException e) {
            result.append(TextUtils.htmlEncode(e.getMessage()));
        }

        result.append("</html>");
        return result.toString();
    }

    private static void makeList (String[] all, String[] enabled,
                                  StringBuilder result) {
        Map<String, Boolean> m = new LinkedHashMap<String, Boolean>();
        for (String s : all) {
            m . put (s, false);
        }
        for (String s : enabled) {
            m . put (s, true);
        }
        // result.append("<ul>");
        for (Map.Entry<String, Boolean> e : m.entrySet()) {
            final String s = e.getKey();
            final boolean b = e.getValue();
            // result.append("<li>");
            if (b)
                result.append("<font color=red>");
            result.append(TextUtils.htmlEncode(s));
            if (b)
                result.append("</font>");
            // result.append("</li>");
            result.append("<br>");
        }
        // result.append("</ul>");
    }
}
