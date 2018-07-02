package xyz.dz0ny.streamy.adapter;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import xyz.dz0ny.streamy.presenter.CardPresenter;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornMovie;

public class MoviesAdapterPopcorn extends PopcornPaginationAdapter {

    public MoviesAdapterPopcorn(Context context, String sort) {
        super(context, new CardPresenter(), sort);
    }

    @Override
    public void addAllItems(List<?> items) {
        List<PopcornMovie> currentPosts = getAllItems();
        ArrayList<PopcornMovie> posts = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Object object = items.get(i);
            if (object instanceof PopcornMovie && !currentPosts.contains(object)) {
                posts.add((PopcornMovie) object);
            }
        }
        addResults(posts);
    }

    @Override
    public List<PopcornMovie> getAllItems() {
        List<Object> itemList = getItems();
        ArrayList<PopcornMovie> posts = new ArrayList<>();
        for (int i = 0; i < itemList.size(); i++) {
            Object object = itemList.get(i);
            if (object instanceof PopcornMovie) posts.add((PopcornMovie) object);
        }
        return posts;
    }
}