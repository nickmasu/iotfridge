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

import java.util.Random;

public class TemperatureService extends Service implements TemperatureGattCallback.DeviceInfoCallback {

    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    private static final String TAG = "TemperatureService";


    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

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
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String deviceAddress = intent.getStringExtra("deviceAddress");


        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(deviceAddress)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        //do heavy work on a background thread

        connectMockup(deviceAddress);


        //stopSelf();
        return START_NOT_STICKY;
    }


    private void connectMockup(String deviceAdress) {
        Log.i(TAG, "In onStartCommand");
        new Thread(new Runnable() {
            public void run() {
                try {
                    Random rand = new Random();
                    while (true) {
                        Thread.sleep(5000);
                        onTemperatureChanged(rand.nextFloat() * 40);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.mBroadcastIntegerAction);
        broadcastIntent.putExtra("temperature", temperature);
        sendBroadcast(broadcastIntent);
    }
}