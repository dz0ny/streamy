package xyz.dz0ny.streamy.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import tv.Tv;
import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.activity.MainActivity;

public class Server extends Service {
    private static final String TAG = "StreamyServer";
    private static final String CHANNEL_DEFAULT_IMPORTANCE = "Streamy";


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        Tv.stop();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String path = this.getCacheDir().getAbsolutePath();
        Toast.makeText(this, path, Toast.LENGTH_LONG).show();
        Log.d(TAG, path);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                        .setContentTitle("Streamy")
                        .setContentText("Streamy")
                        .setSmallIcon(R.drawable.app_icon_your_company)
                        .setContentIntent(pendingIntent)
                        .setPriority(Notification.PRIORITY_LOW)
                        .setTicker("Streamy")
                        .build();

        startForeground(1, notification);
        Tv.start(path);
        return Service.START_STICKY;
    }
}
