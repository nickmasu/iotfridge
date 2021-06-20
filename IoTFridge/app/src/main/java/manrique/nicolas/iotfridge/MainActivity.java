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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_DEVICE_REQUEST_CODE = 23;
    private String TAG = "MainActivity";
    private BluetoothAdapter mBluetoothAdapter;


    private TextView mTvBackground;
    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_CANCELED)
                    mTvBackground.setText("You must enable Blueooth to use this application.");
                else
                    askForDevice();

            });


    ActivityResultLauncher<IntentSenderRequest> selectDeviceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_CANCELED)
                    mTvBackground.setText("You must to pair a device to use this application.");
                else {
                    Intent data = result.getData();
                    BluetoothDevice deviceToPair = data.getParcelableExtra(
                            CompanionDeviceManager.EXTRA_DEVICE
                    );

                    if (deviceToPair != null) {
                        mTvBackground.setText("Connected to " + deviceToPair.getName());
                    } else {
                        mTvBackground.setText("Device wans't connected correctly.");
                    }
                }

            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvBackground = findViewById(R.id.tvBackground);


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            mTvBackground.setText("Bluetooth Not Supported");
            return;
        }


        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            askForDevice();
        }


        /*if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragment_container_view, SelectDeviceFragment.class, null)
                    .commit();
        }*/

    }


    private void askForDevice() {
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

        // The argument provided in setSingleDevice() determines whether a single
        // device name or a list of device names is presented to the user as
        // pairing options.
        AssociationRequest pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                //.setSingleDevice(true)
                .build();

        // When the app tries to pair with the Bluetooth device, show the
        // appropriate pairing request dialog to the user.
        deviceManager.associate(pairingRequest,
                new CompanionDeviceManager.Callback() {
                    @Override
                    public void onDeviceFound(IntentSender chooserLauncher) {
                        IntentSenderRequest request = new IntentSenderRequest.Builder(chooserLauncher).build();
                        selectDeviceLauncher.launch(request);
                        //startIntentSenderForResult(chooserLauncher,SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0);
                    }

                    @Override
                    public void onFailure(CharSequence error) {
                        // handle failure to find the companion device
                    }
                }, null);
    }


}