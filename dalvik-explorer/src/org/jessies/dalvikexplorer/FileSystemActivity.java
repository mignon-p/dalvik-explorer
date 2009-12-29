package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class FileSystemActivity extends ListActivity {
    private static class FileListItem {
        private final File file;
        private final String label;
        private FileListItem(File file, String label) {
            this.file = file;
            this.label = label;
        }
        private FileListItem(File file) {
            this(file, makeLabel(file));
        }
        private static String makeLabel(File file) {
            String result = file.getName();
            return file.isDirectory() ? result + "/" : result;
        }
        @Override public String toString() {
            return label;
        }
    }
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String path = getIntent().getStringExtra("org.jessies.dalvikexplorer.Path");
        if (path == null) {
            path = "/";
        }
        
        setListAdapter(new ArrayAdapter<FileListItem>(this, android.R.layout.simple_list_item_1, directoryItems(path)));
        setTitle(path + " (" + getListAdapter().getCount() + ")");
    }
    
    private List<FileListItem> directoryItems(String path) {
        File[] files = new File(path).listFiles();
        if (files == null) {
            // Returning null is how the retarded java.io.File API reports failure.
            Toast.makeText(this, "Couldn't list directory '" + path + "'", Toast.LENGTH_SHORT).show();
            finish();
            return Collections.emptyList();
        }
        
        Arrays.sort(files);
        
        ArrayList<FileListItem> result = new ArrayList<FileListItem>();
        for (File file : files) {
            result.add(new FileListItem(file));
        }
        return result;
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final Object item = getListAdapter().getItem(position);
        final File child = ((FileListItem) item).file;
        final Intent intent = new Intent(this, child.isDirectory() ? FileSystemActivity.class : FileViewerActivity.class);
        intent.putExtra("org.jessies.dalvikexplorer.Path", child.toString());
        startActivity(intent);
    }
}
