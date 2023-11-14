package cn.edu.hbpu.nil.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.reflect.TypeToken;
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
import cn.edu.hbpu.nil.adapter.NewFriendAdapter;
import cn.edu.hbpu.nil.entity.FriendVerification;
import cn.edu.hbpu.nil.entity.MsgCard;
import cn.edu.hbpu.nil.util.NilDBHelper;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewFriendActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout new_friend_container, null_new_friend;
    private ShapeableImageView new_friend_back;
    private SwipeRecyclerView rv_new_friend;
    private LinearLayoutManager layoutManager;
    private NewFriendAdapter mAdapter;
    private List<FriendVerification>  verifications;
    private SharedHelper sh;
    private NilDBHelper nilDBHelper;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_friend);
        initData();
        initView();
        initEvent();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (nilDBHelper.isOpen()) {
            nilDBHelper.closeLink();
        }
    }

    private void initData() {
        sh = SharedHelper.getInstance(NilApplication.getContext());
        nilDBHelper = NilDBHelper.getInstance(NilApplication.getContext(), sh.getAccount(), 0);
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openReadLink();
        }
        //获取本地数据
        verifications = nilDBHelper.queryAllVer();
        //标记已读未处理
        nilDBHelper.updateVer2();
        serverUpdateVer2(sh.getUid());
    }

    private void serverUpdateVer2(int uid) {
        if (uid == -1) return;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.setFlagHasRead(uid);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    LogUtils.d("云端标记已读成功");
                } else {
                    LogUtils.e("请求错误，错误码" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败" + t);
            }
        });
    }

    private void initEvent() {
        new_friend_back.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        new_friend_container = findViewById(R.id.new_friend_container);
        new_friend_back = findViewById(R.id.new_friend_back);
        rv_new_friend = findViewById(R.id.rv_new_friend);
        null_new_friend = findViewById(R.id.null_new_friend);

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        new_friend_container.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
        //数据为空则显示空提示
        checkEmpty();
        //RecycleView初始化
        rvInit();
    }

    private void checkEmpty() {
        if (verifications == null || verifications.isEmpty()) {
            null_new_friend.setVisibility(View.VISIBLE);
        } else {
            null_new_friend.setVisibility(View.GONE);
        }
    }

    private void rvInit() {
        //设置rv侧滑菜单
        rv_new_friend.setSwipeMenuCreator(swipeMenuCreator);
        rv_new_friend.setOnItemMenuClickListener(mItemMenuClickListener);
        layoutManager = new LinearLayoutManager(NewFriendActivity.this);
        rv_new_friend.setLayoutManager(layoutManager);
        mAdapter = new NewFriendAdapter(verifications, NewFriendActivity.this);
        rv_new_friend.setAdapter(mAdapter);
    }
    //rv侧滑菜单
    SwipeMenuCreator swipeMenuCreator = (leftMenu, rightMenu, position) -> {
        SwipeMenuItem deleteItem = new SwipeMenuItem(NewFriendActivity.this)
                .setBackground(com.xuexiang.xui.R.color.xui_config_color_red)
                .setTextSize(14)
                .setHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,76, getResources().getDisplayMetrics()))
                .setWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,14 * 4, getResources().getDisplayMetrics()))
                .setTextColorResource(R.color.white)
                .setText(R.string.delete);
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
            FriendVerification verification = verifications.get(position);
            if (!nilDBHelper.isOpen()) {
                nilDBHelper.openWriteLink();
            }
            //ps:如果没联网将云端数据置为已读，删除后会重新加载该条验证信息
            if (menuPosition == 0) {
                nilDBHelper.deleteByVerId(verification.getVerificationId());
                verifications.remove(position);
                mAdapter.notifyItemRemoved(position);
                checkEmpty();
            }
        }
    };

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_friend_back:
                finish();
                break;

        }
    }
}