package manrique.nicolas.iotfridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
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

public class AmbientInfoService extends Service implements AmbientInfoGattCallback.Listener {

    public static boolean isRunning = false;
    public static BluetoothDevice deviceConnected = null;

    /* --------------------------------------------------------------------- */

    private static final String TAG = "AmbientInfoService";

    public static final String CHANNEL_AMBIENT_ID = "CHANNEL_AMBIENT_ID";
    public static final String CHANNEL_WARNING_ID = "CHANNEL_WARNING_ID";

    public static final String ACTION_CONNECTION_STATUS = "manrique.nicolas.action.connection";
    public static final String ACTION_AMBIENT_INFO = "manrique.nicolas.action.temperature";

    public static final String EXTRA_CONNECTION_STATE = "EXTRA_CONNECTION_STATE";
    public static final String EXTRA_CONNECTION_MESSAGE = "EXTRA_CONNECTION_MESSAGE";

    public static final String EXTRA_AMBIENT_TEMPERATURE = "EXTRA_AMBIENT_TEMPERATURE";
    public static final String EXTRA_AMBIENT_HUMIDITY = "EXTRA_AMBIENT_HUMIDITY";
    public static final String EXTRA_AMBIENT_BATTERY = "EXTRA_AMBIENT_BATTERY";

    /* --------------------------------------------------------------------- */

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotifyAmbientBuilder;
    private NotificationCompat.Builder mNotifyWarningBuilder;
    private BluetoothGatt mGattConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        mNotificationManager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_AMBIENT_ID,
                    "Ambient Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            ));

            mNotificationManager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_WARNING_ID,
                    "Warning Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            ));
        }
    }


    private void notifyAmbient(float temperature, float humidity, float battery) {
        if (mNotifyAmbientBuilder == null) {
            mNotifyAmbientBuilder = new NotificationCompat.Builder(this, CHANNEL_WARNING_ID);
            mNotifyAmbientBuilder
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setOngoing(true);
        }

        mNotifyAmbientBuilder.setContentText(String.valueOf(temperature) + " Cº  -  "
                + String.valueOf(humidity) + " H  -  "
                + String.valueOf(battery) + " %  Charge");
        // Sets an ID for the notification, so it can be updated
        final int notifyID = 1;
        mNotificationManager.notify(notifyID, mNotifyAmbientBuilder.build());
    }

    private void notifyWarning(String message) {
        if (mNotifyWarningBuilder == null) {
            mNotifyWarningBuilder = new NotificationCompat.Builder(this, CHANNEL_WARNING_ID);
            mNotifyWarningBuilder
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Warning");
        }
        mNotifyWarningBuilder.setContentText(message);
        // Sets an ID for the notification, so it can be updated
        final int notifyID = 2;
        mNotificationManager.notify(notifyID, mNotifyWarningBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        BluetoothDevice device = intent.getParcelableExtra(MainActivity.EXTRA_DEVICE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_WARNING_ID)
                .setContentText("").build();
        final int notifyID = 1;
        startForeground(notifyID, notification);

        AmbientInfoService.isRunning = true;
        AmbientInfoService.deviceConnected = device;

        //do heavy work on a background thread
        connectMockup(device);
        // connect(deviceAddress);
        return START_NOT_STICKY;
    }

    private void connectMockup(BluetoothDevice device) {
        Log.i(TAG, "In onStartCommand");
        new Thread(new Runnable() {
            public void run() {
                try {
                    Random rand = new Random();
                    while (AmbientInfoService.isRunning) {
                        onAmbientChanged(rand.nextFloat() * 40, (rand.nextFloat() * 140), rand.nextFloat() * 100);
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Thread STOPPED");

            }
        }).start();
    }

    private void connect(BluetoothDevice device) {
        if (device != null) {
            //connect to the device found
            BluetoothGattCallback mGattCallback = new AmbientInfoGattCallback(this);
            mGattConnection = device.connectGatt(this, false, mGattCallback);
        } else {
            broadcastActionConnection(false, "Invalid Device");
        }
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

        AmbientInfoService.isRunning = false;
        AmbientInfoService.deviceConnected = null;
        if (mGattConnection != null)
            mGattConnection.close();
    }

    @Override
    public void onConnectionSuccessful() {
        Log.i(TAG, "onConnectionSuccessful ");
        broadcastActionConnection(true, "Device UART connected");
    }

    @Override
    public void onConnectionError(String error) {
        Log.i(TAG, "onConnectionError " + error);
        notifyWarning("Device disconnected");
        broadcastActionConnection(false, error);
    }

    private void broadcastActionConnection(boolean connected, String message) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_CONNECTION_STATUS);
        broadcastIntent.putExtra(EXTRA_CONNECTION_STATE, connected);
        broadcastIntent.putExtra(EXTRA_CONNECTION_MESSAGE, message);
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onAmbientChanged(float temperature, float humidity, float battery) {
        Log.i(TAG, "onAmbientChanged " + temperature);
        if (temperature > 30) {
            notifyWarning("Warning Hot Temperature so high");
        }
        notifyAmbient(temperature, humidity, battery);
        broadcastActionAmbientInfo(temperature, humidity, battery);
    }


    private void broadcastActionAmbientInfo(float temperature, float humidity, float battery) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_AMBIENT_INFO);
        broadcastIntent.putExtra(EXTRA_AMBIENT_TEMPERATURE, temperature);
        broadcastIntent.putExtra(EXTRA_AMBIENT_HUMIDITY, humidity);
        broadcastIntent.putExtra(EXTRA_AMBIENT_BATTERY, battery);
        sendBroadcast(broadcastIntent);
    }
}