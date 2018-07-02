package xyz.dz0ny.streamy.adapter;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import xyz.dz0ny.streamy.presenter.CardPresenter;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornShow;

public class ShowsAdapterPopcorn extends PopcornPaginationAdapter {

    public ShowsAdapterPopcorn(Context context, String sort) {
        super(context, new CardPresenter(), sort);
    }

    @Override
    public void addAllItems(List<?> items) {
        List<PopcornShow> currentPosts = getAllItems();
        ArrayList<PopcornShow> posts = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Object object = items.get(i);
            if (object instanceof PopcornShow && !currentPosts.contains(object)) {
                posts.add((PopcornShow) object);
            }
        }
        addResults(posts);
    }

    @Override
    public List<PopcornShow> getAllItems() {
        List<Object> itemList = getItems();
        ArrayList<PopcornShow> posts = new ArrayList<>();
        for (int i = 0; i < itemList.size(); i++) {
            Object object = itemList.get(i);
            if (object instanceof PopcornShow) posts.add((PopcornShow) object);
        }
        return posts;
    }
}