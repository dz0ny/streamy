package xyz.dz0ny.streamy.remote.streamy.models;


import android.annotation.SuppressLint;
import android.net.Uri;
import android.text.TextUtils;

import com.squareup.moshi.Json;

import java.io.Serializable;
import java.util.List;

public class StreamyTorrent implements Serializable {

    private final static long serialVersionUID = -4124411585258595890L;
    @Json(name = "name")
    public String name;
    @Json(name = "ih")
    public String ih;
    @Json(name = "files")
    public List<File> files;
    @Json(name = "Downloaded")
    public long downloaded;
    @Json(name = "Missing")
    public long missing;
    @Json(name = "Seeding")
    public boolean seeding;

    @SuppressLint("DefaultLocale")
    static String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.1f %sB", (double) v / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    public String getDownloaded() {
        return formatSize(downloaded);
    }

    public String getSize() {
        return formatSize(missing + downloaded);
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return ih;
    }

    public String getPlayableFile() {
        for (File f : files) {
            if (f.data.endsWith(".mkv") || f.data.endsWith(".mp4") || f.data.endsWith(".avi")) {
                return f.data;
            }
        }
        return null;
    }

    public Uri getUrl() {
        return Uri.parse("http://localhost:9092" + getPlayableFile());
    }
}


class File implements Serializable {

    private final static long serialVersionUID = -3905637854836446547L;
    @Json(name = "Length")
    public long size;
    @Json(name = "Path")
    public List<String> path = null;
    @Json(name = "info")
    public String info;
    @Json(name = "data")
    public String data;

    public String getPath() {
        return TextUtils.join("/", path);
    }
}
