package org.jessies.mathdroid;

import android.app.*;
import android.content.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;

public abstract class Compatibility {
    private static final int SDK_INT; // Cupcake doesn't have Build.VERSION.SDK_INT
    static {
        int sdkInt = 1;
        try {
            sdkInt = Integer.parseInt(Build.VERSION.SDK);
        } catch (NumberFormatException nfe) {
        }
        SDK_INT = sdkInt;
    }
    
    public static Compatibility get() {
        if (/*Build.VERSION.*/SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return new HoneycombCompatibility();
        } else {
            return new PreHoneycombCompatibility();
        }
    }
    
    public abstract boolean isTablet(Activity activity);
    public abstract void fixEditText(EditText editText);
    
    public static class PreHoneycombCompatibility extends Compatibility {
        public boolean isTablet(Activity activity) {
            // TODO: try to support crappy Froyo tablets?
            return false;
        }

        public void fixEditText(EditText editText) {
            editText.setFocusableInTouchMode(true);
            editText.setInputType(InputType.TYPE_NULL);
        }
    }
    
    public static class HoneycombCompatibility extends Compatibility {
        public boolean isTablet(Activity activity) {
            final int SCREENLAYOUT_SIZE_XLARGE = 4; // Not available until API 9.
            return (activity.getResources().getConfiguration().screenLayout & SCREENLAYOUT_SIZE_XLARGE) != 0;
        }

        public void fixEditText(EditText editText) {
            editText.setFocusableInTouchMode(false);
            editText.setTextIsSelectable(true);
        }
    }
}
