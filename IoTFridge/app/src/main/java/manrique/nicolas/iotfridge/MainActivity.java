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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_FRAGMENT_DEVICE_INFO = "TAG_FRAGMENT_DEVICE_INFO";
    private static final String TAG_FRAGMENT_ACTIVE_AMBIENT = "TAG_FRAGMENT_ACTIVE_AMBIENT_INFO";
    private static final String TAG = "MainActivity";
    private BluetoothAdapter mBluetoothAdapter;

    private TextView mTvBackground;
    private BluetoothDevice mDevice;
    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        if (AmbientInfoService.isRunning)
            mDevice = AmbientInfoService.connectedDevice;

        mTvBackground = findViewById(R.id.tvBackground);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (AmbientInfoService.isRunning) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container_view, ActiveAmbientFragment.class, null, TAG_FRAGMENT_ACTIVE_AMBIENT)
                    .commit();
        } else if (mBluetoothAdapter == null)
            mTvBackground.setText("Bluetooth Not Supported");

        else if (mBluetoothAdapter.isEnabled())
            askToConnectDevice();
        else
            askToConnectBluetooth();


        // listener


        getSupportFragmentManager().setFragmentResultListener("requestKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                // Connect the service Start the other fragment
                if (AmbientInfoService.isRunning)
                    return;

                if (mDevice == null)
                    return;

                Intent serviceIntent = new Intent(mContext, AmbientInfoService.class);
                serviceIntent.putExtra("device", mDevice);
                mContext.startForegroundService(serviceIntent);

                Bundle bundle = new Bundle();
                bundle.putParcelable("device", mDevice);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_view, ActiveAmbientFragment.class, bundle, TAG_FRAGMENT_ACTIVE_AMBIENT)
                        .commit();
            }
        });


        getSupportFragmentManager().setFragmentResultListener("onDisconnectKey", this, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                // Connect the service Start the other fragment
                if (!AmbientInfoService.isRunning)
                    return;
                Intent serviceIntent = new Intent(mContext, AmbientInfoService.class);
                stopService(serviceIntent);

                Bundle bundle = new Bundle();
                bundle.putParcelable("device", mDevice);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container_view, ConnectDeviceFragment.class, bundle, TAG_FRAGMENT_DEVICE_INFO)
                        .commit();
            }
        });


    }

    ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {

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

    ActivityResultLauncher<IntentSenderRequest> selectDeviceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "You must to pair a device to use this application.", Toast.LENGTH_SHORT).show();
                    askToConnectDevice();
                } else {
                    BluetoothDevice deviceToPair = result.getData().getParcelableExtra(
                            CompanionDeviceManager.EXTRA_DEVICE);

                    if (deviceToPair == null) {
                        Toast.makeText(this, "Device wans't connected correctly.", Toast.LENGTH_SHORT).show();
                        askToConnectDevice();
                    } else {
                        onDeviceConnected(deviceToPair);

                    }
                }
            });

    private void askToConnectDevice() {
        // clean message
        mTvBackground.setText("");

        CompanionDeviceManager deviceManager = (CompanionDeviceManager) getSystemService(
                Context.COMPANION_DEVICE_SERVICE
        );

        // To skip filtering based on name and supported feature flags,
        // don't include calls to setNamePattern() and addServiceUuid(),
        // respectively. This example uses Bluetooth.
        BluetoothDeviceFilter deviceFilter =
                new BluetoothDeviceFilter.Builder()
                        //.setNamePattern(Pattern.compile("My device"))
                        //.addServiceUuid(new ParcelUuid(new UUID(0x123abcL, -1L)), null)
                        .build();


        AssociationRequest pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .build();

        // When the app tries to pair with the Bluetooth device, show the
        // appropriate pairing request dialog to the user.
        deviceManager.associate(pairingRequest,
                new CompanionDeviceManager.Callback() {
                    @Override
                    public void onDeviceFound(IntentSender chooserLauncher) {
                        IntentSenderRequest request = new IntentSenderRequest.Builder(chooserLauncher).build();
                        selectDeviceLauncher.launch(request);
                    }

                    @Override
                    public void onFailure(CharSequence error) {
                        mTvBackground.setText("Something went wrong selecting devices.");
                    }
                }, null);
    }


    private void onDeviceConnected(BluetoothDevice device) {
        mDevice = device;
        Bundle bundle = new Bundle();
        bundle.putParcelable("device", device);

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view, ConnectDeviceFragment.class, bundle, TAG_FRAGMENT_DEVICE_INFO)
                .commit();
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(TAG_FRAGMENT_DEVICE_INFO);
        if (fragment != null)
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();

        askToConnectDevice();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("device", mDevice);
        //
        super.onSaveInstanceState(outState);
    }
}