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
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import manrique.nicolas.iotfridge.R;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements TemperatureGattCallback.DeviceInfoCallback {

    public static final String mBroadcastStringAction = "com.truiton.broadcast.string";
    public static final String mBroadcastIntegerAction = "com.truiton.broadcast.integer";
    public static final String mBroadcastArrayListAction = "com.truiton.broadcast.arraylist";

    private BluetoothDevice mDevice;
    private String TAG = "MainActivity";
    private String DEVICE_NAME = "Adafruit Bluefruit LE";

    private BluetoothAdapter mBluetoothAdapter;
    private TextView mTvDeviceInfo;
    private TextView mTvTemperature;
    private Handler mHandler;
    private IntentFilter mIntentFilter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(mBroadcastStringAction);
        mIntentFilter.addAction(mBroadcastIntegerAction);
        mIntentFilter.addAction(mBroadcastArrayListAction);

        mHandler = new Handler(); // Initialize the Handler from the Main Thread

        mTvDeviceInfo = (TextView) findViewById(R.id.tvDeviceInfo);
        mTvTemperature = (TextView) findViewById(R.id.tvTemperature);

        mDevice = finDevice();
        if (mDevice == null) {
            mTvDeviceInfo.setText("Device \'" + DEVICE_NAME + "\' Not Found");
            Log.d(TAG, "DEVICE NOT FOUND");
        } else {
            mTvDeviceInfo.setText("Device \'" + DEVICE_NAME + "\' Found)");
            Log.d(TAG, "DEVICE FOUND : " + mDevice.getName());
        }
        startService();

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
        Intent serviceIntent = new Intent(this, TemperatureService.class);
        String adress = "Hola";
        if (mDevice != null) {
            adress = mDevice.getAddress();
        }
        serviceIntent.putExtra("inputExtra", adress);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
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
            mTvDeviceInfo.setText("Broadcast From Service: \n");
            if (intent.getAction().equals(mBroadcastStringAction)) {
                mTvDeviceInfo.setText(intent.getStringExtra("Data") + "\n\n");
            } else if (intent.getAction().equals(mBroadcastIntegerAction)) {
                mTvDeviceInfo.setText(intent.getIntExtra("Data", 0) + "\n\n");
            
                Intent stopIntent = new Intent(MainActivity.this,
                        TemperatureService.class);
                stopService(stopIntent);
            }
        }
    };

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiver);
        super.onPause();
    }


    @Override
    public void onDeviceStateChanged(String state) {
        mHandler.post(new Runnable() {
            public void run() {
                mTvDeviceInfo.setText(state);
            }
        });
    }

    @Override
    public void onTemperatureChanged(float temperature) {
        mHandler.post(new Runnable() {
            public void run() {
                mTvTemperature.setText(String.valueOf(temperature));
            }
        });
    }
}