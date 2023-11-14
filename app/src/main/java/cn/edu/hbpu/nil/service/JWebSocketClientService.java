package cn.edu.hbpu.nil.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.google.gson.reflect.TypeToken;

import org.java_websocket.handshake.ServerHandshake;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import cn.edu.hbpu.nil.entity.ChatMsg;
import cn.edu.hbpu.nil.entity.MsgCard;
import cn.edu.hbpu.nil.util.NilDBHelper;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.JWebSocketClient;

public class JWebSocketClientService extends Service {
    public JWebSocketClient client;
    private JWebSocketClientBinder mBinder;
    private String mAccount;
    private SharedHelper sh;
    private NilDBHelper nilDBHelper;

    //webSocket消息类型
    public static final String CHAT_MESSAGE = "chatMsg";
    public static final String SYSTEM_TIP = "tip";
    public static final String VER_MESSAGE = "verMsg";
    public static final String VER_AGREED = "verAgreed";
    public static final String RECEIVED_MSG = "received_msg";

    private static final long HEART_BEAT_RATE = 10 * 1000;//每隔10秒进行一次对长连接的心跳检测
    private final Handler wsHandler = new Handler();
    private final Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (client != null) {
                LogUtils.d("心跳包检测webSocket连接状态:" + client.isOpen() + "/" + IUserNetUtil.wsIp + mAccount);
                if (client.isClosed()) {
                    reconnectWs();//心跳机制发现断开开启重连
                }
            } else {
                LogUtils.d("心跳包检测webSocket连接状态重新连接");
                //如果client已为空，重新初始化连接
                client = null;
                initWebSocket();
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            wsHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    //用于Activity和service通讯
    public class JWebSocketClientBinder extends Binder {
        public JWebSocketClientService getService() {
            return JWebSocketClientService.this;
        }
    }

    public JWebSocketClientService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (sh == null) {
            sh = SharedHelper.getInstance(NilApplication.getContext());
            mAccount = sh.getAccount();
        }
        if (mBinder == null) {
            mBinder = new JWebSocketClientBinder();
        }
        if (client == null) {
            initWebSocket();
            wsHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测
        }
        if (nilDBHelper == null) {
            nilDBHelper = NilDBHelper.getInstance(NilApplication.getContext(), sh.getAccount(), 0);
        }
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //调用unbindService和stopService服务结束的时候才会调用onDestroy 此时关闭webSocket连接
        closeConnect();
        if (nilDBHelper.isOpen()) {
            nilDBHelper.closeLink();
        }
    }

