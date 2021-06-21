package manrique.nicolas.iotfridge.settings;

import androidx.appcompat.app.AppCompatActivity;

import manrique.nicolas.iotfridge.R;

import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_view, SettingsFragment.class, null)
                .commit();
    }

}