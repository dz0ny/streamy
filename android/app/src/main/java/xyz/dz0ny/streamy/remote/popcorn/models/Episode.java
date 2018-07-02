package xyz.dz0ny.streamy.remote.popcorn.models;

import android.annotation.SuppressLint;

import com.squareup.moshi.Json;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Episode {

    @Json(name = "torrents")
    public TorrentsShows torrents;
    String overview;
    String title;
    Integer episode;
    Integer season;
    String tvdb_id;
    Long first_aired;
    public String show_tvdb_id;

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


    public String getImg() {
        return String.format("https://www.thetvdb.com/banners/episodes/%s/%s.jpg", show_tvdb_id, tvdb_id);
    }

    public String getMagnet() {
        if (torrents.magnetHD != null){
            return torrents.magnetHD.url;
        }
        return torrents.magnetSD.url;
    }

    public String getAired() {
        Date d = new Date(first_aired * 1000);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
        return dt.format(d);
    }
}


class TorrentsShows implements Serializable {

    private final static long serialVersionUID = 6072825306775792371L;

    @Json(name = "1080p")
    public Magnet magnetHD;

    @Json(name = "0")
    public Magnet magnetSD;

}

