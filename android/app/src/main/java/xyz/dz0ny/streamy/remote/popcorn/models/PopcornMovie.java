package xyz.dz0ny.streamy.remote.popcorn.models;

import com.squareup.moshi.Json;

import java.io.Serializable;

public class PopcornMovie implements Serializable {
    static final long serialVersionUID = 727566175075960653L;

    private String imdb_id;
    private String title;
    private String synopsis;
    private String trailer;
    private String year;
    private String runtime;
    private MovieImages images;
    private Torrents torrents;

    public String getTitle() {
        return title;
    }

    public String getTrailer() {
        return trailer;
    }

    public String getBody() {
        return synopsis;
    }

    public String get720p() {
        return torrents.english.s720p.url;
    }

    public String get1080p() {
        return torrents.english.s1080p.url;
    }

    public MovieImages getImages() {
        return images;
    }

    public String getInfo() {
        return String.format("%s %smin", year, runtime);
    }
}

class Torrents implements Serializable {

    private final static long serialVersionUID = 2217272369202931506L;
    @Json(name = "en")
    public English english;

}

class Magnet implements Serializable {

    private final static long serialVersionUID = 8274912766082723550L;
    @Json(name = "filesize")
    public String filesize;
    @Json(name = "url")
    public String url;

}


class English implements Serializable {

    private final static long serialVersionUID = 404013127460491054L;
    @Json(name = "1080p")
    public Magnet s1080p;
    @Json(name = "720p")
    public Magnet s720p;
}