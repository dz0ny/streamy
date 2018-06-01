package xyz.dz0ny.streamy.remote.popcorn.models;

import java.io.Serializable;

public class MovieImages implements Serializable {
    static final long serialVersionUID = 827566175075960653L;
    private String imdb_id;
    private String poster;
    private String fanart;

    public String getIMDB() {
        return imdb_id;
    }

    public String getPoster() {
        return poster;
    }

    public String getFanart() {
        if (fanart == null) {
            return "";
        }
        return fanart.replace("/w500/", "/original/");
    }

    public String getBanner() {
        return getFanart();
    }
}
