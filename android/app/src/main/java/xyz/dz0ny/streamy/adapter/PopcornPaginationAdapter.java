package xyz.dz0ny.streamy.adapter;

import android.content.Context;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.PresenterSelector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.dz0ny.streamy.R;
import xyz.dz0ny.streamy.model.Option;
import xyz.dz0ny.streamy.presenter.CardPresenter;
import xyz.dz0ny.streamy.presenter.LoadingPresenter;
import xyz.dz0ny.streamy.view.LoadingCardView;

public abstract class PopcornPaginationAdapter extends ArrayObjectAdapter {

    public static final String KEY_NEXT_PAGE = "next_page";
    public static final String KEY_SORT = "sort";

    private Context mContext;
    private Integer mNextPage;
    private LoadingPresenter mLoadingPresenter;
    private CardPresenter mIconItemPresenter;
    private Presenter mPresenter;
    private int mLoadingIndicatorPosition;
    private String mSort;

    public PopcornPaginationAdapter(Context context, Presenter presenter, String sort) {
        mContext = context;
        mPresenter = presenter;
        mLoadingPresenter = new LoadingPresenter();
        mIconItemPresenter = new CardPresenter();
        mLoadingIndicatorPosition = -1;
        mNextPage = 1;
        mSort = sort;
        setPresenterSelector();
    }

    public void setNextPage() {
        mNextPage += 1;
    }

    public void setPresenterSelector() {
        setPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object item) {
                if (item instanceof LoadingCardView) {
                    return mLoadingPresenter;
                } else if (item instanceof Option) {
                    return mIconItemPresenter;
                }
                return mPresenter;
            }
        });
    }

    public List<Object> getItems() {
        return unmodifiableList();
    }

    public boolean shouldShowLoadingIndicator() {
        return mLoadingIndicatorPosition == -1;
    }

    public void showLoadingIndicator() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                mLoadingIndicatorPosition = size();
                add(mLoadingIndicatorPosition, new LoadingCardView(mContext));
                notifyItemRangeInserted(mLoadingIndicatorPosition, 1);
            }
        });
    }

    public void removeLoadingIndicator() {
        try {
            removeItems(mLoadingIndicatorPosition, 1);
            notifyItemRangeRemoved(mLoadingIndicatorPosition, 1);
        } catch (Exception ignored) {

        }

        mLoadingIndicatorPosition = -1;
    }


    public void addResults(List<?> posts) {
        if (posts.size() > 0) {
            addAll(size(), posts);
        } else {
            mNextPage = 0;
        }
    }

    public void removeAll() {
        for (Object i : getItems()) {
            remove(i);
        }
    }

    public boolean shouldLoadNextPage() {
        return shouldShowLoadingIndicator() && mNextPage != 0;
    }

    public Map<String, String> getAdapterOptions() {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_NEXT_PAGE, String.valueOf(mNextPage.toString()));
        map.put(KEY_SORT, String.valueOf(mSort.toString()));
        return map;
    }

    public void showReloadCard() {
        Option option = new Option(
                mContext.getString(R.string.no_torrents),
                mContext.getString(R.string.try_again),
                R.drawable.ic_refresh_white);
        add(option);
    }

    public void showTryAgainCard() {
        Option option = new Option(
                mContext.getString(R.string.no_torrents),
                mContext.getString(R.string.try_again),
                R.drawable.ic_refresh_white);
        add(option);
    }

    public void removeReloadCard() {
        if (isRefreshCardDisplayed()) {
            removeItems(0, 1);
            notifyItemRangeRemoved(size(), 1);
        }
    }

    public boolean isRefreshCardDisplayed() {
        Object item = get(size() - 1);
        if (item instanceof Option) {
            Option option = (Option) item;
            String noVideosTitle = mContext.getString(R.string.no_torrents);
            String oopsTitle = mContext.getString(R.string.try_again);
            return (option.title.equals(noVideosTitle) ||
                    option.title.equals(oopsTitle));
        }
        return false;
    }

    public abstract void addAllItems(List<?> items);

    public abstract List<?> getAllItems();


}