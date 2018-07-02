package xyz.dz0ny.streamy.remote.popcorn.models;

import android.annotation.SuppressLint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ShowDetails implements Serializable {
    static final long serialVersionUID = 1272346175075960653L;

    private String imdb_id;
    private String tvdb_id;
    private String title;
    private String synopsis;
    private String status;
    private String year;
    private String runtime;
    private Integer num_seasons;
    private MovieImages images;
    private List<Episode> episodes;

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public Integer getSeasons() {
        return num_seasons;
    }

    public String getBody() {
        return synopsis;
    }

    public List<Episode> getEpisodes(int season) {
        List<Episode> eps = new ArrayList<>();
        for (Episode ep : episodes) {
            if (ep.season == season) {
                ep.show_tvdb_id = tvdb_id;
                eps.add(ep);
            }
        }
        eps.sort(Comparator.comparingInt(o -> o.episode));
        return eps;
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
