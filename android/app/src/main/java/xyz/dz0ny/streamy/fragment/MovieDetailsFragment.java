/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.ComponentName;
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
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.activity.DetailsActivity;
import xyz.dz0ny.streamy.activity.MainActivity;
import xyz.dz0ny.streamy.presenter.DetailsDescriptionPresenter;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornMovie;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class MovieDetailsFragment extends DetailsFragment {
    private static final String TAG = "MovieDetailsFragment";

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
        Log.d(TAG, "onCreate DetailsFragment");
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
        Log.d(TAG, "doInBackground: " + mSelectedMovie.toString());
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
                        Log.d(TAG, "details overview card image url ready: " + resource);
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
            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == ACTION_WATCH_H) {
                    Intent vlcIntent = new Intent(Intent.ACTION_VIEW);
                    vlcIntent.setPackage("org.videolan.vlc");
                    vlcIntent.setComponent(new ComponentName("org.videolan.vlc", "org.videolan.vlc.gui.video.VideoPlayerActivity"));
                    //vlcIntent.setDataAndTypeAndNormalize(request.getUrl(), "video/*");
                    getContext().startActivity(vlcIntent);
                }
                if (action.getId() == ACTION_TRAILER) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(mSelectedMovie.getTrailer()));
                    getContext().startActivity(intent);
                }

            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }


    private int convertDpToPixel(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

}
