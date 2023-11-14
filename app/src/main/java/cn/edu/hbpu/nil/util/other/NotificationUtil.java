package cn.edu.hbpu.nil.util.other;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

/**
 * Created by Administrator
 * 开启通知
 */

public class NotificationUtil {
    /**
     * 跳转到app的设置界面--开启通知
     * @param context
     */
    public static void goToNotificationSetting(Context context) {
        Intent intent = new Intent();

        if (Build.VERSION.SDK_INT >= 26) {
            // android 8.0引导
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
        } else {
            // android 5.0-7.0
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 判断是否需要打开通知
     * @param context
     */
    public static boolean isNotificationEnabled(Context context) {
        boolean isOpened = false;

        try {
            isOpened = NotificationManagerCompat.from(context).areNotificationsEnabled();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return isOpened;
    }
}