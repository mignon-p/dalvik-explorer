package org.jessies.mathdroid;

import android.content.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

public class MathdroidEditText extends EditText {
  public MathdroidEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
    Compatibility.get().fixEditText(this);
    setSelectAllOnFocus(true);
  }
}
