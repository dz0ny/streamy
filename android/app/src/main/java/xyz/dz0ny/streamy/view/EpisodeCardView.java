package xyz.dz0ny.streamy.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import butterknife.BindView;
import butterknife.ButterKnife;
import xyz.dz0ny.streamy.BindableCardView;
import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.remote.popcorn.models.Episode;

public class EpisodeCardView extends BindableCardView<Episode> {

    @BindView(R.id.title)
    TextView mTitle;

    @BindView(R.id.aired)
    TextView mAired;

    @BindView(R.id.body)
    TextView mBody;

    @BindView(R.id.img)
    ImageView mImg;

    public EpisodeCardView(Context context) {
        super(context);
        ButterKnife.bind(this);
    }

    @Override
    public void bind(Object data) {
        Episode ep = (Episode) data;
        mTitle.setText(ep.getInfo());
        mBody.setText(ep.getBody());
        mAired.setText(ep.getAired());
        Glide.with(getContext())
                .load(ep.getImg())
                .asBitmap()
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.default_background)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                                                GlideAnimation<? super Bitmap> glideAnimation) {
                        mImg.setImageBitmap(bitmap);

                    }
                });

    }

    @Override
    protected int getLayoutResource() {
        return R.layout.card_episode_img;
    }
}