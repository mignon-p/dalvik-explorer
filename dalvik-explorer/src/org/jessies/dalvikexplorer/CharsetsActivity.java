package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.nio.charset.*;
import java.util.*;

public class CharsetsActivity extends ListActivity {
    private static class CharsetListItem {
        private final Charset charset;
        private CharsetListItem(Charset charset) {
            this.charset = charset;
        }
        @Override public String toString() {
            String result = charset.displayName();
            if (charset.equals(Charset.defaultCharset())) {
                result += " (default)";
            }
            return result;
        }
    }
    private static final List<CharsetListItem> CHARSETS = gatherCharsets();
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ArrayAdapter<CharsetListItem>(this, android.R.layout.simple_list_item_1, CHARSETS));
        setTitle("Charsets (" + CHARSETS.size() + ")");
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final Intent intent = new Intent(this, CharsetActivity.class);
        intent.putExtra("org.jessies.dalvikexplorer.Charset", CHARSETS.get(position).charset.name());
        startActivity(intent);
    }
    
    private static List<CharsetListItem> gatherCharsets() {
        final SortedMap<String,Charset> charsets = Charset.availableCharsets();
        final Charset defaultCharset = Charset.defaultCharset();
        // Put the default charset at the top of the list...
        final List<CharsetListItem> result = new ArrayList<CharsetListItem>(charsets.size());
        result.add(new CharsetListItem(defaultCharset));
        // ...followed by all the others.
        for (String name : charsets.keySet()) {
            final Charset charset = charsets.get(name);
            if (!charset.equals(defaultCharset)) {
                result.add(new CharsetListItem(charset));
            }
        }
        return result;
    }
}
