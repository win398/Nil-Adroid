package cn.edu.hbpu.nil.util.other;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.tencent.mmkv.MMKV;
import com.xuexiang.xui.XUI;

public class NilApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        XUI.init(this);
        XUI.debug(true);
        context = getApplicationContext();
        new BGABadgeInit();
        String rootDir = MMKV.initialize(this);
        System.out.println("mmkv root: " + rootDir);
    }

    public static Context getContext() {
        return context;
    }
}