    //WebSocket初始化
    private void initWebSocket() {
        URI uri = URI.create(IUserNetUtil.wsIp + mAccount);
        client = new JWebSocketClient(uri) {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onMessage(String message) {
                //message就是接收到的消息
                super.onMessage(message);
                LogUtils.d(message);
                Type type = new TypeToken<ArrayList<String>>(){}.getType();
                List<String> list = GsonUtils.fromJson(message, type);
                String operation = list.get(0);
                String res = list.get(1);
                switch (operation) {
                    case SYSTEM_TIP:
                        LogUtils.d(res);
                        break;
                    case CHAT_MESSAGE:
                        //这里做一下时间戳字符串转时间字符串的操作
                        ChatMsg chatMsg = GsonUtils.fromJson(res, ChatMsg.class);
                        chatMsg.setSendTime(TimeUtils.millis2String(Long.parseLong(chatMsg.getSendTime())));
                        String json = GsonUtils.toJson(chatMsg);
                        //保存本地数据库
                        if (!nilDBHelper.isOpen()) {
                            nilDBHelper.openWriteLink();
                        }
                        nilDBHelper.insert(GsonUtils.fromJson(json, ChatMsg.class));
                        //同步消息卡片
                        synchronizeMsgCard(chatMsg);
                        //接收到就给客户端发个消息
                        List<String> resList = new ArrayList<>();
                        resList.add(RECEIVED_MSG);
                        resList.add(String.valueOf(chatMsg.getMsgId()));
                        sendMsg(GsonUtils.toJson(resList));
                        //广播
                        Intent intent = new Intent();
                        intent.setAction("cn.edu.hbpu.chatmsg");
                        intent.putExtra("message", json);
                        sendBroadcast(intent);
                        break;
                    case VER_MESSAGE:
                        //广播
                        Intent intent1 = new Intent();
                        intent1.setAction("cn.edu.hbpu.vermsg");
                        sendBroadcast(intent1);
                        break;
                    case VER_AGREED:
                        Intent intent2 = new Intent();
                        intent2.setAction("cn.edu.hbpu.veragreed");
                        sendBroadcast(intent2);
                        break;
                }
            }
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                super.onOpen(handshakedata);
                LogUtils.d("webSocket连接成功");
            }

            @Override
            public void onError(Exception ex) {
                super.onError(ex);
                LogUtils.d("webSocket错误：" + ex);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                super.onClose(code, reason, remote);
                if (code!=1000) {
                    reconnectWs();//意外断开马上重连
                }
                LogUtils.d("webSocket断开连接：·code:" + code + "·reason:" + reason + "·remote:" + remote);
            }
        };
        //TODO 设置超时时间
        client.setConnectionLostTimeout(110 * 1000);
        //TODO 连接webSocket
        ThreadUtils.getFixedPool(12).submit(() -> {
            try {
                //connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                client.connectBlocking();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void synchronizeMsgCard(ChatMsg chatMsg) {
        List<MsgCard> cardList = nilDBHelper.card_exists_account(chatMsg.getSendAccount());
        if (!cardList.isEmpty()) {
            MsgCard card = cardList.get(0);
            //成功则返回ID
            MsgCard msgCard = new MsgCard();
            msgCard.setCardId(card.getCardId());
            msgCard.setUnreadNum(card.getUnreadNum() + 1);
            msgCard.setLastTime(chatMsg.getSendTime());
            msgCard.setLastContent(chatMsg.getMsgContent());
            nilDBHelper.card_update_by_id(msgCard);
        } else {
            //没有则插入消息卡片
            MsgCard card = new MsgCard();
            //有备注则显示备注
            if (StringUtils.isEmpty(chatMsg.getNameMem())) {
                card.setSenderName(chatMsg.getSenderName());
            } else {
                card.setSenderName(chatMsg.getNameMem());
            }
            card.setSenderHeader(chatMsg.getSenderHeader());
            card.setSenderAccount(chatMsg.getSendAccount());
            card.setReceiveAccount(sh.getAccount());
            card.setLastContent(chatMsg.getMsgContent());
            card.setLastTime(chatMsg.getSendTime());
            card.setUnreadNum(1);
            nilDBHelper.card_insert_one(card);
        }
    }

    public void synchronizeMsgCardBySelf(ChatMsg chatMsg) {
        List<MsgCard> cardList = nilDBHelper.card_exists_account(chatMsg.getReceiveAccount());
        if (!cardList.isEmpty()) {
            MsgCard card = cardList.get(0);
            //成功则返回ID
            MsgCard msgCard = new MsgCard();
            msgCard.setCardId(card.getCardId());
            msgCard.setLastTime(chatMsg.getSendTime());
            msgCard.setLastContent(chatMsg.getMsgContent());
            nilDBHelper.card_update_by_id(msgCard);
        } else {
            //没有则插入消息卡片
            MsgCard card = new MsgCard();
            if (StringUtils.isEmpty(chatMsg.getNameMem())) {
                card.setSenderName(chatMsg.getSenderName());
            } else {
                card.setSenderName(chatMsg.getNameMem());
            }
            card.setSenderHeader(chatMsg.getSenderHeader());
            card.setSenderAccount(chatMsg.getReceiveAccount());
            card.setReceiveAccount(chatMsg.getSendAccount());
            card.setLastContent(chatMsg.getMsgContent());
            card.setLastTime(chatMsg.getSendTime());
            nilDBHelper.card_insert_one(card);
        }
    }

    /**
     * 发送消息
     *
     * @param msg 消息
     */
    public void sendMsg(String msg) {

        if (null != client) {
            LogUtils.d("发送消息：" + msg);
            if (client.isOpen()) {
                client.send(msg);
            }

        }
    }
    /**
     * 开启重连
     */
    private void reconnectWs() {
        wsHandler.removeCallbacks(heartBeatRunnable);
        ThreadUtils.getFixedPool(12).submit(() -> {
            try {
                Log.e("开启重连", "");
                client.reconnectBlocking();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
    /**
     * 断开连接
     */
    private void closeConnect() {
        try {
            //关闭webSocket
            if (null != client) {
                client.close();
            }
            //停止心跳
            wsHandler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            client = null;
        }
    }
}