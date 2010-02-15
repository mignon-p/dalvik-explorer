package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.text.ClipboardManager;
import android.view.*;
import android.widget.*;

/**
 * An abstract superclass for our TextView-based activities.
 */
public abstract class TextViewActivity extends Activity {
    private static final int CONTEXT_MENU_COPY = 0;
    private static final int CONTEXT_MENU_MAIL = 1;
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final TextView textView = (TextView) findViewById(R.id.output);
        registerForContextMenu(textView);
        
        final String extraValue = getExtraValue();
        textView.setText(content(extraValue));
        setTitle(title(extraValue));
    }
    
    protected String extraName() {
        return null;
    }
    
    protected abstract CharSequence title(String extraValue);
    
    protected abstract CharSequence content(String extraValue);
    
    protected String getExtraValue() {
        final String extraName = extraName();
        return (extraName != null) ? getIntent().getStringExtra(extraName) : null;
    }
    
    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Details");
        menu.add(0, CONTEXT_MENU_COPY,  0, "Copy to clipboard"); // "Copy" might be ambiguous in FileViewerActivity.
        menu.add(0, CONTEXT_MENU_MAIL,  0, "Send as mail");
    }
    
    @Override public boolean onContextItemSelected(MenuItem item) {
        final TextView textView = (TextView) findViewById(R.id.output);
        final CharSequence title = getTitle();
        final CharSequence content = textView.getText();
        switch (item.getItemId()) {
        case CONTEXT_MENU_COPY:
            return copyToClipboard(title, content);
        case CONTEXT_MENU_MAIL:
            return mail(title, content);
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    private boolean copyToClipboard(CharSequence title, CharSequence content) {
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setText(title + "\n\n" + content);
        return true;
    }
    
    private boolean mail(CharSequence title, CharSequence content) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Dalvik Explorer: " + title);
        intent.putExtra(Intent.EXTRA_TEXT, content);
        startActivity(intent);
        return true;
    }
}
