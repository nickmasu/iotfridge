package manrique.nicolas.iotfridge.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import manrique.nicolas.iotfridge.R;


public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}