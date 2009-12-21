package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.nio.charset.*;
import java.util.*;

public class CharsetActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final String charsetName = getIntent().getStringExtra("org.jessies.dalvikexplorer.Charset");
        setTitle("Charset \"" + charsetName + "\"");
        
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(describeCharset(charsetName));
    }
    
    private String describeCharset(String name) {
        final StringBuilder result = new StringBuilder();
        final Charset charset = Charset.forName(name);
        result.append("Canonical Name: " + charset.name() + "\n");
        if (!charset.displayName().equals(charset.name())) {
            result.append("Display Name: " + charset.displayName() + "\n");
        }
        result.append('\n');
        result.append("Can Encode: " + charset.canEncode() + "\n");
        result.append("IANA Registered: " + charset.isRegistered() + "\n");
        result.append('\n');
        Set<String> aliases = charset.aliases();
        if (aliases.size() > 0) {
            result.append("Aliases:\n");
            for (String alias : aliases) {
                result.append(' ');
                result.append(alias);
                result.append('\n');
            }
        }
        return result.toString();
    }
}
