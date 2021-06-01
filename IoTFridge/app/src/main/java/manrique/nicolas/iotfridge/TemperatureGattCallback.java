package manrique.nicolas.iotfridge;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.UUID;

public class TemperatureGattCallback extends BluetoothGattCallback {


    public interface DeviceInfoCallback {

        void onConnectionSuccessful();

        void onConnectionError(String error);

        void onTemperatureChanged(float temperature);
    }

    private static final String TAG = "TemperatureGattCallback";

    // UUIDs for UART service and associated characteristics.
    public final UUID UART_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public final UUID TX_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public final UUID RX_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    // UUID for the UART BTLE client characteristic which is necessary for notifications.
    public final UUID USER_UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
    public final UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private DeviceInfoCallback mDeviceInfoCallback;

    public TemperatureGattCallback(DeviceInfoCallback deviceInfoCallback) {
        mDeviceInfoCallback = deviceInfoCallback;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            Log.i(TAG, "Device connected");
            gatt.discoverServices();
        } else {
            mDeviceInfoCallback.onConnectionError("Device NOT connected");
            Log.i(TAG, "ERROR Device NOT connected");
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            mDeviceInfoCallback.onConnectionError("ERROR discovering services");
            Log.w(TAG, "ERROR onServicesDiscovered received: " + status);
            return;
        }

        // Save reference to each UART characteristic.
        BluetoothGattService uartService = gatt.getService(UART_UUID);
        BluetoothGattCharacteristic tx = uartService.getCharacteristic(TX_UUID);
        BluetoothGattCharacteristic rx = uartService.getCharacteristic(RX_UUID);

        // Next update the RX characteristic's client descriptor to enable notifications.
        BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
        if (desc == null) {
            // Stop if the RX characteristic has no client descriptor.
            mDeviceInfoCallback.onConnectionError("ERROR descriptor not found");
            Log.d(TAG, "ERROR descriptor not found");
            return;
        }

        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(desc)) {
            // Stop if the client descriptor could not be written.
            mDeviceInfoCallback.onConnectionError("ERROR writting descriptor");
            Log.d(TAG, "ERROR writting descriptor");
            return;
        }

        // Notify of connection completion.
        mDeviceInfoCallback.onConnectionSuccessful();
        gatt.setCharacteristicNotification(rx, true);
        Log.d(TAG, "Connected to UART service");
    }


    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "GATT CALLBACK : onCharacteristicRead " + characteristic.getUuid() + "::" + characteristic.getValue());
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Log.d(TAG, "GATT CALLBACK : onDescriptorWrite");
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "GATT CALLBACK : onCharacteristicWrite");
    }


    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        float temperature = Float.parseFloat(characteristic.getStringValue(0));
        mDeviceInfoCallback.onTemperatureChanged(temperature);
        Log.d(TAG, "GATT CALLBACK : onCharacteristicChanged" + characteristic.getStringValue(0));
    }

}
