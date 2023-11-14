package cn.edu.hbpu.nil.util.other;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

/**
 * Created by Administrator
 * 自启动设置
 */

public class AutoStartUtil {
    /**
     * 获取自启动管理页面的Intent
     * @param context context
     * @return 返回自启动管理页面的Intent
     * */
    public static void getAutostartSettingIntent(Context context) {
        ComponentName componentName = null;
        String brand = Build.MANUFACTURER;
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            switch (brand.toLowerCase()) {
                case "samsung"://三星
                    componentName = new ComponentName("com.samsung.android.sm", "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity");
                    break;

                case "huawei"://华为
                    //荣耀V8，EMUI 8.0.0，Android 8.0上，以下两者效果一样
                    componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity");
//            componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");//目前看是通用的
                    break;

                case "xiaomi"://小米
                    componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
                    break;

                case "vivo"://VIVO
//            componentName = new ComponentName("com.iqoo.secure", "com.iqoo.secure.safaguard.PurviewTabActivity");
                    componentName = new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity");
                    break;

                case "oppo"://OPPO
//            componentName = new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity");
                    componentName = new ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity");
                    break;

                case "yulong":
                case "360"://360
                    componentName = new ComponentName("com.yulong.android.coolsafe", "com.yulong.android.coolsafe.ui.activity.autorun.AutoRunListActivity");
                    break;

                case "meizu"://魅族
                    componentName = new ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity");
                    break;

                case "oneplus"://一加
                    componentName = new ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity");
                    break;

                case "letv"://乐视
                    intent.setAction("com.letv.android.permissionautoboot");

                default://其他
                    intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                    intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                    break;
            }

            intent.setComponent(componentName);
            context.startActivity(intent);
        } catch (Exception e){
            Log.e("-----HLQ_Struggle-----", e.getLocalizedMessage());

            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            //设置标题
            builder.setTitle("请用户设置自启动");

            //设置对话框内容
            builder.setMessage("设置自启动防止错过消息");

            //设置图标
            builder.setIcon(android.R.drawable.ic_dialog_alert);

            //设置是否可以点击屏幕其他地方或者返回键取消显示
            builder.setCancelable(false);

            //确定按钮
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //该按钮的点击事件，这里设置按钮返回
                    dialog.dismiss();
                }
            });

            //很多朋友都会忘了show
            builder.show();

            intent = new Intent(Settings.ACTION_SETTINGS);
            context.startActivity(intent);
        }
    }
}