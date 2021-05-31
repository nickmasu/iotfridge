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
import android.content.Intent;
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


    private BluetoothDevice mDevice;
    private String TAG = "MainActivity";
    private String DEVICE_NAME = "Adafruit Bluefruit LE";

    private BluetoothAdapter mBluetoothAdapter;
    private TextView mTvDeviceInfo;
    private TextView mTvTemperature;
    private Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            startService();
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
        Intent serviceIntent = new Intent(this, TemperatureService.class);
        serviceIntent.putExtra("inputExtra", mDevice.getAddress());
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, TemperatureService.class);
        stopService(serviceIntent);
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