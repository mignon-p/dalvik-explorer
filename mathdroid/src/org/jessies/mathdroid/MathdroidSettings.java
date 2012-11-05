package org.jessies.mathdroid;

import android.content.*;
import android.os.Bundle;
import android.preference.*;

public class MathdroidSettings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
    updateSummaries();

    Preference runTestsButton = findPreference("runTestsButton");
    runTestsButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      @Override public boolean onPreferenceClick(Preference arg0) {
        startActivity(new Intent(MathdroidSettings.this, MathdroidTests.class));
        return true;
      }
    });
  }

  @Override protected void onResume() {
    super.onResume();
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override protected void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    updateSummaries();
  }

  private void updateSummaries() {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    String angleMode = settings.getString("angleMode", "Radians");
    findPreference("angleMode").setSummary(angleMode);
  }
}
