package cn.edu.hbpu.nil.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.KeyboardUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.NotificationUtils;
import com.blankj.utilcode.util.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.edu.hbpu.nil.service.JWebSocketClientService;
import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.adapter.ChatAdapter;
import cn.edu.hbpu.nil.entity.ChatMsg;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.other.TimeUtil;
import cn.edu.hbpu.nil.util.web.JWebSocketClient;
import cn.edu.hbpu.nil.util.NilDBHelper;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout go_back, chat_nav, chat_container;
    private List<ChatMsg> chatMsgList;
    private NilDBHelper nilDBHelper;
    private String mAccount, senderAccount, contactName, contactHeader, mHeader, state;
    private ChatAdapter chatAdapter;
    private RecyclerView recyclerView;
    private TextView chat_obj_name, chat_obj_state;
    private ImageView chat_obj_online, chat_obj_not_online;

    private Button btn_send;
    private EditText chat_msg_content;

    private static final int SEND_MESSAGE = 0;
    private static final int UPDATE_MESSAGE_UI = 10;


    private JWebSocketClientService jWSClientService;
    private boolean isBind = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogUtils.d("服务与活动成功绑定");
            JWebSocketClientService.JWebSocketClientBinder binder = (JWebSocketClientService.JWebSocketClientBinder) iBinder;
            jWSClientService = binder.getService();
            //绑定webSocket服务
            JWebSocketClient client = jWSClientService.client;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogUtils.d("服务与活动成功断开");
        }
    };

    //注册广播接收消息
    private class ChatMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("message");
            ChatMsg chatMsg = GsonUtils.fromJson(msg, ChatMsg.class);
            //更新UI
            Message message = Message.obtain();
            message.what = UPDATE_MESSAGE_UI;
            message.obj = chatMsg;
            mHandler.sendMessage(message);
        }
    }
    //动态注册广播
    private void doRegisterReceiver() {
        ChatMessageReceiver chatMessageReceiver = new ChatMessageReceiver();
        IntentFilter filter = new IntentFilter("cn.edu.hbpu.chatmsg");
        registerReceiver(chatMessageReceiver, filter);
    }

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SEND_MESSAGE:
                    //保存消息到本地数据库并更新UI
                    ChatMsg chatMsg = (ChatMsg) msg.obj;
                    chatMsg.setSendTime(TimeUtil.getTimeNow());
                    if (!nilDBHelper.isOpen()) {
                        nilDBHelper.openWriteLink();
                    }
                    nilDBHelper.insert(chatMsg);
                    chatMsgList.add(chatMsg);
                    chatAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(chatAdapter.getItemCount()-1);
                    chat_msg_content.setText("");
                    //更新msgCard
                    chatMsg.setSenderHeader(contactHeader);
                    chatMsg.setSenderName(contactName);
                    jWSClientService.synchronizeMsgCardBySelf(chatMsg);
                    break;
                case UPDATE_MESSAGE_UI:
                    ChatMsg chatMsg1 = (ChatMsg) msg.obj;
                    chatMsgList.add(chatMsg1);
                    chatAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                    if (!nilDBHelper.isOpen()) {
                        nilDBHelper.openWriteLink();
                    }
                    nilDBHelper.clear_unread_by_account(senderAccount);
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initData();
        initView();
        initEvent();
        //WebSocket
        bindService();
        doRegisterReceiver();
    }


    private void bindService() {
        Intent bindIntent = new Intent(ChatActivity.this, JWebSocketClientService.class);
        //BIND_AUTO_CREATE 自动创建服务只要连接存在
        isBind = bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void initData() {

        Intent intent = getIntent();
        senderAccount = intent.getStringExtra("sender");
        contactName = intent.getStringExtra("contactName");
        contactHeader = intent.getStringExtra("contactHeader");
        mHeader = intent.getStringExtra("mHeader");
        mAccount = intent.getStringExtra("receiver");

        nilDBHelper = NilDBHelper.getInstance(NilApplication.getContext(), mAccount, 0);
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openWriteLink();
        }
        //查询全部
        chatMsgList = nilDBHelper.queryAllBy2(senderAccount, mAccount);
        //进入聊天界面后 与该联系人的聊天未读置为0
        nilDBHelper.clear_unread_by_account(senderAccount);
        state = nilDBHelper.getStateByAccount(senderAccount);

        List<Activity> activityList = ActivityUtils.getActivityList();
        for (Activity activity : activityList) {
            if (activity instanceof AppActivity) {
                ((AppActivity) activity).showOrUpdateMsgBadge();
            }
        }
    }

    private void initEvent() {
        go_back.setOnClickListener(this);
        btn_send.setOnClickListener(this);

        //注册软键盘监听
        KeyboardUtils.registerSoftInputChangedListener(ChatActivity.this, new KeyboardUtils.OnSoftInputChangedListener() {
            @Override
            public void onSoftInputChanged(int height) {
                if (chatAdapter != null && recyclerView != null && chatAdapter.getItemCount() > 1) {
                    recyclerView.scrollToPosition(chatAdapter.getItemCount()-1);
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        go_back = findViewById(R.id.go_back);
        recyclerView = findViewById(R.id.rv_chat);
        btn_send = findViewById(R.id.btn_send);
        chat_msg_content = findViewById(R.id.chat_msg_content);
        chat_nav = findViewById(R.id.chat_nav);
        chat_container = findViewById(R.id.chat_container);
        chat_obj_name = findViewById(R.id.chat_obj_name);
        chat_obj_state = findViewById(R.id.chat_obj_state);
        chat_obj_online = findViewById(R.id.chat_obj_online);
        chat_obj_not_online = findViewById(R.id.chat_obj_not_online);

        chat_obj_name.setText(contactName);
        if (state.equals("在线")) {
            chat_obj_state.setText(state);
            chat_obj_online.setVisibility(View.VISIBLE);
            chat_obj_not_online.setVisibility(View.GONE);
        } else {

            chat_obj_state.setText("离线");
            chat_obj_online.setVisibility(View.GONE);
            chat_obj_not_online.setVisibility(View.VISIBLE);
        }
        bindAdapter();
        //取消通知
        try {
            NotificationUtils.cancel(Integer.parseInt(senderAccount));
        } catch (Exception ignored) {

        }
        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        chat_container.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.go_back:
                goBack();
                break;
            case R.id.btn_send:
                String text = chat_msg_content.getText().toString();
                if (!TextUtils.isEmpty(text)) {
                    sendMsg(text);
                } else {
                    ToastUtils.showShort("消息内容不能为空");
                }
                break;
        }
    }

    private void goBack() {
        List<Activity> activityList = ActivityUtils.getActivityList();
        for (Activity activity : activityList) {
            if (activity instanceof AppActivity) {
                ((AppActivity) activity).cardDataChanged();
                break;
            }
        }
        finish();
    }

    //重写系统返回
    public boolean onKeyDown(int keyCode,KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            goBack();
        }
        return false;
    }

    private void sendMsg(String text) {
        ChatMsg chatMsg = new ChatMsg();
        chatMsg.setMsgContent(text);
        chatMsg.setSendAccount(mAccount);
        chatMsg.setReceiveAccount(senderAccount);
        chatMsg.setSendTime(TimeUtil.getTimeNow());
        List<String> list = new ArrayList<>();
        list.add(JWebSocketClientService.CHAT_MESSAGE);
        list.add(GsonUtils.toJson(chatMsg));
        String json = GsonUtils.toJson(list);
        jWSClientService.sendMsg(json);
        //更新UI
        Message msg = Message.obtain();
        msg.what = SEND_MESSAGE;
        msg.obj = chatMsg;
        mHandler.sendMessage(msg);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (nilDBHelper.isOpen()) {
            nilDBHelper.closeLink();
        }
        //解绑服务
        if (isBind) {
            unbindService(serviceConnection);
            isBind = false;
        }
        stopService(new Intent(ChatActivity.this, JWebSocketClientService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        //取消通知
        try {
            NotificationUtils.cancel(Integer.parseInt(senderAccount));
        } catch (Exception ignored) {

        }
    }

    private void bindAdapter() {
        chatAdapter = new ChatAdapter(chatMsgList, this, mAccount, mHeader, contactHeader);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);
        recyclerView.scrollToPosition(chatAdapter.getItemCount()-1);
    }

}