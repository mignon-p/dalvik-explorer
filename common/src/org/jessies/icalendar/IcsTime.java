package org.jessies.icalendar;

public class IcsTime {
    public final long utcMillis;
    public final boolean allDay;
    
    public IcsTime(long utcMillis, boolean allDay) {
        this.utcMillis = utcMillis;
        this.allDay = allDay;
    }
}
