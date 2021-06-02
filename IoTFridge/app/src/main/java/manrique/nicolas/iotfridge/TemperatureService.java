package manrique.nicolas.iotfridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Random;

public class TemperatureService extends Service implements TemperatureGattCallback.Listener {

    public static boolean isRunning = false;
    public static final String STATE_CHANNEL_ID = "StateChannelId";
    public static final String WARNING_CHANNEL_ID = "WarningChannelId";

    private static final String TAG = "TemperatureService";
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotyStateBuilder;
    private NotificationCompat.Builder mNotyWarningBuilder;

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
        if (mNotyStateBuilder == null) {
            mNotyStateBuilder = new NotificationCompat.Builder(this, WARNING_CHANNEL_ID);
            mNotyStateBuilder
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setOngoing(true)
                    .setContentTitle("Title");
        }
        mNotyStateBuilder.setContentText("Temperature : " + String.valueOf(temperature) + " Cº");
        // Sets an ID for the notification, so it can be updated
        final int notifyID = 1;
        mNotificationManager.notify(notifyID, mNotyStateBuilder.build());
    }

    private void notifyWarningTemperature(float temperature) {
        if (mNotyWarningBuilder == null) {
            mNotyWarningBuilder = new NotificationCompat.Builder(this, WARNING_CHANNEL_ID);
            mNotyWarningBuilder
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Warning");
        }
        mNotyWarningBuilder.setContentText("Temperature Broken : " + String.valueOf(temperature) + " Cº");
        // Sets an ID for the notification, so it can be updated
        final int notifyID = 2;
        mNotificationManager.notify(notifyID, mNotyWarningBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceAddress = intent.getStringExtra("deviceAddress");
        Notification notification = new NotificationCompat.Builder(this, WARNING_CHANNEL_ID)
                .setContentText("Loading . . .").build();
        final int notifyID = 1;
        startForeground(notifyID, notification);
        //do heavy work on a background thread
        connectMockup(deviceAddress);
        TemperatureService.isRunning = true;

        return START_NOT_STICKY;
    }


    private void connectMockup(String deviceAdress) {
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
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        //connect to the device found
        BluetoothGattCallback mGattCallback = new TemperatureGattCallback(this);
        device.connectGatt(this, false, mGattCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        TemperatureService.isRunning = false;
    }

    @Override
    public void onConnectionSuccessful() {
        Log.i(TAG, "onConnectionSuccessful ");
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.mBroadcastStringAction);
        broadcastIntent.putExtra("connectionMessage", "Connected Successful");
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onConnectionError(String error) {
        Log.i(TAG, "onConnectionError " + error);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.mBroadcastStringAction);
        broadcastIntent.putExtra("connectionMessage", error);
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onTemperatureChanged(float temperature) {
        Log.i(TAG, "onTemperatureChanged " + temperature);
        if (temperature < 10) {
            notifyWarningTemperature(temperature);
        }
        notifyStateTemperature(temperature);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.mBroadcastIntegerAction);
        broadcastIntent.putExtra("temperature", temperature);
        sendBroadcast(broadcastIntent);
    }


}