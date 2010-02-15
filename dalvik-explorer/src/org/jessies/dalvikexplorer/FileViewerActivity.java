package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;

public class FileViewerActivity extends TextViewActivity {
    protected String extraName() {
        return "org.jessies.dalvikexplorer.Path";
    }
    
    protected CharSequence title(String path) {
        return path;
    }
    
    protected CharSequence content(String path) {
        return readFile(path);
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
