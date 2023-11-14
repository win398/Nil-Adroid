package cn.edu.hbpu.nil.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.reflect.TypeToken;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.constant.RefreshState;
import com.yanzhenjie.recyclerview.OnItemMenuClickListener;
import com.yanzhenjie.recyclerview.SwipeMenuBridge;
import com.yanzhenjie.recyclerview.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.SwipeMenuItem;
import com.yanzhenjie.recyclerview.SwipeRecyclerView;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.AppActivity;
import cn.edu.hbpu.nil.activity.MsgSearchActivity;
import cn.edu.hbpu.nil.activity.PersonalActivity;
import cn.edu.hbpu.nil.adapter.MsgAdapter;
import cn.edu.hbpu.nil.entity.ChatMsg;
import cn.edu.hbpu.nil.entity.MsgCard;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.NilDBHelper;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MsgFragment extends Fragment implements View.OnClickListener {
    private AppActivity mActivity;
    private ImageView msg_fun;
    private View mView;
    private EditText msg_search;
    private ImageButton msg_btn_search;
    private SwipeRecyclerView msg_rv;
    private List<MsgCard> mData;
    private MsgAdapter msgAdapter;
    private ShapeableImageView header;
    private LinearLayout mContainer, msg_empty;

    private NilDBHelper nilDBHelper;
    private SharedHelper sh;

    private RefreshLayout msg_refresh;

    private static final int REFRESH_SUCCESS = 0;
    private static final int REFRESH_FAIL = 1;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH_SUCCESS:
                    List<ChatMsg> msgList = AppUtil.castList(msg.obj, ChatMsg.class);
                    if (msgList != null && !msgList.isEmpty()) {
                        if (!nilDBHelper.isOpen()) {
                            nilDBHelper.openWriteLink();
                        }
                        nilDBHelper.insert(msgList);
                        synchronizeMsgCard(msgList);
                        //数据处理完成  通知UI更新
                        dataChanged();
                    }
                    mActivity.showOrUpdateMsgBadge();
                    if (msg_refresh.getState() == RefreshState.Refreshing) {
                        msg_refresh.finishRefresh(true);
                    }
                    break;
                case REFRESH_FAIL:
                    if (msg_refresh.getState() == RefreshState.Refreshing) {
                        //设置加载失败
                        msg_refresh.finishRefresh(false);
                    }
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fg_content_msg, container, false);
        initData();
        initView();
        initEvent();
        return mView;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (nilDBHelper.isOpen()) {
            nilDBHelper.closeLink();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("ResourceAsColor")
    private void initView() {
        mActivity = (AppActivity) getActivity();
        msgAdapter = new MsgAdapter(mData, mActivity, nilDBHelper);

        msg_fun = mView.findViewById(R.id.msg_fun);
        msg_search = mView.findViewById(R.id.msg_search);
        msg_btn_search = mView.findViewById(R.id.msg_btn_search);
        msg_empty = mView.findViewById(R.id.msg_empty);
        msg_rv = mView.findViewById(R.id.msg_rv);
        header = mView.findViewById(R.id.msg_header);
        mContainer = mView.findViewById(R.id.content_msg);
        msg_refresh = mView.findViewById(R.id.msg_refresh);


        mContainer.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
        //头像
        loadHeader();
        //消息卡片列表不空 隐藏空信息控件
        checkDataIsEmpty();
        //设置rv侧滑菜单
        msg_rv.setSwipeMenuCreator(swipeMenuCreator);
        msg_rv.setOnItemMenuClickListener(mItemMenuClickListener);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(mActivity);
        msg_rv.setLayoutManager(linearLayoutManager);
        msg_rv.setAdapter(msgAdapter);

        initRefreshLayout();
        //更新消息
        refreshMsg();
    }



    private void checkDataIsEmpty() {
        if (!mData.isEmpty()) {
            msg_empty.setVisibility(View.GONE);
        } else {
            msg_empty.setVisibility(View.VISIBLE);
        }
    }

    //rv侧滑菜单
    SwipeMenuCreator swipeMenuCreator = (leftMenu, rightMenu, position) -> {
        SwipeMenuItem readItem = new SwipeMenuItem(getContext())
                .setBackground(R.color.blue_light)
                .setTextColorResource(R.color.white)
                .setHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,70, getResources().getDisplayMetrics()))
                .setWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,14 * 6, getResources().getDisplayMetrics()))
                .setTextSize(14)
                .setText(R.string.read);
        SwipeMenuItem deleteItem = new SwipeMenuItem(getContext())
                .setBackground(com.xuexiang.xui.R.color.xui_config_color_red)
                .setTextSize(14)
                .setHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,70, getResources().getDisplayMetrics()))
                .setWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,14 * 4, getResources().getDisplayMetrics()))
                .setTextColorResource(R.color.white)
                .setText(R.string.delete);
        rightMenu.addMenuItem(readItem);
        rightMenu.addMenuItem(deleteItem);
    };

    //菜单点击
    OnItemMenuClickListener mItemMenuClickListener = new OnItemMenuClickListener() {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onItemClick(SwipeMenuBridge menuBridge, int position) {
            // 任何操作必须先关闭菜单，否则可能出现Item菜单打开状态错乱。
            menuBridge.closeMenu();
            // 菜单在Item中的Position：
            int menuPosition = menuBridge.getPosition();
            MsgCard msgCard = mData.get(position);
            if (!nilDBHelper.isOpen()) {
                nilDBHelper.openWriteLink();
            }
            switch (menuPosition) {
                case 0:
                    nilDBHelper.clear_unread_by_id(msgCard.getCardId());
                    msgCard.setUnreadNum(0);
                    msgAdapter.notifyItemChanged(position);
                    //更新导航栏消息条数
                    mActivity.showOrUpdateMsgBadge();
                    break;
                case 1:
                    nilDBHelper.card_delete_by_id(msgCard.getCardId());
                    mData.remove(position);
                    msgAdapter.notifyItemRemoved(position);
                    mActivity.showOrUpdateMsgBadge();
                    checkDataIsEmpty();
                    break;
            }
        }
    };


    private void initEvent() {
        msg_fun.setOnClickListener(this);
        msg_search.setOnClickListener(this);
        msg_btn_search.setOnClickListener(this);
        header.setOnClickListener(this);
    }

    public void loadHeader() {
        if (mActivity.headerPath == null || mActivity.headerPath.equals(""))  return;
        Glide.with(this)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(mActivity.headerPath)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        header.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }
    private void initData() {
        sh = SharedHelper.getInstance(getContext());
        nilDBHelper = NilDBHelper.getInstance(NilApplication.getContext(), sh.getAccount(), 0);
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openWriteLink();
        }
        mData = nilDBHelper.card_query_all_by_time();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void dataChanged() {
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openReadLink();
        }
        mData.clear();
        mData.addAll(nilDBHelper.card_query_all_by_time());
        LogUtils.d("更新消息卡片" + mData);
        msgAdapter.notifyDataSetChanged();
        if (!mData.isEmpty()) {
            msg_empty.setVisibility(View.GONE);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.msg_fun:
                mActivity.openDrawer();
                break;
            case R.id.msg_search:
            case R.id.msg_btn_search:
                startActivity(new Intent(getContext(), MsgSearchActivity.class));
                //LogUtils.d(ActivityUtils.getActivityList());
                break;
            case R.id.msg_header:
                Intent intent = new Intent(mActivity, PersonalActivity.class);
                intent.putExtra("type", PersonalActivity.PERSONAL_SELF);
                startActivity(intent);
                break;
        }
    }

    private void initRefreshLayout() {
        msg_refresh.setOnRefreshListener(refreshLayout -> refreshMsg());
    }

    private void refreshMsg() {
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.refreshConverse(sh.getAccount());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        Type type = new TypeToken<ArrayList<ChatMsg>>(){}.getType();
                        List<ChatMsg> list = GsonUtils.fromJson(res, type);
                        sendMsg(REFRESH_SUCCESS, list);

                    } catch (IOException e) {
                        e.printStackTrace();
                        sendMsg(REFRESH_FAIL);
                    }

                } else {
                    LogUtils.e("连接错误：" + response.code());
                    sendMsg(REFRESH_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败");
                sendMsg(REFRESH_FAIL);
            }
        });
    }

    private void sendMsg(int state, Object obj) {
        if (ActivityUtils.isActivityAlive(mActivity)) {
            Message msg = Message.obtain();
            msg.what = state;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }

    private void sendMsg(int state) {
        if (ActivityUtils.isActivityAlive(mActivity)) {
            Message msg = Message.obtain();
            msg.what = state;
            handler.sendMessage(msg);
        }
    }

    //同步信息到消息卡片
    private void synchronizeMsgCard(List<ChatMsg> msgList) {
        for (ChatMsg chatMsg : msgList) {
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
                    card.setSenderName(chatMsg.getSenderName());
                    card.setSenderHeader(chatMsg.getSenderHeader());
                    card.setSenderAccount(chatMsg.getSendAccount());
                    card.setReceiveAccount(sh.getAccount());
                    card.setLastContent(chatMsg.getMsgContent());
                    card.setLastTime(chatMsg.getSendTime());
                    card.setUnreadNum(1);
                    nilDBHelper.card_insert_one(card);
                }
            }
    }

}
