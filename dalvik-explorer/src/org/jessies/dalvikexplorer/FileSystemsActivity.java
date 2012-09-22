package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class FileSystemsActivity extends BetterListActivity {
  private static class FsListItem implements Comparable<FsListItem> {
    private final String fs;
    private final String mountPoint;
    private final String type;
    private final String options;
    
    private FsListItem(String fs, String mountPoint, String type, String options) {
      this.fs = fs;
      this.mountPoint = mountPoint;
      this.type = type;
      this.options = options;
    }
    
    @Override public String toString() {
      return mountPoint;
    }
    
    public String toSubtitle() {
      File f = new File(mountPoint);
      long totalBytes = f.getTotalSpace();
      long freeBytes = f.getFreeSpace();
      long usedBytes = totalBytes - freeBytes;
      
      return fs + "\n" +
          options + "\n" +
          Utils.prettySize(usedBytes) + " of " + Utils.prettySize(totalBytes) + " used (" + type + ")";
    }
        
    @Override public int compareTo(FsListItem other) {
      return String.CASE_INSENSITIVE_ORDER.compare(this.mountPoint, other.mountPoint);
    }
  }
  
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setListAdapter(new BetterArrayAdapter<FsListItem>(this, fileSystems(), FsListItem.class, "toSubtitle"));
    setTitle("File Systems (" + getListAdapter().getCount() + ")");
  }
  
  private List<FsListItem> fileSystems() {
    ArrayList<FsListItem> result = new ArrayList<FsListItem>();
    
    String[] mountLines = Utils.readLines("/proc/mounts");
    for (String mountLine : mountLines) {
      String[] fields = mountLine.split(" ");
      File f = new File(fields[1]);
      if (f.getTotalSpace() == 0 && !fields[1].equals("/")) {
        continue;
      }
      result.add(new FsListItem(fields[0], fields[1], fields[2], fields[3]));
    }
    
    Collections.sort(result);
    return result;
  }
  
  @Override protected void onListItemClick(ListView l, View v, int position, long id) {
    final FsListItem item = (FsListItem) l.getAdapter().getItem(position);
    final Intent intent = new Intent(this, FileSystemActivity.class);
    intent.putExtra("org.jessies.dalvikexplorer.Path", item.mountPoint);
    startActivity(intent);
  }
}
