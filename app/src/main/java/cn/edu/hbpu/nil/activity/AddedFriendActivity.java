package cn.edu.hbpu.nil.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.LogUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.widget.edittext.ClearEditText;
import com.xuexiang.xui.widget.picker.widget.OptionsPickerView;
import com.xuexiang.xui.widget.picker.widget.builder.OptionsPickerBuilder;

import java.io.File;
import java.io.IOException;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.fragment.GroupContactsFragment;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import es.dmoral.toasty.Toasty;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddedFriendActivity extends AppCompatActivity implements View.OnClickListener {
    private ShapeableImageView added_friend_back, added_friend_header;
    private TextView added_friend_ok, added_friend_username, added_friend_ver_info, added_friend_group_name;
    private ClearEditText added_friend_name_mem;
    private LinearLayout added_friend_select_group, added_friend_container;

    private String[] groups;
    private int groupIndex;
    private String fromUserName, fromUserHeader, verContent;
    private int fromUid, toUid;

    private static final int MODIFY_SUCCESS = 1;
    private static final int REQUEST_FAIL = 2;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MODIFY_SUCCESS:
                    //通知更新联系人数据
                    Intent intent = new Intent();
                    intent.setAction("cn.edu.hbpu.refreshContacts");
                    AddedFriendActivity.this.sendBroadcast(intent);
                    finish();
                    break;
                case REQUEST_FAIL:
                    Toasty.error(AddedFriendActivity.this, "操作失败", Toast.LENGTH_SHORT, true).show();
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUI.initTheme(this);
        setContentView(R.layout.activity_added_friend);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        groups = GroupContactsFragment.getGroupList();
        groupIndex = 0;
        Intent intent = getIntent();
        fromUserName = intent.getStringExtra("fromUserName").intern();
        fromUserHeader = intent.getStringExtra("fromUserHeader").intern();
        verContent = intent.getStringExtra("verContent").intern();
        fromUid = intent.getIntExtra("fromUid", -1);
        toUid = intent.getIntExtra("toUid", -1);
    }

    private void initEvent() {
        added_friend_back.setOnClickListener(this);
        added_friend_select_group.setOnClickListener(this);
        added_friend_ok.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        added_friend_container = findViewById(R.id.added_friend_container);
        added_friend_back = findViewById(R.id.added_friend_back);
        added_friend_header = findViewById(R.id.added_friend_header);
        added_friend_ok = findViewById(R.id.added_friend_ok);
        added_friend_username = findViewById(R.id.added_friend_username);
        added_friend_ver_info = findViewById(R.id.added_friend_ver_info);
        added_friend_group_name = findViewById(R.id.added_friend_group_name);
        added_friend_name_mem = findViewById(R.id.added_friend_name_mem);
        added_friend_select_group = findViewById(R.id.added_friend_select_group);

        //设置默认值
        added_friend_ver_info.setText(fromUserName);
        if (groups.length > 0) {
            added_friend_group_name.setText(groups[groupIndex]);
        }
        added_friend_username.setText(fromUserName);
        added_friend_ver_info.setText(verContent);
        //头像加载
        loadFriendHeader();
        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        added_friend_container.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

    private void loadFriendHeader() {
        Glide.with(this)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(IUserNetUtil.picIp + File.separator + fromUserHeader)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        added_friend_header.setImageDrawable(resource);
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.added_friend_back:
                finish();
                break;
            case R.id.added_friend_select_group:
                selectGroup();
                break;
            case R.id.added_friend_ok:
                setContactInfo();
                break;
        }
    }

    private static class FriendData {
        //注意：此处的groupIndex并不包括0组，所以对应服务端应从1开始计数
        public FriendData(int fromUid, int toUid, int groupIndex, String nameMem) {
            this.contactId = fromUid;
            this.userId = toUid;
            this.groupIndex = groupIndex + 1;
            this.nameMem = nameMem;
        }

        int contactId, userId, groupIndex;
        String nameMem;
    }

    private void setContactInfo() {
        //有0和-1两种情况
        if (fromUid <= 0 || toUid <= 0) return;
        String nameMem = String.valueOf(added_friend_name_mem.getText());
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.setContactGroupAndName(new FriendData(fromUid, toUid, groupIndex, nameMem));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        if (res.equals("success")) {
                            sendMsg(MODIFY_SUCCESS);
                        } else {
                            sendMsg(REQUEST_FAIL);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    sendMsg(REQUEST_FAIL);
                    LogUtils.e("连接错误:" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                sendMsg(REQUEST_FAIL);
                LogUtils.e("连接失败" + t);
            }
        });
    }

    public void sendMsg(int state) {
        if (ActivityUtils.isActivityAlive(AddedFriendActivity.this)) {
            Message msg = Message.obtain();
            msg.what = state;
            handler.sendMessage(msg);
        }
    }

    //分组选择
    private void selectGroup() {
        OptionsPickerView<String> pvOptions = new OptionsPickerBuilder(AddedFriendActivity.this, (view, options1, options2, options3) -> {
            String tx = groups[options1].intern();
            //ToastUtils.showShort(tx);
            groupIndex = options1;
            added_friend_group_name.setText(tx);
            return false;
        })
                .setTitleText("分组选择")
                .setDividerColor(Color.LTGRAY)
                .setTextColorCenter(Color.BLACK) //设置选中项文字颜色
                .setContentTextSize(16)
                .setTitleSize(16)
                .setSubCalSize(16)
                .isDialog(false)
                .setSelectOptions(0)
                .build();

        pvOptions.setPicker(groups);
        pvOptions.show();
    }
}