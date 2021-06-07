package manrique.nicolas.iotfridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Random;
import java.util.Set;

public class TemperatureService extends Service implements TemperatureGattCallback.Listener {

    public static boolean isRunning = false;
    public static final String STATE_CHANNEL_ID = "StateChannelId";
    public static final String WARNING_CHANNEL_ID = "WarningChannelId";

    public static final String ACTION_CONNECTION_STATUS = "manrique.nicolas.action.connection";
    public static final String ACTION_TEMPERATURE = "manrique.nicolas.action.temperature";

    private static final String TAG = "TemperatureService";
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mStateBuilder;
    private NotificationCompat.Builder mWarningBuilder;
    private BluetoothGatt mConection;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        mNotificationManager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(new NotificationChannel(
                    STATE_CHANNEL_ID,
                    "State Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            ));

            mNotificationManager.createNotificationChannel(new NotificationChannel(
                    WARNING_CHANNEL_ID,
                    "Warning Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            ));
        }
    }


    private void notifyStateTemperature(float temperature) {
        if (mStateBuilder == null) {
            mStateBuilder = new NotificationCompat.Builder(this, WARNING_CHANNEL_ID);
            mStateBuilder
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setOngoing(true)
                    .setContentTitle("Title");
        }
        mStateBuilder.setContentText("Temperature : " + String.valueOf(temperature) + " Cº");
        // Sets an ID for the notification, so it can be updated
        final int notifyID = 1;
        mNotificationManager.notify(notifyID, mStateBuilder.build());
    }

    private void notifyWarningTemperature(float temperature) {
        if (mWarningBuilder == null) {
            mWarningBuilder = new NotificationCompat.Builder(this, WARNING_CHANNEL_ID);
            mWarningBuilder
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Warning");
        }
        mWarningBuilder.setContentText("Temperature Broken : " + String.valueOf(temperature) + " Cº");
        // Sets an ID for the notification, so it can be updated
        final int notifyID = 2;
        mNotificationManager.notify(notifyID, mWarningBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceAddress = intent.getStringExtra("deviceAddress");
        Notification notification = new NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
                .setContentText("Loading . . .").build();
        final int notifyID = 1;
        startForeground(notifyID, notification);
        //do heavy work on a background thread
        // connectMockup(deviceAddress);

        TemperatureService.isRunning = true;
        connect(deviceAddress);
        return START_NOT_STICKY;
    }


    private void connectMockup(String deviceAddress) {
        Log.i(TAG, "In onStartCommand");
        new Thread(new Runnable() {
            public void run() {
                try {
                    Random rand = new Random();
                    while (TemperatureService.isRunning) {
                        onTemperatureChanged(rand.nextFloat() * 40);
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Thread STOPPED");

            }
        }).start();
    }

    private void connect(String deviceAddress) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getAddress().equals(deviceAddress)) {
                //connect to the device found
                BluetoothGattCallback mGattCallback = new TemperatureGattCallback(this);
                mConection = device.connectGatt(this, false, mGattCallback);

                return;
            }
        }
        broadcastActionConnection(false, "Invalid Address");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TemperatureService.isRunning = false;
        Log.i(TAG, "onDestroy");
        if (mConection != null)
            mConection.close();
    }

    @Override
    public void onConnectionSuccessful() {
        Log.i(TAG, "onConnectionSuccessful ");
        broadcastActionConnection(true, "Device UART connected");
    }

    @Override
    public void onConnectionError(String error) {
        Log.i(TAG, "onConnectionError " + error);
        broadcastActionConnection(false, error);
    }

    private void broadcastActionConnection(boolean connected, String message) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_CONNECTION_STATUS);
        broadcastIntent.putExtra("connected", connected);
        broadcastIntent.putExtra("message", message);
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onTemperatureChanged(float temperature) {
        Log.i(TAG, "onTemperatureChanged " + temperature);
        if (temperature > 30) {
            notifyWarningTemperature(temperature);
        }
        notifyStateTemperature(temperature);
        broadcastActionTemperature(temperature);
    }

    private void broadcastActionTemperature(float temperature) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_TEMPERATURE);
        broadcastIntent.putExtra("temperature", temperature);
        sendBroadcast(broadcastIntent);
    }
}