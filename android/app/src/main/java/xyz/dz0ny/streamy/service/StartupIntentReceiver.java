package xyz.dz0ny.streamy.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class StartupIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent arg1) {
        Intent intent = new Intent(context, Server.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        Log.i("Autostart", "started");
    }
}
