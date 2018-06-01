package xyz.dz0ny.streamy.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import tv.Tv;

public class Server extends Service {
    private static final String TAG = "StreamyServer";

    public Server() {
    }

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String path = this.getCacheDir().getAbsolutePath();
        Toast.makeText(this, path, Toast.LENGTH_LONG).show();
        Log.d(TAG, path);

        Tv.start(path);
        return super.onStartCommand(intent, flags, startId);
    }
}
