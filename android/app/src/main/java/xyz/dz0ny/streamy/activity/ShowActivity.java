package xyz.dz0ny.streamy.activity;

import android.app.Activity;
import android.os.Bundle;

import xyz.dz0ny.streamy.R;

public class ShowActivity extends Activity {

    public static final String SHOW = "Show";
    public static final String SHARED_ELEMENT_NAME = "hero";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
    }
}