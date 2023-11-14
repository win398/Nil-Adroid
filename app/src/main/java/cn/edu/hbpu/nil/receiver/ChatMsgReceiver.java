package cn.edu.hbpu.nil.receiver;

import static com.blankj.utilcode.util.NotificationUtils.IMPORTANCE_HIGH;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NotificationUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.Utils;

import java.io.File;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.AppActivity;
import cn.edu.hbpu.nil.activity.ChatActivity;
import cn.edu.hbpu.nil.entity.ChatMsg;
import cn.edu.hbpu.nil.util.AppUtil;

public class ChatMsgReceiver extends BroadcastReceiver {
    private int count;

    @SuppressLint("UnspecifiedImmutableFlag")
    @Override
    public void onReceive(Context context, Intent intent) {
        String msg = intent.getStringExtra("message");
        ChatMsg chatMsg = GsonUtils.fromJson(msg, ChatMsg.class);
        LogUtils.d(chatMsg);

        if (AppUtils.isAppForeground()) return;
        Intent mIntent = new Intent(context, ChatActivity.class);
        mIntent.putExtra("sender", chatMsg.getSendAccount());
        mIntent.putExtra("contactName", chatMsg.getSenderName());
        mIntent.putExtra("contactHeader", chatMsg.getSenderHeader());
        mIntent.putExtra("mHeader", AppActivity.getUser().getHeader());
        mIntent.putExtra("receiver", chatMsg.getReceiveAccount());
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, count++, mIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(context, count++, mIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        NotificationUtils.notify(Integer.parseInt(chatMsg.getSendAccount()), new NotificationUtils.ChannelConfig(Utils.getApp().getPackageName(), "chat_msg", IMPORTANCE_HIGH), builder -> {
            builder.setSmallIcon(R.drawable.icon_notify)
                    .setContentTitle(chatMsg.getSenderName())
                    .setContentText(chatMsg.getMsgContent())
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)  // 通知类别，"勿扰模式"时系统会决定要不要显示你的通知
                    .setLargeIcon(BitmapFactory.decodeFile(AppUtil.getImgBasePath(context) + File.separator + chatMsg.getSenderHeader()))
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
        });
        if (SPUtils.getInstance().getString("notifySound").equals("ON")) {
            AppUtil.playNotificationRing(context);
        }
    }
}
