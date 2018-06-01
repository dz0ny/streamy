package xyz.dz0ny.streamy.remote.popcorn.models;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Episode {

    String overview;
    String title;
    Integer episode;
    Integer season;
    Long first_aired;

    @SuppressLint("DefaultLocale")
    public String getInfo() {
        return String.format("E%02d %s", episode, title);
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return overview;
    }

    public String getAired() {
        Date d = new Date(first_aired * 1000);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
        return dt.format(d);
    }
}
