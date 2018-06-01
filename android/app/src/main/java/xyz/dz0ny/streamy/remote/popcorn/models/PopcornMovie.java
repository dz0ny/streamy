package xyz.dz0ny.streamy.remote.popcorn.models;

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

    public String getInfo() {
        return String.format("%s %smin", year, runtime);
    }
}

