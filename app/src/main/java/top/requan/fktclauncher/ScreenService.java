package top.requan.fktclauncher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class ScreenService extends Service {
    private ScreenOffReceiver screenOffReceiver;

    private static final String CHANNEL_ID = "ScreenServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        screenOffReceiver = new ScreenOffReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOffReceiver, filter);

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Service")
                .setContentText("Running in the background")
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification); // 1是通知ID
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenOffReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
