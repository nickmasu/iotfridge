package manrique.nicolas.iotfridge;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_view, SettingsFragment.class, null)
                .commit();
    }

}