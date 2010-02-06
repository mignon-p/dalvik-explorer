/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jessies.icsbot;

import android.app.*;
import android.content.*;
import android.database.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.text.*;
import android.text.format.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.*;
import java.text.DateFormat;
import java.util.*;

public class IcsBotViewActivity extends Activity {
    private static final String TAG = "IcsBot";
    
    private final View.OnClickListener mImportListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            importCalendar(true);
            finish();
        }
    };
    
    private final View.OnClickListener mCancelListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            finish();
        }
    };
    
    private static class CalendarInfo {
        public final long id;
        public final String name;
        
        public CalendarInfo(long id, String name) {
            this.id = id;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    private String getIntentDataAsString() {
        Intent intent = getIntent();
        String result = intent.getStringExtra("ics");
        if (result != null) {
            return result;
        }
        
        Uri content = intent.getData();
        if (content != null) {
            return getUriDataAsString(content);
        }
        return null;
    }
    
    private String getUriDataAsString(Uri content) {
        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(content);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int bytesRead = -1;
            int pos = 0;
            while ((bytesRead = is.read(buf)) != -1) {
                baos.write(buf, pos, bytesRead);
                pos += bytesRead;
            }
            return new String(baos.toByteArray(), "UTF-8");
        } catch (FileNotFoundException fnfe) {
            Log.w(TAG, "Could not open data.", fnfe);
        } catch (IOException ioe) {
            Log.w(TAG, "Could not read data.", ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    Log.w(TAG, "Could not close InputStream.", ioe);
                }
            }
        }
        return null;
    }
    
    private TextView mTextView;
    private Spinner mCalendarsSpinner;
    
    private ICalendar.Component mCalendar;
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mTextView = (TextView) findViewById(R.id.output);
        
        Button importButton = (Button) findViewById(R.id.import_button);
        importButton.setOnClickListener(mImportListener);
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(mCancelListener);
        
        String data = getIntentDataAsString();
        if (data == null) {
            Log.w(TAG, "No iCalendar data to parse.");
            finish();
            return;
        }
        parseCalendar(data);
        populateCalendarSpinner();
        importCalendar(false);
        mTextView.append(data);
    }
    
    private void parseCalendar(String data) {
        try {
            mCalendar = ICalendar.parseCalendar(data);
        } catch (ICalendar.FormatException fe) {
            Log.d(TAG, "Could not parse iCalendar.", fe);
            finish();
            return;
        }
        int eventCount = 0;
        if (mCalendar.getComponents() != null) {
            for (ICalendar.Component component : mCalendar.getComponents()) {
                if ("VEVENT".equals(component.getName())) {
                    // TODO: display a list of the events (start time, title) in the UI?
                    ++eventCount;
                }
            }
        }
        if (eventCount == 0) {
            Log.d(TAG, "No events in iCalendar.");
            finish();
            return;
        }
        // TODO: special-case a single-event calendar.  switch to the
        // EventActivity, once the EventActivity supports displaying data that
        // is passed in via the extras.
        // OR, we could flip things around, where the EventActivity handles ICS
        // import by default, and delegates to the IcsImportActivity if it finds
        // that there are more than one event in the iCalendar.  that would
        // avoid an extra activity launch for the expected common case of
        // importing a single event.
        mTextView.append("Events: " + Integer.toString(eventCount));
    }
    
    private static final String DISPLAY_NAME_COLUMN = "displayName";
    private static final String _ID_COLUMN = "_id";
    private static final String SELECTED_COLUMN = "selected";
    private static final String ACCESS_LEVEL_COLUMN = "access_level";
    private static final int CONTRIBUTOR_ACCESS = 500;
    private static final int STATUS_CONFIRMED = 1;
    // Constants from Calendar.EventsColumns...
    private static final String ALL_DAY = "allDay";
    private static final String CALENDAR_ID = "calendar_id";
    private static final String DESCRIPTION = "description";
    private static final String DTEND = "dtend";
    private static final String DTSTART = "dtstart";
    private static final String DURATION = "duration";
    private static final String EVENT_LOCATION = "eventLocation";
    private static final String EVENT_TIMEZONE = "eventTimezone";
    private static final String STATUS = "eventStatus";
    private static final String TITLE = "title";
    
    private void populateCalendarSpinner() {
        mCalendarsSpinner = (Spinner) findViewById(R.id.calendars);
        
        String[] columns = new String[] { _ID_COLUMN, DISPLAY_NAME_COLUMN, SELECTED_COLUMN, ACCESS_LEVEL_COLUMN };
        Cursor c = getContentResolver().query(Uri.parse("content://calendar/calendars"), columns, SELECTED_COLUMN + "=1 AND " + ACCESS_LEVEL_COLUMN + ">=" + CONTRIBUTOR_ACCESS, null, null /* sort order */);
        
        ArrayList<CalendarInfo> items = new ArrayList<CalendarInfo>();
        try {
            // TODO: write a custom adapter that wraps the cursor?
            int idColumn = c.getColumnIndex(_ID_COLUMN);
            int nameColumn = c.getColumnIndex(DISPLAY_NAME_COLUMN);
            while (c.moveToNext()) {
                long id = c.getLong(idColumn);
                String name = c.getString(nameColumn);
                items.add(new CalendarInfo(id, name));
            }
        } finally {
            c.deactivate();
        }
        
        // TODO: hide the spinner completely if the user only has one appropriate calendar.
        
        ArrayAdapter<CalendarInfo> adapter = new ArrayAdapter<CalendarInfo>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCalendarsSpinner.setAdapter(adapter);
    }
    
    private void importCalendar(boolean actuallyInsert) {
        Log.d(TAG, "importCalendar");
        
        int numImported = 0;
        for (ICalendar.Component component : mCalendar.getComponents()) {
            if ("VEVENT".equals(component.getName())) {
                CalendarInfo calInfo = (CalendarInfo) mCalendarsSpinner.getSelectedItem();
                if (insertVEvent(component, calInfo.id, STATUS_CONFIRMED, actuallyInsert)) {
                    ++numImported;
                }
            }
        }
        // TODO: check for success/failure.
    }
    
    private static String extractValue(ICalendar.Component component, String propertyName) {
        ICalendar.Property property = component.getFirstProperty(propertyName);
        return (property != null) ? property.getValue() : null;
    }
    
    private boolean insertVEvent(ICalendar.Component event, long calendarId, int status, boolean actuallyInsert) {
        // TODO: define VEVENT component names as constants in some
        // appropriate class (ICalendar.Component?).
        
        ContentValues values = new ContentValues();
        
        // title
        String title = extractValue(event, "SUMMARY");
        if (TextUtils.isEmpty(title)) {
            if (Config.LOGD) {
                Log.d(TAG, "No SUMMARY provided for event. Cannot import.");
            }
            return false;
        }
        values.put(TITLE, title);
        
        // status
        values.put(STATUS, status);
        
        // description
        String description = extractValue(event, "DESCRIPTION");
        if (!TextUtils.isEmpty(description)) {
            values.put(DESCRIPTION, description);
        }
        
        // where
        String where = extractValue(event, "LOCATION");
        if (!TextUtils.isEmpty(where)) {
            values.put(EVENT_LOCATION, where);
        }
        
        // Calendar ID
        values.put(CALENDAR_ID, calendarId);
        
        // TODO: deal with VALARMs
        
        // dtStart & dtEnd
        Time time = new Time(Time.TIMEZONE_UTC);
        long dtStartMillis = 0;
        long dtEndMillis = 0;
        String dtStart = null;
        String dtEnd = null;
        String duration = null;
        ICalendar.Property dtStartProp = event.getFirstProperty("DTSTART");
        // TODO: handle "floating" timezone (no timezone specified).
        if (dtStartProp != null) {
            dtStart = dtStartProp.getValue();
            if (!TextUtils.isEmpty(dtStart)) {
                ICalendar.Parameter tzIdParam = dtStartProp.getFirstParameter("TZID");
                if (tzIdParam != null && tzIdParam.value != null) {
                    time.clear(tzIdParam.value);
                }
                try {
                    time.parse(dtStart);
                } catch (Exception e) {
                    if (Config.LOGD) {
                        Log.d(TAG, "Cannot parse dtStart " + dtStart, e);
                    }
                    return false;
                }
                if (time.allDay) {
                    values.put(ALL_DAY, 1);
                }
                dtStartMillis = time.toMillis(false /* use isDst */);
                values.put(DTSTART, dtStartMillis);
                values.put(EVENT_TIMEZONE, time.timezone);
            }
            
            ICalendar.Property dtEndProp = event.getFirstProperty("DTEND");
            if (dtEndProp != null) {
                dtEnd = dtEndProp.getValue();
                if (!TextUtils.isEmpty(dtEnd)) {
                    // TODO: make sure the timezones are the same for
                    // start, end.
                    try {
                        time.parse(dtEnd);
                    } catch (Exception e) {
                        if (Config.LOGD) {
                            Log.d(TAG, "Cannot parse dtEnd " + dtEnd, e);
                        }
                        return false;
                    }
                    dtEndMillis = time.toMillis(false /* use isDst */);
                    values.put(DTEND, dtEndMillis);
                }
            } else {
                // look for a duration
                ICalendar.Property durationProp = event.getFirstProperty("DURATION");
                if (durationProp != null) {
                    duration = durationProp.getValue();
                    if (!TextUtils.isEmpty(duration)) {
                        // TODO: check that it is valid?
                        values.put(DURATION, duration);
                    }
                }
            }
        }
        if (TextUtils.isEmpty(dtStart) || (TextUtils.isEmpty(dtEnd) && TextUtils.isEmpty(duration))) {
            if (Config.LOGD) {
                Log.d(TAG, "No DTSTART or DTEND/DURATION defined.");
            }
            return false;
        }
        
        mTextView.append("title: " + title + "\n");
        mTextView.append("description: " + description + "\n");
        mTextView.append("where: " + where + "\n");
        if (values.get(ALL_DAY) != null) {
            mTextView.append("when: " + String.format("%tF", dtStartMillis) + "\n");
            mTextView.append("when: " + DateFormat.getDateInstance().format(dtStartMillis) + "\n");
            mTextView.append("when: " + DateUtils.formatDateTime(this, dtStartMillis, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_UTC) + "\n");
        } else {
            mTextView.append("when: " + String.format("%tF %tR - %tF %tR", dtStartMillis, dtStartMillis, dtEndMillis, dtEndMillis) + "\n");
            mTextView.append("when: " + DateFormat.getDateTimeInstance().format(dtStartMillis) + " - " + DateFormat.getDateTimeInstance().format(dtEndMillis) + "\n");
            mTextView.append("when: " + DateUtils.formatDateRange(this, dtStartMillis, dtEndMillis, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_UTC) + "\n");
        }
        
        // rrule
        /*
        if (!RecurrenceSet.populateContentValues(event, values)) {
            return null;
        }
        */
        
        if (actuallyInsert) {
            return getContentResolver().insert(Uri.parse("content://calendar/events"), values) != null;
        }
        return true;
    }
    
}
