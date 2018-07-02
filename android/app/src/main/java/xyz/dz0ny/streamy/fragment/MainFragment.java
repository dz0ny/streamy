/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package xyz.dz0ny.streamy.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.activity.DetailsActivity;
import xyz.dz0ny.streamy.activity.SearchActivity;
import xyz.dz0ny.streamy.activity.ShowActivity;
import xyz.dz0ny.streamy.activity.TorrentActivity;
import xyz.dz0ny.streamy.adapter.MoviesAdapterPopcorn;
import xyz.dz0ny.streamy.adapter.PopcornPaginationAdapter;
import xyz.dz0ny.streamy.adapter.ShowsAdapterPopcorn;
import xyz.dz0ny.streamy.presenter.TorrentPresenter;
import xyz.dz0ny.streamy.remote.ApiCalls;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornMovie;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornShow;
import xyz.dz0ny.streamy.remote.streamy.models.StreamyTorrent;

public class MainFragment extends BrowseFragment {

    private static final int BACKGROUND_UPDATE_DELAY = 300;

    private final Handler mHandler = new Handler();
    private final int REQUEST_PERMISSION_RECORD_AUDIO = 0;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private String mBackgroundUri;
    private BackgroundManager mBackgroundManager;
    private ArrayObjectAdapter rowsAdapter;
    private ArrayObjectAdapter torrents;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Timber.i("onCreate");
        super.onActivityCreated(savedInstanceState);

