package xyz.dz0ny.streamy.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import tv.Tv;
import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.activity.MainActivity;

import static android.app.NotificationManager.IMPORTANCE_HIGH;

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

        String CHANNEL_ONE_ID = "xyz.dz0ny.streamy";
        String CHANNEL_ONE_NAME = "Channel One";
        NotificationChannel notificationChannel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        Notification notification = new Notification.Builder(getApplicationContext())
                .setChannelId(CHANNEL_ONE_ID)
                .setContentTitle("Streamy")
                .setContentText("Streamy")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(icon)
                .build();

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

        startForeground(0, notification);

        Tv.start(path);
        return Service.START_STICKY;
    }


}
