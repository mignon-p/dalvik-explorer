package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;

public class TimeZoneActivity extends TextViewActivity {
  protected String extraName() {
    return "org.jessies.dalvikexplorer.TimeZone";
  }
  
  protected CharSequence title(String timeZoneId) {
    return "Time Zone \"" + timeZoneId + "\"";
  }
  
  protected CharSequence content(String timeZoneId) {
    return describeTimeZone(timeZoneId);
  }
  
  private String describeTimeZone(String id) {
    final StringBuilder result = new StringBuilder();
    
    final TimeZone timeZone = TimeZone.getTimeZone(id);
    final Date now = new Date();
    final DateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss Z (EEEE)", Locale.US);
    
    result.append("Long Display Name: " + timeZone.getDisplayName(false, TimeZone.LONG) + "\n");
    if (timeZone.useDaylightTime()) {
      result.append("Long Display Name (DST): " + timeZone.getDisplayName(true, TimeZone.LONG) + "\n");
    }
    result.append('\n');
    
    result.append("Short Display Name: " + timeZone.getDisplayName(false, TimeZone.SHORT) + "\n");
    if (timeZone.useDaylightTime()) {
      result.append("Short Display Name (DST): " + timeZone.getDisplayName(true, TimeZone.SHORT) + "\n");
    }
    result.append('\n');
    
    iso8601.setTimeZone(TimeZone.getDefault());
    result.append("Time Here: " + iso8601.format(now) + "\n");
    iso8601.setTimeZone(timeZone);
    result.append("Time There: " + iso8601.format(now) + "\n");
    result.append('\n');
    
    result.append("Raw Offset: UTC" + Utils.offsetString(timeZone.getRawOffset(), true, true) + "\n");
    result.append("Current Offset: UTC" + Utils.offsetString(timeZone.getOffset(System.currentTimeMillis()), true, true) + "\n");
    result.append('\n');
    
    result.append("Uses DST: " + timeZone.useDaylightTime() + "\n");
    if (timeZone.useDaylightTime()) {
      result.append("DST Savings: " + Utils.offsetString(timeZone.getDSTSavings(), false, true) + "\n");
      result.append("In DST Now: " + timeZone.inDaylightTime(now) + "\n");
    }
    result.append('\n');
    
    result.append("Source: tzdata" + TimeUtils.getTimeZoneDatabaseVersion() + "\n");
    result.append('\n');
    
    // TODO: make this available in Android, and get it from there (falling back to our hard-coded copy).
    InputStream is = getResources().openRawResource(R.raw.zone_tab);
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      boolean found = false;
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains(id)) {
          String[] fields = line.split("\t");
          // 0: country code
          // 1: coordinates
          // 2: id
          // 3: comments
          if (!fields[2].equals(id)) {
            continue;
          }
          String countryCode = fields[0];
          String country = new Locale("", countryCode).getDisplayCountry(Locale.getDefault());
          result.append("Country: " + countryCode + " (" + country + ")\n");
          String iso6709Coordinates = fields[1];
          String dmsCoordinates;
          if (iso6709Coordinates.length() == 11) {
            dmsCoordinates = iso6709Coordinates.replaceAll("([+-])(\\d{2})(\\d{2})([+-])(\\d{3})(\\d{2})", "$1$2\u00b0$3', $4$5\u00b0$6'");
          } else {
            dmsCoordinates = iso6709Coordinates.replaceAll("([+-])(\\d{2})(\\d{2})(\\d{2})([+-])(\\d{3})(\\d{2})(\\d{2})", "$1$2\u00b0$3'$4\", $5$6\u00b0$7'$8\"");
          }
          result.append("Coordinates: " + dmsCoordinates + "\n");
          String notes = (fields.length > 3) ? fields[3] : "(no notes)";
          result.append("Notes: " + notes + "\n");
          found = true;
        }
      }
      if (!found) {
        result.append("(Not found in zone.tab.)\n");
      }
    } catch (IOException ex) {
      result.append("(Failed to read zone.tab.)\n");
    } finally {
      try {
        is.close();
      } catch (IOException ignored) {
      }
    }
    result.append('\n');
    
    try {
      describeTransitions(result, timeZone);
      result.append('\n');
    } catch (Exception unexpected) {
      result.append("(Couldn't find transition data in " + timeZone.getClass() + ".)\n");
      unexpected.printStackTrace();
    }
    
    return result.toString();
  }
  
  private static void describeTransitions(StringBuilder result, TimeZone tz) throws IllegalAccessException, NoSuchFieldException {
    Class<?> zoneInfoClass = tz.getClass();
    Field mTransitionsField = zoneInfoClass.getDeclaredField("mTransitions");
    mTransitionsField.setAccessible(true);
    int[] mTransitions = (int[]) mTransitionsField.get(tz);
    
    long unixNow = System.currentTimeMillis() / 1000;
    boolean shownNow = false;
    
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss Z", Locale.US);
    df.setTimeZone(tz);
    DateFormat weekdayFormat = new SimpleDateFormat("EEEE", Locale.US);
    weekdayFormat.setTimeZone(tz);
    
    Formatter f = new Formatter(result);
    f.format("Transitions:\n");
    for (int i = 0; i < mTransitions.length; ++i) {
      if (!shownNow && mTransitions[i] > unixNow) {
        f.format("\n  -- now (%d) --\n\n", unixNow);
        shownNow = true;
      }
      String fromTime = formatTime(df, mTransitions[i] - 1);
      String toTime = formatTime(df, mTransitions[i]);
      f.format("  %s ... %s (%d)", fromTime, toTime, mTransitions[i]);
      
      String weekday = weekdayFormat.format(new Date(1000L * mTransitions[i]));
      if (!weekday.equals("Sunday")) {
        f.format(" -- %s", weekday);
      }
      result.append('\n');
    }
  }
  
  private static String formatTime(DateFormat df, int s) {
    long ms = ((long) s) * 1000L;
    return df.format(new Date(ms));
  }
}
