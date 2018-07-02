package xyz.dz0ny.streamy.utils;

import android.content.Intent;
import android.net.Uri;

import timber.log.Timber;

public class VLC {
    public static Intent Intent(String title, Uri url) {
        Timber.i("Playing '%s'", url.toString());
        Intent vlcIntent = new Intent(Intent.ACTION_VIEW);
        vlcIntent.setDataAndTypeAndNormalize(url, "video/mp4");
        return vlcIntent;
    }
}
