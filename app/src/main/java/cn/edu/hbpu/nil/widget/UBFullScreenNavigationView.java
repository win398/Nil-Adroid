package cn.edu.hbpu.nil.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import cn.edu.hbpu.nil.util.other.UIUtils;

/**
 * Created by Andy.chen on 2016/6/30.
 * NavigationView 默认是不全屏 显示占屏幕80%
 * 重写该类，实现全屏
 *
 */
public class UBFullScreenNavigationView extends NavigationView {
    private final static String TAG  = UBFullScreenNavigationView.class.getSimpleName();

    public UBFullScreenNavigationView(Context context) {
        super(context);
        initView(context);
    }

    public UBFullScreenNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public UBFullScreenNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(final Context context) {
        //侦测待UI完全加载完成才允许计算宽值，否则取得值为0
        ViewTreeObserver vto = this.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressWarnings("deprecation")
            @Override
            public void onGlobalLayout() {
                setFullScreenWidth(context);
            }
        });
    }

    public void setFullScreenWidth(Context context) {
        DrawerLayout.LayoutParams params = (DrawerLayout.LayoutParams) this.getLayoutParams();
        params.width = UIUtils.getScreenWidth(context);
        this.setLayoutParams(params);
    }
}
