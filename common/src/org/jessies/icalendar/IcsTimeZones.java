package org.jessies.icalendar;

import java.util.*;
import java.util.regex.*;
import org.jessies.test.*;

/**
 * Learns about all the time zones represented in a .ics file, and parses RFC 5545 DATE or DATE-TIME values appropriately.
 */
public class IcsTimeZones {
    // The custom time zones defined by our .ics file.
    private final Map<String, TimeZone> mTimeZones = new HashMap<String, TimeZone>();
    
    /**
     * Constructs a new IcsTimeZones from the given 'calendar'.
     */
    public IcsTimeZones(ICalendar.Component calendar) {
        if (calendar.getComponents() != null) {
            parseTimeZones(calendar);
        }
    }
    
    /**
     * Parses all the VTIMEZONE components in the given 'calendar'.
     */
    private void parseTimeZones(ICalendar.Component calendar) {
        for (ICalendar.Component component : calendar.getComponents()) {
            if (component.getName().equals(ICalendar.Component.VTIMEZONE)) {
                parseTimeZone(component);
            }
        }
    }
    
    /**
     * Translates the given VTIMEZONE component 'timeZone' into an entry in the TimeZone map.
     */
    private void parseTimeZone(ICalendar.Component timeZone) {
        String name = IcsUtils.getFirstPropertyText(timeZone, "TZID");
        int standardDay = 0, standardDayOfWeek = 0, standardMonth = 0, daylightDay = 0, daylightDayOfWeek = 0, daylightMonth = 0;
        int standardOffset = 0, daylightOffset = 0;
        for (ICalendar.Component variant : timeZone.getComponents()) {
            // RRULE:FREQ=YEARLY;BYMINUTE=0;BYHOUR=2;BYDAY=1SU;BYMONTH=11
            boolean standard = variant.getName().equals("STANDARD");
            ICalendar.Property rule = variant.getFirstProperty("RRULE");
            if (rule == null) {
                throw new IllegalArgumentException(name + ", variant " + variant.getName() + " has no RRULE!");
            }
            for (String param : rule.getValue().split(";")) {
                String[] keyValue = param.split("=");
                String key = keyValue[0];
                String value = keyValue[1];
                if (key.equals("FREQ")) {
                    if (!value.equals("YEARLY")) {
                        throw new IllegalArgumentException(rule.getValue() + ": FREQ not YEARLY!");
                    }
                } else if (key.equals("BYDAY")) {
                    Matcher m = Pattern.compile("(-?\\d+)(..)").matcher(value);
                    if (!m.matches()) {
                        throw new IllegalArgumentException(rule.getValue() + ": can't parse BYDAY!");
                    }
                    if (standard) {
                        standardDay = Integer.parseInt(m.group(1));
                        standardDayOfWeek = IcsUtils.parseDayName(m.group(2));
                    } else {
                        daylightDay = Integer.parseInt(m.group(1));
                        daylightDayOfWeek = IcsUtils.parseDayName(m.group(2));
                    }
                } else if (key.equals("BYMONTH")) {
                    if (standard) {
                        standardMonth = Integer.parseInt(value) - 1;
                    } else {
                        daylightMonth = Integer.parseInt(value) - 1;
                    }
                } else {
                    System.err.println("warning: ignoring key " + key + " (value " + value + ") for " + name + ", variant " + variant.getName());
                }
            }
            
            ICalendar.Property offset = variant.getFirstProperty("TZOFFSETTO");
            if (offset != null && offset.getValue() != null) {
                int rawOffset = IcsUtils.parseUtcOffset(offset.getValue());
                if (standard) {
                    standardOffset = rawOffset;
                } else {
                    daylightOffset = rawOffset;
                }
            } else {
                System.err.println("didn't understand TZOFFSETTO in '" + name + "' variant '" + variant.getName() + "'");
            }
        }
        SimpleTimeZone tz = new SimpleTimeZone(standardOffset, name, daylightMonth, daylightDay, daylightDayOfWeek, 7200000, standardMonth, standardDay, standardDayOfWeek, 7200000, Math.abs(daylightOffset - standardOffset));
        mTimeZones.put(name, tz);
        System.err.println("added '" + name + "' as " + tz);
    }
    
