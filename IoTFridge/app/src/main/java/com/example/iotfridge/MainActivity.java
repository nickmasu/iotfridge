package com.example.iotfridge;

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
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


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

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            mTvDeviceInfo.setText("Not Bluetooth supported");
            return;
        }


        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            Log.d(TAG, "DEVICE NAME: " + device.getName());

            //String deviceName = "Galaxy Buds+ (FDF8)";
            if (device.getName().equals(DEVICE_NAME))
                mDevice = device;
        }


        if (mDevice == null) {
            mTvDeviceInfo.setText("Device \'" + DEVICE_NAME + "\' Not Found");
            Log.d(TAG, "DEVICE NOT FOUND");
        } else {
            mTvDeviceInfo.setText("Device \'" + DEVICE_NAME + "\' Found)");
            Log.d(TAG, "DEVICE FOUND : " + mDevice.getName());
            connect();
        }

    }

    public void updateDevice(String text) {
        mHandler.post(new Runnable() {
            public void run() {
                mTvDeviceInfo.setText(text);
            }
        });
    }

    public void updateTemperature(String text) {
        mHandler.post(new Runnable() {
            public void run() {
                mTvTemperature.setText(text);
            }
        });
    }

    public void connect() {
        //connect to the given deviceaddress
        mDevice.connectGatt(this, false, mGattCallback);
    }

    //get callbacks when something changes
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {


        // UUIDs for UART service and associated characteristics.
        public final UUID UART_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
        public final UUID TX_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
        public final UUID RX_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

        // UUID for the UART BTLE client characteristic which is necessary for notifications.
        public final UUID USER_UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
        public final UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                updateDevice("Device Connected");
                Log.i(TAG, "Connected to GATT server.");
                gatt.discoverServices();
            } else {
                updateDevice("Device NOT Connected");
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "GATT CALLBACK : onServicesDiscovered");

            if (status != BluetoothGatt.GATT_SUCCESS) {
                updateDevice("ERROR discovering services");
                Log.w(TAG, "ERROR onServicesDiscovered received: " + status);
                return;
            }

            // Save reference to each UART characteristic.
            BluetoothGattService uartService = gatt.getService(UART_UUID);
            BluetoothGattCharacteristic tx = uartService.getCharacteristic(TX_UUID);
            BluetoothGattCharacteristic rx = uartService.getCharacteristic(RX_UUID);

            printAllInfo(uartService, tx, rx);


            // Setup notifications on RX characteristic changes (i.e. data received).
            // First call setCharacteristicNotification to enable notification.

            // Next update the RX characteristic's client descriptor to enable notifications.
            BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
            if (desc == null) {
                // Stop if the RX characteristic has no client descriptor.
                updateDevice("ERROR descriptor not found");
                Log.d(TAG, "ERROR 2: desc");
                return;
            }
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (!gatt.writeDescriptor(desc)) {
                // Stop if the client descriptor could not be written.
                updateDevice("ERROR writting descriptor");
                Log.d(TAG, "ERROR 3: writeDescriptor");
                return;
            }


            // Notify of connection completion.
            updateDevice("Connected to UART service");
            Log.d(TAG, "SUCCESS??");
            gatt.setCharacteristicNotification(rx, true);

        }

        public void printAllInfo(BluetoothGattService uartService, BluetoothGattCharacteristic tx, BluetoothGattCharacteristic rx) {

            Log.d(TAG, "INFO = ServiceUUID" + uartService.getUuid());
            Log.d(TAG, "INFO = tx UUID" + tx.getUuid());
            printAllCharacteristicInfo(tx);
            Log.d(TAG, "INFO = rx UUID" + rx.getUuid());
            printAllCharacteristicInfo(rx);

        }

        public void printAllCharacteristicInfo(BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "\tgetValue " + characteristic.getValue());
            Log.d(TAG, "\tgetStringValue " + characteristic.getStringValue(0));
            Log.d(TAG, "\tgetPermissions " + characteristic.getPermissions());
            Log.d(TAG, "\tgetProperties " + characteristic.getProperties());

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            int format = BluetoothGattCharacteristic.FORMAT_UINT16;
            Log.d(TAG, "GATT CALLBACK : onCharacteristicRead " + characteristic.getUuid() + "::" + characteristic.getValue());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "GATT CALLBACK : onDescriptorWrite");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "GATT CALLBACK : onCharacteristicWrite");
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            updateTemperature(characteristic.getStringValue(0));
            Log.d(TAG, "GATT CALLBACK : onCharacteristicChanged" + characteristic.getStringValue(0));
        }

    };


}