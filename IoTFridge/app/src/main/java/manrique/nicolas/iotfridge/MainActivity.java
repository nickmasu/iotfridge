package manrique.nicolas.iotfridge;

import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;

import manrique.nicolas.iotfridge.R;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    private BluetoothDevice mDevice;
    private String TAG = "MainActivity";
    private String DEVICE_NAME = "Adafruit Bluefruit LE";

    private BluetoothAdapter mBluetoothAdapter;
    private TextView mTvDeviceInfo;
    private TextView mTvTemperature;
    private SwitchCompat mScConnection;
    private Handler mHandler;
    private IntentFilter mIntentFilter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(TemperatureService.ACTION_CONNECTION_STATUS);
        mIntentFilter.addAction(TemperatureService.ACTION_TEMPERATURE);

        mHandler = new Handler(); // Initialize the Handler from the Main Thread

        mTvDeviceInfo = (TextView) findViewById(R.id.tvDeviceInfo);
        mTvTemperature = (TextView) findViewById(R.id.tvTemperature);
        mScConnection = (SwitchCompat) findViewById(R.id.scConnection);

        mScConnection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    startService();
                else
                    stopService();
            }
        });

        mDevice = finDevice();
        mScConnection.setChecked(TemperatureService.isRunning);
        mScConnection.setEnabled(mDevice != null); // allowed using a real device

        if (mDevice == null) {
            mTvDeviceInfo.setText("Device \'" + DEVICE_NAME + "\' Not Found");
            Log.d(TAG, "DEVICE NOT FOUND");
        } else {
            mTvDeviceInfo.setText("Device \'" + DEVICE_NAME + "\' Found)");
            Log.d(TAG, "DEVICE FOUND : " + mDevice.getAddress());
        }
    }

    private BluetoothDevice finDevice() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            mTvDeviceInfo.setText("Not Bluetooth supported");
            return null;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            Log.d(TAG, "DEVICE NAME: " + device.getName());
            //String deviceName = "Galaxy Buds+ (FDF8)";
            if (device.getName().equals(DEVICE_NAME))
                return device;
        }

        return null;
    }

    public void startService() {
        if (TemperatureService.isRunning)
            return;

        if (mDevice == null)
            return;

        mScConnection.setEnabled(false);
        Intent serviceIntent = new Intent(this, TemperatureService.class);

        String address = mDevice.getAddress();

        serviceIntent.putExtra("deviceAddress", address);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        if (!TemperatureService.isRunning)
            return;
        Intent serviceIntent = new Intent(this, TemperatureService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mScConnection.setEnabled(true);
            if (intent.getAction().equals(TemperatureService.ACTION_CONNECTION_STATUS)) {
                mTvDeviceInfo.setText(intent.getStringExtra("message") + "\n\n");
                if (!intent.getBooleanExtra("connected", false)) {
                    stopService();
                    mScConnection.setChecked(false);
                }
            } else if (intent.getAction().equals(TemperatureService.ACTION_TEMPERATURE)) {
                mTvTemperature.setText(intent.getFloatExtra("temperature", 0) + "\n\n");

            }
        }
    };

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiver);
        super.onPause();
    }

}