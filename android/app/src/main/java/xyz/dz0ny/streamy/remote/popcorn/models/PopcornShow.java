package xyz.dz0ny.streamy.remote.popcorn.models;


import android.annotation.SuppressLint;

import java.io.Serializable;

public class PopcornShow implements Serializable {
    static final long serialVersionUID = 627566175075960653L;

    private String imdb_id;
    private String title;
    private String synopsis;
    private String trailer;
    private String year;
    private String runtime;
    private Integer num_seasons;
    private MovieImages images;

    public String getTitle() {
        return title;
    }

    public String getTrailer() {
        return trailer;
    }

    public String getBody() {
        return synopsis;
    }

    public MovieImages getImages() {
        return images;
    }

    @SuppressLint("DefaultLocale")
    public String getInfo() {
        return String.format("%s %d seasons", year, num_seasons);
    }

    public String getID() {
        return imdb_id;
    }
}

