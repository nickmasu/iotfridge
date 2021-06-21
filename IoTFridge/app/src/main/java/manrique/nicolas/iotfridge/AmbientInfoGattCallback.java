package manrique.nicolas.iotfridge;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.UUID;

public class AmbientInfoGattCallback extends BluetoothGattCallback {

    /**
     * Interface to listen events of connections and ambient changes
     */
    public interface Listener {

        void onConnectionSuccessful();

        void onConnectionError(String error);

        void onAmbientChanged(float temperature, float humidity, float battery);

    }

    // --------------------------------------------------------------------------------------------

    private static final String TAG = "AmbientInfoGattCallback";

    // UUIDs for UART service and associated characteristics.
    private static final UUID UART_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID TX_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID RX_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    // UUID for the UART BTLE client characteristic which is necessary for notifications.
    private static final UUID USER_UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Listener mListener;

    public AmbientInfoGattCallback(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            gatt.discoverServices();
            Log.i(TAG, "Device connected");
        } else {
            mListener.onConnectionError("Device NOT connected");
            Log.i(TAG, "Device NOT connected");
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            mListener.onConnectionError("ERROR discovering services");
            Log.w(TAG, "ERROR discovering services: " + status);
            return;
        }

        // Save reference to each UART characteristic.
        BluetoothGattService uartService = gatt.getService(UART_UUID);
        BluetoothGattCharacteristic tx = uartService.getCharacteristic(TX_UUID);
        BluetoothGattCharacteristic rx = uartService.getCharacteristic(RX_UUID);

        // Next update the RX characteristic's client descriptor to enable notifications.
        BluetoothGattDescriptor desc = rx.getDescriptor(CLIENT_UUID);
        if (desc == null) {
            mListener.onConnectionError("ERROR descriptor not found");
            Log.d(TAG, "ERROR descriptor not found");
            return;
        }

        desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if (!gatt.writeDescriptor(desc)) {
            mListener.onConnectionError("ERROR writing descriptor");
            Log.d(TAG, "ERROR writing descriptor");
            return;
        }

        // Notify of connection completion.
        gatt.setCharacteristicNotification(rx, true);
        mListener.onConnectionSuccessful();
        Log.d(TAG, "Connected to UART service");
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        float temperature = Float.parseFloat(characteristic.getStringValue(0));
        mListener.onAmbientChanged(temperature, temperature + 100, temperature % 100);
        Log.d(TAG, "GATT CALLBACK : onCharacteristicChanged" + temperature);
    }

}
