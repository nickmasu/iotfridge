package manrique.nicolas.iotfridge;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String TAG_FRAGMENT_DEVICE_INFO = "TAG_FRAGMENT_DEVICE_INFO";
    private static final String TAG_FRAGMENT_ACTIVE_AMBIENT = "TAG_FRAGMENT_ACTIVE_AMBIENT_INFO";

    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";

    public static final String REQUEST_START_AMBIENT_SERVICE = "REQUEST_START_AMBIENT_SERVICE";
    public static final String REQUEST_CLOSE_AMBIENT_SERVICE = "REQUEST_CLOSE_AMBIENT_SERVICE";
    public static final String REQUEST_FORGET_DEVICE = "REQUEST_FORGET_DEVICE";

    public static final String PREFERENCES_DEVICE_ADDRESS = "PREFERENCES_DEVICE";


    private TextView mTvBackground;
    private BluetoothDevice mDevice;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mContext = this;
        mTvBackground = findViewById(R.id.tvBackground);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (AmbientInfoService.isRunning) {
            mDevice = AmbientInfoService.deviceConnected;
            setAmbientInfoFragment();

        } else if (mBluetoothAdapter == null) {
            mTvBackground.setText("Bluetooth Not Supported");

        } else if (mBluetoothAdapter.isEnabled()) {
            askToConnectDevice();

        } else {
            askToConnectBluetooth();
        }

        // listeners
        listenRequestOpenAmbientService();
        listenRequestCloseAmbientService();
        listenRequestForgetDevice();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
    }

    private void listenRequestOpenAmbientService() {
        getSupportFragmentManager()
                .setFragmentResultListener(REQUEST_START_AMBIENT_SERVICE, this, new FragmentResultListener() {

                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        if (AmbientInfoService.isRunning || mDevice == null)
                            return;

                        Intent serviceIntent = new Intent(mContext, AmbientInfoService.class);
                        serviceIntent.putExtra(EXTRA_DEVICE, mDevice);
                        mContext.startForegroundService(serviceIntent);
                        setAmbientInfoFragment();
                    }
                });
    }

    private void listenRequestCloseAmbientService() {
        getSupportFragmentManager().
                setFragmentResultListener(REQUEST_CLOSE_AMBIENT_SERVICE, this, new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        if (!AmbientInfoService.isRunning)
                            return;

                        Intent serviceIntent = new Intent(mContext, AmbientInfoService.class);
                        stopService(serviceIntent);
                        setDeviceFragment();
                    }
                });
    }


    private void listenRequestForgetDevice() {
        getSupportFragmentManager().
                setFragmentResultListener(REQUEST_FORGET_DEVICE, this, new FragmentResultListener() {
                    @Override
                    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_DEVICE_INFO);
                        if (fragment != null)
                            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                        mDevice = null;
                        setPreferencesDevice(null);
                        askToConnectDevice();
                    }
                });
    }

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {

                if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "You must enable Bluetooth to use this application.", Toast.LENGTH_SHORT).show();
                    askToConnectBluetooth();
                } else
                    askToConnectDevice();

            });


    private void askToConnectBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        enableBluetoothLauncher.launch(enableBtIntent);
    }

    private ActivityResultLauncher<IntentSenderRequest> connectDeviceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(), result -> {

                if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "You must to pair a device to use this application.", Toast.LENGTH_SHORT).show();
                    askToConnectDevice();

                } else {
                    mDevice = result.getData().getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE);

                    if (mDevice == null) {
                        Toast.makeText(this, "Device wans't connected correctly.", Toast.LENGTH_SHORT).show();
                        askToConnectDevice();
                    } else {
                        setPreferencesDevice(mDevice);
                        setDeviceFragment();
                    }
                }
            });

    private void askToConnectDevice() {
        if (mDevice == null)
            mDevice = getPreferencesDevice();

        if (mDevice != null) {
            setDeviceFragment();
            return;
        }

        CompanionDeviceManager deviceManager = (CompanionDeviceManager) getSystemService(
                Context.COMPANION_DEVICE_SERVICE
        );

        BluetoothDeviceFilter deviceFilter = new BluetoothDeviceFilter.Builder()
                //.setNamePattern(Pattern.compile("My device"))
                //.addServiceUuid(new ParcelUuid(new UUID(0x123abcL, -1L)), null)
                .build();

        AssociationRequest pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .build();

        deviceManager.associate(pairingRequest, new CompanionDeviceManager.Callback() {

            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                IntentSenderRequest request = new IntentSenderRequest.Builder(chooserLauncher).build();
                connectDeviceLauncher.launch(request);
            }

            @Override
            public void onFailure(CharSequence error) {
                mTvBackground.setText("Something went wrong selecting devices.");
            }
        }, null);
    }


    private BluetoothDevice getPreferencesDevice() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String deviceAddress = preferences.getString(PREFERENCES_DEVICE_ADDRESS, null);
        try {
            return mBluetoothAdapter.getRemoteDevice(deviceAddress);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void setPreferencesDevice(BluetoothDevice device) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (device == null)
            editor.putString(PREFERENCES_DEVICE_ADDRESS, "");
        else
            editor.putString(PREFERENCES_DEVICE_ADDRESS, device.getAddress());
        editor.apply();
    }


    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_DEVICE_INFO);
        if (fragment != null)
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();

        // askToConnectDevice();
    }

    private void setAmbientInfoFragment() {
        mTvBackground.setText("");
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_DEVICE, mDevice);

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container_view, AmbientInfoFragment.class, bundle, TAG_FRAGMENT_ACTIVE_AMBIENT)
                .commit();
    }

    private void setDeviceFragment() {
        mTvBackground.setText("");
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_DEVICE, mDevice);

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container_view, DeviceFragment.class, bundle, TAG_FRAGMENT_DEVICE_INFO)
                .commit();
    }

}