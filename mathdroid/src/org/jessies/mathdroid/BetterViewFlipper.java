package org.jessies.mathdroid;

import android.content.*;
import android.util.*;
import android.widget.*;

/**
 * Works around http://code.google.com/p/android/issues/detail?id=6191.
 */
public class BetterViewFlipper extends ViewFlipper {
    public BetterViewFlipper(Context context) {
        super(context);
    }

    public BetterViewFlipper(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override protected void onDetachedFromWindow() {
        try {
            super.onDetachedFromWindow();
        } catch (IllegalArgumentException ex) {
            System.err.println("Ignoring Android framework bug http://code.google.com/p/android/issues/detail?id=6191 --- ignored exception follows...");
            ex.printStackTrace();
            stopFlipping();
        }
    }
}
