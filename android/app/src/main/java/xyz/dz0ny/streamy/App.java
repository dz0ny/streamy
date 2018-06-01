package xyz.dz0ny.streamy;

import android.app.Application;

import timber.log.Timber;

public class App extends Application {

    private static App instance;

    public static App instance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

    }

}