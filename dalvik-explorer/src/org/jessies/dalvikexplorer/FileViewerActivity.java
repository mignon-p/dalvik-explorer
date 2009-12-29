package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;

public class FileViewerActivity extends Activity {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        final String path = getIntent().getStringExtra("org.jessies.dalvikexplorer.Path");
        setTitle(path);
        TextView textView = (TextView) findViewById(R.id.output);
        textView.setText(readFile(path));
    }
    
    private CharSequence readFile(String path) {
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(path));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
                result.append('\n');
            }
            in.close();
        } catch (IOException ex) {
            Toast.makeText(this, "Couldn't read '" + path + "'", Toast.LENGTH_SHORT).show();
            finish();
        }
        return result;
    }
}
