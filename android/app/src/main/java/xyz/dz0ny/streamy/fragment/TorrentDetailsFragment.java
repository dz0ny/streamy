package xyz.dz0ny.streamy.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.app.DetailsFragmentBackgroundController;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.activity.DetailsActivity;
import xyz.dz0ny.streamy.activity.MainActivity;
import xyz.dz0ny.streamy.presenter.DetailsDescriptionPresenter;
import xyz.dz0ny.streamy.remote.ApiCalls;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornMovie;
import xyz.dz0ny.streamy.remote.streamy.models.StreamyTorrent;
import xyz.dz0ny.streamy.utils.VLC;

public class TorrentDetailsFragment extends DetailsFragment {

    private static final int ACTION_WATCH_L = 1;
    private static final int ACTION_WATCH_H = 2;
    private static final int ACTION_TRAILER = 3;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 413;


    private PopcornMovie mSelectedMovie;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private DetailsFragmentBackgroundController mDetailsBackground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        mDetailsBackground = new DetailsFragmentBackgroundController(this);

        mSelectedMovie = (PopcornMovie) getActivity().getIntent().getSerializableExtra(DetailsActivity.MOVIE);
        if (mSelectedMovie != null) {
            mPresenterSelector = new ClassPresenterSelector();
            mAdapter = new ArrayObjectAdapter(mPresenterSelector);
            setupDetailsOverviewRow();
            setupDetailsOverviewRowPresenter();
            setAdapter(mAdapter);
            initializeBackground(mSelectedMovie);
        } else {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

    private void initializeBackground(PopcornMovie data) {
        mDetailsBackground.enableParallax();
        Glide.with(getActivity())
                .load(data.getImages().getBanner())
                .asBitmap()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.default_background)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                                                GlideAnimation<? super Bitmap> glideAnimation) {
                        mDetailsBackground.setCoverBitmap(bitmap);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });
    }

    private void setupDetailsOverviewRow() {
        Timber.d("doInBackground: %s", mSelectedMovie.toString());
        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedMovie);
        row.setImageDrawable(
                ContextCompat.getDrawable(getContext(), R.drawable.default_background));
        int width = convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH);
        int height = convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT);
        Glide.with(getActivity())
                .load(mSelectedMovie.getImages().getPoster())
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.default_background)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        Timber.d("details overview card image url ready: %s", resource);
                        row.setImageDrawable(resource);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });

        ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter();

        actionAdapter.add(
                new Action(
                        ACTION_TRAILER,
                        getResources().getString(R.string.trailer)));

        actionAdapter.add(
                new Action(
                        ACTION_WATCH_L,
                        getResources().getString(R.string.watch), "720p"));

        actionAdapter.add(
                new Action(
                        ACTION_WATCH_H,
                        getResources().getString(R.string.watch), "1080p"));

        row.setActionsAdapter(actionAdapter);

        mAdapter.add(row);
    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(getContext(), R.color.selected_background));

        // Hook up transition element.
        FullWidthDetailsOverviewSharedElementHelper sharedElementHelper =
                new FullWidthDetailsOverviewSharedElementHelper();
        sharedElementHelper.setSharedElementEnterTransition(
                getActivity(), DetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(sharedElementHelper);
        detailsPresenter.setParticipatingEntranceTransition(true);

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @SuppressLint("CheckResult")
            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == ACTION_WATCH_L) {
                    ApiCalls.addTorrent(mSelectedMovie.get720p())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .unsubscribeOn(Schedulers.io())
                            .subscribe(this::bindTorrentResult, Timber::e);
                }
                if (action.getId() == ACTION_WATCH_H) {
                    ApiCalls.addTorrent(mSelectedMovie.get1080p())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .unsubscribeOn(Schedulers.io())
                            .subscribe(this::bindTorrentResult, Timber::e);
                }
                if (action.getId() == ACTION_TRAILER) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(mSelectedMovie.getTrailer()));
                    getContext().startActivity(intent);
                }

            }

            private void bindTorrentResult(StreamyTorrent streamyTorrent) {
                String url = streamyTorrent.getPlayableFile();
                if (url == null) {
                    Toast.makeText(getContext(), "Cannot play this torrent file!", Toast.LENGTH_LONG).show();
                    return;
                }

                getContext().startActivity(VLC.Intent("Web", streamyTorrent.getUrl()));
            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }


    private int convertDpToPixel(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

}
