package org.jessies.dalvikexplorer;

import android.hardware.*;
import java.util.*;

/**
 * So, it appears to me that Android is reusing SensorEvents, so
 * that it isn't safe to squirrel them away in mData and look at
 * them later.  The display was totally wonky for me, because I
 * would randomly get things like lux measurements for the barometer,
 * because the SensorEvent was now talking about a different sensor.
 *
 * The solution is to make a copy of the SensorEvent.  I couldn't
 * find a way to copy a SensorEvent, although perhaps there's something
 * obvious I'm overlooking.  So, instead, I made a new class that
 * contains the two things about a SensorEvent that we want to
 * know: the sensor and the values.  Using this copy fixed the
 * problem where events would be of the wrong type when we looked
 * at them.  No more radians for the ambient temperature sensor!
 */
public class FakeSensorEvent {
    public final Sensor sensor;
    public final float[] values;

    public FakeSensorEvent(SensorEvent e) {
        sensor = e.sensor;
        values = new float[e.values.length];
        System.arraycopy(e.values, 0, values, 0, e.values.length);
    }
}
