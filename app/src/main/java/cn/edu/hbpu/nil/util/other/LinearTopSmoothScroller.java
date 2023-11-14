package cn.edu.hbpu.nil.util.other;

import android.content.Context;
import android.util.DisplayMetrics;

import androidx.recyclerview.widget.LinearSmoothScroller;

public class LinearTopSmoothScroller extends LinearSmoothScroller {

    /**
     * MILLISECONDS_PER_INCH 值越大滚动越慢
     */
    private float MILLISECONDS_PER_INCH = 0.03f;
    private final Context context;

    /**
     * @param context  context
     * @param needFast 是否需要快速滑动
     */
    public LinearTopSmoothScroller(Context context, boolean needFast) {
        super(context);
        this.context = context;
        if (needFast) {
            setScrollFast();
        } else {
            setScrollSlowly();
        }
    }

    @Override
    protected int getVerticalSnapPreference() {
        return SNAP_TO_START;
    }

    @Override
    protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
        //return super.calculateSpeedPerPixel(displayMetrics);
        setScrollSlowly();
        return MILLISECONDS_PER_INCH / displayMetrics.density;
    }

    public void setScrollSlowly() {
        //建议不同分辨率设备上的滑动速度相同
        //0.3f可以根据不同自己的需求进行更改
        MILLISECONDS_PER_INCH = context.getResources().getDisplayMetrics().density * 0.3f;
    }

    public void setScrollFast() {
        MILLISECONDS_PER_INCH = context.getResources().getDisplayMetrics().density * 0.03f;
    }

}
