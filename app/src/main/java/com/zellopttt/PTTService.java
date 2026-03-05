package com.zellopttt;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PTTService extends Service {

    private static final String TAG = "ZelloPTT";
    private static final String CHANNEL_ID = "zello_ptt_channel";
    private static final int KEYCODE_ZELLO_PTT = 250;
    private static final String ZELLO_PACKAGE = "com.loudtalks";
    private Thread inputThread;
    private volatile boolean running = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Zello PTT Active")
                .setContentText("Button monitoring active")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .build();
        startForeground(1, notification);

        startInputMonitor();
        return START_STICKY;
    }

    private void startInputMonitor() {
        inputThread = new Thread(() -> {
            try {
                FileInputStream fis = new FileInputStream("/dev/input/event4");
                ByteBuffer buffer = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
                byte[] bytes = new byte[24];

                while (running) {
                    int read = fis.read(bytes);
                    if (read < 24) continue;
                    buffer.clear();
                    buffer.put(bytes);
                    buffer.flip();

                    long tvSec = buffer.getLong();
                    long tvUsec = buffer.getLong();
                    short type = buffer.getShort();
                    short code = buffer.getShort();
                    int value = buffer.getInt();

                    // EV_KEY = 1, code 250, value 1 = DOWN
                    if (type == 1 && code == KEYCODE_ZELLO_PTT && value == 1) {
                        Log.d(TAG, "PTT button pressed - launching Zello");
                        launchZello();
                    }
                }
                fis.close();
            } catch (Exception e) {
                Log.e(TAG, "Input monitor error: " + e.getMessage());
            }
        });
        inputThread.start();
    }

    private void launchZello() {
        Intent launch = getPackageManager().getLaunchIntentForPackage(ZELLO_PACKAGE);
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                          Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(launch);
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Zello PTT Service",
                NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }
}