        prepareBackgroundManager();
        setupUIElements();
        loadRows();
        setupEventListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mBackgroundTimer) {
            Timber.d("onDestroy: %s", mBackgroundTimer.toString());
            mBackgroundTimer.cancel();
        }
    }

    private void loadRows() {
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        MoviesAdapterPopcorn popular = new MoviesAdapterPopcorn(getActivity(), "trending");
        addMoviesSubscription(popular);
        rowsAdapter.add(new ListRow(new HeaderItem(0, "Popular Movies"), popular));
        MoviesAdapterPopcorn newest = new MoviesAdapterPopcorn(getActivity(), "last added");
        addMoviesSubscription(newest);
        rowsAdapter.add(new ListRow(new HeaderItem(1, "Newest Movies"), newest));
        MoviesAdapterPopcorn rated = new MoviesAdapterPopcorn(getActivity(), "rating");
        addMoviesSubscription(rated);
        rowsAdapter.add(new ListRow(new HeaderItem(2, "Movies by Rating"), rated));


        ShowsAdapterPopcorn populars = new ShowsAdapterPopcorn(getActivity(), "trending");
        addShowsubscription(populars);
        rowsAdapter.add(new ListRow(new HeaderItem(3, "Popular Shows"), populars));
        ShowsAdapterPopcorn newests = new ShowsAdapterPopcorn(getActivity(), "updated");
        addShowsubscription(newests);
        rowsAdapter.add(new ListRow(new HeaderItem(4, "Newest Shows"), newests));
        ShowsAdapterPopcorn rateds = new ShowsAdapterPopcorn(getActivity(), "rating");
        addShowsubscription(rateds);
        rowsAdapter.add(new ListRow(new HeaderItem(5, "Shows by Rating"), rateds));

        torrents = new ArrayObjectAdapter(new TorrentPresenter());
        addTorrentsSubscription(torrents);
        rowsAdapter.add(new ListRow(new HeaderItem(6, "Torrents"), torrents));

        setAdapter(rowsAdapter);
    }

    @SuppressLint("CheckResult")
    private void addTorrentsSubscription(ArrayObjectAdapter adapter) {
        ApiCalls.getTorrents()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(
                        torrents -> bindTorrentDetails(torrents, adapter),
                        throwable -> new Handler().postDelayed(() -> addTorrentsSubscription(adapter), 5000)
                );
    }

    private void bindTorrentDetails(List<StreamyTorrent> torrents, ArrayObjectAdapter adapter) {
        if (!torrents.isEmpty()) {
            adapter.addAll(0, torrents);
        }
    }

    @SuppressLint("CheckResult")
    private void addShowsubscription(final ShowsAdapterPopcorn adapter) {
        if (adapter.shouldShowLoadingIndicator()) adapter.showLoadingIndicator();

        Map<String, String> options = adapter.getAdapterOptions();
        String nextPage = options.get(PopcornPaginationAdapter.KEY_NEXT_PAGE);
        String sort = options.get(PopcornPaginationAdapter.KEY_SORT);

        ApiCalls.getShows(nextPage, sort)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(popcornShows -> bindShowDetails(popcornShows, adapter), Timber::e);
    }

    @SuppressLint("CheckResult")
    private void addMoviesSubscription(final MoviesAdapterPopcorn adapter) {
        if (adapter.shouldShowLoadingIndicator()) adapter.showLoadingIndicator();

        Map<String, String> options = adapter.getAdapterOptions();
        String nextPage = options.get(PopcornPaginationAdapter.KEY_NEXT_PAGE);
        String sort = options.get(PopcornPaginationAdapter.KEY_SORT);

        ApiCalls.getMovies(nextPage, sort)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .subscribe(popcornMovies -> bindMoviesDetails(popcornMovies, adapter), Timber::e);

    }

    private void bindMoviesDetails(List<PopcornMovie> popcornMovies, MoviesAdapterPopcorn adapter) {
        adapter.removeLoadingIndicator();
        if (adapter.size() == 0 && popcornMovies.isEmpty()) {
            adapter.showReloadCard();
        } else {
            adapter.setNextPage();
            adapter.addAllItems(popcornMovies);
        }
    }

    private void bindShowDetails(List<PopcornShow> popcornShows, ShowsAdapterPopcorn adapter) {
        adapter.removeLoadingIndicator();
        if (adapter.size() == 0 && popcornShows.isEmpty()) {
            adapter.showReloadCard();
        } else {
            adapter.setNextPage();
            adapter.addAllItems(popcornShows);
        }
    }

    private void prepareBackgroundManager() {

        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());

        mDefaultBackground = ContextCompat.getDrawable(getContext(), R.drawable.default_background);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getContext(), R.color.fastlane_background));
        // set search icon color
        setSearchAffordanceColor(ContextCompat.getColor(getContext(), R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(view -> {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_PERMISSION_RECORD_AUDIO);

            } else {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(this::getOnItemViewClickedListener);
        setOnItemViewSelectedListener(this::getOnItemViewSelectedListener);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_RECORD_AUDIO) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // launch SearchActivity
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            } else {
            }
        }
    }

    private void getOnItemViewSelectedListener(
            Presenter.ViewHolder itemViewHolder,
            Object item,
            RowPresenter.ViewHolder rowViewHolder,
            Row row
    ) {
        if (item instanceof PopcornMovie) {
            mBackgroundUri = ((PopcornMovie) item).getImages().getFanart();
            startBackgroundTimer();

            int index = rowsAdapter.indexOf(row);
            MoviesAdapterPopcorn adapter =
                    ((MoviesAdapterPopcorn) ((ListRow) rowsAdapter.get(index)).getAdapter());
            if (adapter.get(adapter.size() - 1).equals(item) && adapter.shouldLoadNextPage()) {
                addMoviesSubscription(adapter);
            }
        }
        if (item instanceof PopcornShow) {
            mBackgroundUri = ((PopcornShow) item).getImages().getFanart();
            startBackgroundTimer();

            int index = rowsAdapter.indexOf(row);
            ShowsAdapterPopcorn adapter =
                    ((ShowsAdapterPopcorn) ((ListRow) rowsAdapter.get(index)).getAdapter());
            if (adapter.get(adapter.size() - 1).equals(item) && adapter.shouldLoadNextPage()) {
                addShowsubscription(adapter);
            }
        }
        if (item instanceof StreamyTorrent) {
            addTorrentsSubscription(torrents);
        }
    }

    private void getOnItemViewClickedListener(
            Presenter.ViewHolder itemViewHolder,
            Object item,
            RowPresenter.ViewHolder rowViewHolder,
            Row row
    ) {

        if (item instanceof PopcornMovie) {
            PopcornMovie movie = (PopcornMovie) item;
            Timber.d("Item: %s", item.toString());
            Intent intent = new Intent(getActivity(), DetailsActivity.class);
            intent.putExtra(DetailsActivity.MOVIE, movie);

            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),
                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                    DetailsActivity.SHARED_ELEMENT_NAME)
                    .toBundle();
            getActivity().startActivity(intent, bundle);
        }

        if (item instanceof PopcornShow) {
            PopcornShow show = (PopcornShow) item;
            Timber.d("Item: %s", item.toString());
            Intent intent = new Intent(getActivity(), ShowActivity.class);
            intent.putExtra(ShowActivity.SHOW, show);

            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),
                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                    ShowActivity.SHARED_ELEMENT_NAME)
                    .toBundle();
            getActivity().startActivity(intent, bundle);
        }

        if (item instanceof StreamyTorrent) {
            StreamyTorrent torrent = (StreamyTorrent) item;
            Timber.d("Item: %s", item.toString());
            Intent intent = new Intent(getActivity(), TorrentActivity.class);
            intent.putExtra(TorrentActivity.SHOW, torrent);

            Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(),
                    ((ImageCardView) itemViewHolder.view).getMainImageView(),
                    TorrentActivity.SHARED_ELEMENT_NAME)
                    .toBundle();
            getActivity().startActivity(intent, bundle);
        }
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(mDefaultBackground)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        mBackgroundManager.setDrawable(resource);
                    }
                });
        mBackgroundTimer.cancel();
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), BACKGROUND_UPDATE_DELAY);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(() -> updateBackground(mBackgroundUri));
        }
    }

}
