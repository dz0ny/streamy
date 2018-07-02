package xyz.dz0ny.streamy;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v17.leanback.widget.BaseCardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;


public abstract class BindableCardView<T> extends BaseCardView {

    public BindableCardView(Context context) {
        super(context);
        initLayout();
    }

    public BindableCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLayout();
    }

    public BindableCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout();
    }

    private void initLayout() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(getLayoutResource(), this);
    }

    protected abstract void bind(Object data);

    protected abstract @LayoutRes
    int getLayoutResource();
}
