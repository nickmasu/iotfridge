package manrique.nicolas.iotfridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class TemperatureService extends Service implements TemperatureGattCallback.DeviceInfoCallback {

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String TAG = "TemperatureService";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        //do heavy work on a background thread

        connectMockup(input);

        //stopSelf();
        return START_NOT_STICKY;
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            manager.getNotificationChannel(CHANNEL_ID).setImportance(NotificationManager.IMPORTANCE_LOW);
        }
    }

    private void connectMockup(String deviceAdress) {
        Log.i(TAG, "In onStartCommand");
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(MainActivity.mBroadcastStringAction);
                broadcastIntent.putExtra("Data", "Broadcast Data");
                sendBroadcast(broadcastIntent);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                broadcastIntent.setAction(MainActivity.mBroadcastIntegerAction);
                broadcastIntent.putExtra("Data", 10);
                sendBroadcast(broadcastIntent);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void connect(String deviceAdress) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAdress);
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
    public void onDeviceStateChanged(String state) {
        Log.i(TAG, "onDeviceStateChanged " + state);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.mBroadcastStringAction);
        broadcastIntent.putExtra("Data", "Broadcast Data");
        sendBroadcast(broadcastIntent);
    }

    @Override
    public void onTemperatureChanged(float temperature) {
        Log.i(TAG, "onTemperatureChanged " + temperature);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.mBroadcastIntegerAction);
        broadcastIntent.putExtra("Data", temperature);
        sendBroadcast(broadcastIntent);
    }
}