    public IcsTime parseTimeProperty(ICalendar.Component event, String propertyName) {
        final ICalendar.Property property = event.getFirstProperty(propertyName);
        if (property == null) {
            return null;
        }
        
        final String value = property.getValue();
        if (value == null || value.length() == 0) {
            return null;
        }
        
        final ICalendar.Parameter tzIdParam = property.getFirstParameter("TZID");
        // FIXME: shouldn't ICalendar automatically remove the quotes for us?
        final String tzId = (tzIdParam != null && tzIdParam.value != null) ? tzIdParam.value.replaceFirst("^\"(.*)\"$", "$1") : null;
        final TimeZone tz = (tzId != null) ? mTimeZones.get(tzId) : TimeZone.getTimeZone("GMT");
        System.err.println("TZID=" + tzId);
        System.err.println("TZ=" + tz);
        
        final Calendar calendar = Calendar.getInstance(tz);
        return parseDateTime(calendar, value);
    }
    
    private static IcsTime parseDateTime(Calendar calendar, String dateTime) {
        if (dateTime.length() == 8) {
            // Just a date.
            Matcher m = Pattern.compile("(....)(..)(..)").matcher(dateTime);
            if (!m.matches()) {
                throw new IllegalArgumentException(dateTime);
            }
            final int year = Integer.parseInt(m.group(1));
            final int month = Integer.parseInt(m.group(2)) - 1;
            final int day = Integer.parseInt(m.group(3));
            calendar.clear();
            calendar.set(year, month, day);
            System.err.println(dateTime + " -> " + calendar);
            return new IcsTime(calendar.getTimeInMillis(), true);
        }
        
        // TODO: if we see Z, check that the calendar's time zone is GMT.
        Matcher m = Pattern.compile("(....)(..)(..)T(..)(..)(..)Z?").matcher(dateTime);
        if (!m.matches()) {
            throw new IllegalArgumentException(dateTime);
        }
        final int year = Integer.parseInt(m.group(1));
        final int month = Integer.parseInt(m.group(2)) - 1;
        final int day = Integer.parseInt(m.group(3));
        final int hour = Integer.parseInt(m.group(4));
        final int minute = Integer.parseInt(m.group(5));
        final int second = Integer.parseInt(m.group(6));
        calendar.clear();
        calendar.set(year, month, day, hour, minute, second);
        System.err.println(dateTime + " -> " + calendar);
        return new IcsTime(calendar.getTimeInMillis(), false);
    }
    
    @Test private static void testParseDateTime() {
        // DATE
        // 19700101
        Assert.equals(0L, parseDateTime(Calendar.getInstance(TimeZone.getTimeZone("GMT")), "19700101").utcMillis);
        Assert.equals(true, parseDateTime(Calendar.getInstance(TimeZone.getTimeZone("GMT")), "19700101").allDay);
        // 19970714
        Assert.equals(868838400000L, parseDateTime(Calendar.getInstance(TimeZone.getTimeZone("GMT")), "19970714").utcMillis);
        // 19980704
        Assert.equals(899510400000L, parseDateTime(Calendar.getInstance(TimeZone.getTimeZone("GMT")), "19980704").utcMillis);
        
        // DATE-TIME
        // 19960401T150000Z
        Assert.equals(828370800000L, parseDateTime(Calendar.getInstance(TimeZone.getTimeZone("GMT")), "19960401T150000Z").utcMillis);
        Assert.equals(false, parseDateTime(Calendar.getInstance(TimeZone.getTimeZone("GMT")), "19960401T150000Z").allDay);
        
        // DTSTART
        // 19980119T070000Z
        // TZID=America/New_York:19980119T020000
        long t1 = parseDateTime(Calendar.getInstance(TimeZone.getTimeZone("GMT")), "19980119T070000Z").utcMillis;
        long t2 = parseDateTime(Calendar.getInstance(TimeZone.getTimeZone("America/New_York")), "19980119T020000").utcMillis;
        Assert.equals(t1, t2);
        Assert.equals(885193200000L, t1);
        
        // Example:  The following represents July 14, 1997, at 1:30 PM in New
        // York City in each of the three time formats, using the "DTSTART"
        // property.
        // DTSTART:19970714T133000                   ; Local time
        // DTSTART:19970714T173000Z                  ; UTC time
        // DTSTART;TZID=America/New_York:19970714T133000
        //                                           ; Local time and time
        //                                           ; zone reference
        long t3 = parseDateTime(Calendar.getInstance(TimeZone.getTimeZone("America/New_York")), "19970714T133000").utcMillis;
        long t4 = parseDateTime(Calendar.getInstance(TimeZone.getTimeZone("GMT")), "19970714T173000Z").utcMillis;
        Assert.equals(t3, t4);
        Assert.equals(868901400000L, t3);
    }
}
