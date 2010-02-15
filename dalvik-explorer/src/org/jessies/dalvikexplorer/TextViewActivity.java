package org.jessies.dalvikexplorer;

import android.app.*;
import android.os.*;
import android.widget.*;

/**
 * An abstract superclass for our TextView-based activities.
 */
public abstract class TextViewActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        String extraName = extraName();
        String extraValue = null;
        if (extraName != null) {
            extraValue = getIntent().getStringExtra(extraName);
        }
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(content(extraValue));
        setTitle(title(extraValue));
    }
    
    protected String extraName() {
        return null;
    }
    
    protected abstract CharSequence title(String extraValue);
    
    protected abstract CharSequence content(String extraValue);
}
