package org.jessies.dalvikexplorer;

import java.util.*;

public class LocaleListItem {
    private final Locale locale;
    
    public LocaleListItem(Locale locale) {
        this.locale = locale;
    }
    
    public Locale locale() {
        return locale;
    }
    
    @Override public String toString() {
        String result = locale.toString();
        if (locale.equals(Locale.getDefault())) {
            result += " (default)";
        }
        return result;
    }
    
    public String toSubtitle() {
        return locale.getDisplayName();
    }
}
