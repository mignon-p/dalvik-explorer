package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.hardware.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import java.util.*;

public class SensorsActivity extends BetterListActivity implements SensorEventListener {
  private final HashMap<Sensor, SensorEvent> mData = new HashMap<Sensor, SensorEvent>();

  @Override protected void onResume() {
    super.onResume();
    SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    final List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
    for (Sensor sensor : sensors) {
      sensorManager.registerListener(this, sensor, 1000);
    }
  }

  @Override protected void onPause() {
    super.onPause();
    SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    final List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
    sensorManager.unregisterListener(this);
  }

  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    System.err.println(sensor + " " + accuracy);
  }

  public void onSensorChanged(SensorEvent event) {
    mData.put(event.sensor, event);
    getListView().invalidateViews();
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final List<SensorItem> sensors = gatherSensors();
    setListAdapter(new BetterArrayAdapter<SensorItem>(this, sensors, SensorItem.class, "toSubtitle"));
    setTitle("Sensors (" + sensors.size() + ")");
  }

  @Override protected void onListItemClick(ListView l, View v, int position, long id) {
/*    final LocaleListItem item = (LocaleListItem) l.getAdapter().getItem(position);
    String languageName = item.locale().toString();
    final Intent intent;
    if (languageName.contains("_")) {
      intent = new Intent(this, LocaleActivity.class);
      final String localeName = languageName.replace(" (default)", "");
      intent.putExtra("org.jessies.dalvikexplorer.Locale", localeName);
    } else {
      intent = new Intent(this, LocaleCountriesActivity.class);
      intent.putExtra("org.jessies.dalvikexplorer.Language", languageName);
    }
    startActivity(intent);*/
  }

  private List<SensorItem> gatherSensors() {
    SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    final List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
    final ArrayList<SensorItem> result = new ArrayList<SensorItem>(sensors.size());
    for (Sensor sensor : sensors) {
      if (sensor.getType() == Sensor.TYPE_ORIENTATION || sensor.getType() == Sensor.TYPE_TEMPERATURE) {
        continue; // Ignore the two deprecated types.
      }
      result.add(new SensorItem(mData, sensor));
    }
    return result;
  }
}
