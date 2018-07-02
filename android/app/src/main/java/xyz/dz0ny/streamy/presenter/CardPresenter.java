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

package xyz.dz0ny.streamy.presenter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.Presenter;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornMovie;
import xyz.dz0ny.streamy.remote.popcorn.models.PopcornShow;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class CardPresenter extends Presenter {
    private static final String TAG = "CardPresenter";

    private static final int CARD_WIDTH = 276;
    private static final int CARD_HEIGHT = 413;
    private static int sSelectedBackgroundColor;
    private static int sDefaultBackgroundColor;
    Context mContext;
    private Drawable mDefaultCardImage;

    private static void updateCardBackgroundColor(ImageCardView view, boolean selected) {
        int color = selected ? sSelectedBackgroundColor : sDefaultBackgroundColor;
        // Both background colors should be set because the view's background is temporarily visible
        // during animations.
        view.setBackgroundColor(color);
        view.findViewById(R.id.info_field).setBackgroundColor(color);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        if (mContext == null) {
            // We do this to avoid creating a new ContextThemeWrapper for each one of the cards
            // If we look inside the ImageCardView they warn us about the same this.
            // Example: Try using the constructor: ImageCardView(context, style)
            // It is deprecated right? This is because that constructor creates a new ContextThemeWrapper every time a
            // ImageCardView is allocated.
            mContext = new ContextThemeWrapper(parent.getContext(), R.style.AppTheme);
        }

        sDefaultBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.default_background);
        sSelectedBackgroundColor =
                ContextCompat.getColor(parent.getContext(), R.color.selected_background);
        /*
         * This template uses a default image in res/drawable, but the general case for Android TV
         * will require your resources in xhdpi. For more information, see
         * https://developer.android.com/training/tv/start/layouts.html#density-resources
         */
        mDefaultCardImage = ContextCompat.getDrawable(parent.getContext(), R.drawable.movie);

        ImageCardView cardView =
                new ImageCardView(mContext) {
                    @Override
                    public void setSelected(boolean selected) {
                        updateCardBackgroundColor(this, selected);
                        super.setSelected(selected);
                    }
                };

        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {

        ImageCardView cardView = (ImageCardView) viewHolder.view;
        if (item instanceof PopcornMovie) {
            PopcornMovie movie = (PopcornMovie) item;
            cardView.setTitleText(movie.getTitle());
            cardView.setContentText(movie.getInfo());
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
            Glide.with(viewHolder.view.getContext())
                    .load(movie.getImages().getPoster())
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(mDefaultCardImage)
                    .into(cardView.getMainImageView());
        }
        if (item instanceof PopcornShow) {
            PopcornShow show = (PopcornShow) item;
            cardView.setTitleText(show.getTitle());
            cardView.setContentText(show.getInfo());
            cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT);
            Glide.with(viewHolder.view.getContext())
                    .load(show.getImages().getPoster())
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(mDefaultCardImage)
                    .into(cardView.getMainImageView());
        }
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        ImageCardView cardView = (ImageCardView) viewHolder.view;
        // Remove references to images so that the garbage collector can free up memory
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);
    }
}
