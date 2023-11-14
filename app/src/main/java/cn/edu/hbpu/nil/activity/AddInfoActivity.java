package cn.edu.hbpu.nil.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.card.MaterialCardView;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.widget.edittext.ClearEditText;
import com.xuexiang.xui.widget.picker.widget.OptionsPickerView;
import com.xuexiang.xui.widget.picker.widget.builder.OptionsPickerBuilder;
import com.xuexiang.xui.widget.picker.widget.listener.OnOptionsSelectListener;

import java.io.IOException;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.entity.FriendVerification;
import cn.edu.hbpu.nil.fragment.GroupContactsFragment;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.NilDBHelper;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.MaskUtil;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.other.TimeUtil;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import es.dmoral.toasty.Toasty;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddInfoActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout select_group;
    private TextView add_cancel, add_tv_group;
    private ClearEditText cet_validation_info, cet_mem_name;
    private MaterialCardView btn_send_info;
    private RelativeLayout add_nav_container;

    private int fromUserId, toUserId, groupIndex;
    private String[] groups;
    private String fromUserName, toUserName, toUserHeader;
    private ProgressDialog progressDialog;
    private NilDBHelper nilDBHelper;
    private SharedHelper sh;

    private static final int SEND_INFO_FAIL = 0;
    private static final int SEND_INFO_SUCCESS = 1;
    private static final int SEND_INFO_IMPERFECT = 2;

    private Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SEND_INFO_IMPERFECT:
                    progressDialog.dismiss();
                    Toasty.error(AddInfoActivity.this, "数据异常", Toast.LENGTH_SHORT, true).show();
                    break;
                case SEND_INFO_SUCCESS:
                    progressDialog.dismiss();
                    Toasty.success(AddInfoActivity.this, "发送成功", Toast.LENGTH_SHORT, true).show();
                    Intent intent = new Intent();
                    intent.setAction("cn.edu.hbpu.vermsg");
                    sendBroadcast(intent);
                    finish();
                    break;
                case SEND_INFO_FAIL:
                    progressDialog.dismiss();
                    Toasty.error(AddInfoActivity.this, "发送失败", Toast.LENGTH_SHORT, true).show();
                    break;
            }
        }
    };

    private void sendMsg(int state) {
        //先判断活动是否空
        if (ActivityUtils.isActivityAlive(AddInfoActivity.this)) {
            Message msg = Message.obtain();
            msg.what = state;
            handler.sendMessage(msg);
        }
    }

    private void sendMsg(int state, Object obj) {
        //先判断活动是否空
        if (ActivityUtils.isActivityAlive(AddInfoActivity.this)) {
            Message msg = Message.obtain();
            msg.what = state;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUI.initTheme(this);
        setContentView(R.layout.activity_add_info);
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
        Intent intent = getIntent();
        fromUserId = intent.getIntExtra("fromUserId", -1);
        toUserId = intent.getIntExtra("toUserId", -1);
        fromUserName = intent.getStringExtra("fromUserName").intern();
        toUserHeader = intent.getStringExtra("toUserHeader");
        toUserName = intent.getStringExtra("toUserName");
        groups =  GroupContactsFragment.getGroupList();

        groupIndex = 0;
        sh = SharedHelper.getInstance(NilApplication.getContext());
        nilDBHelper = NilDBHelper.getInstance(NilApplication.getContext(), sh.getAccount(), 0);
    }

    private void initEvent() {
        add_cancel.setOnClickListener(this);
        select_group.setOnClickListener(this);
        btn_send_info.setOnClickListener(this);
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        add_nav_container = findViewById(R.id.add_nav_container);
        add_cancel = findViewById(R.id.add_cancel);
        select_group = findViewById(R.id.select_group);
        add_tv_group = findViewById(R.id.add_tv_group);
        cet_validation_info = findViewById(R.id.cet_validation_info);
        btn_send_info = findViewById(R.id.btn_send_info);
        cet_mem_name = findViewById(R.id.cet_mem_name);

        //设置默认值
        cet_validation_info.setText("我是" + fromUserName);
        if (groups.length > 0) {
            add_tv_group.setText(groups[groupIndex]);
        }

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        add_nav_container.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add_cancel:
                finish();
                break;
            case R.id.select_group:
                selectGroup();
                break;
            case R.id.btn_send_info:
                progressDialog = MaskUtil.showProgressDialog("发送验证信息...", AddInfoActivity.this);
                progressDialog.show();
                if (fromUserId == -1 || toUserId == -1) {
                    sendMsg(SEND_INFO_IMPERFECT);
                } else {
                    String ver_info = "", mem_name = "";
                    if (cet_validation_info.getText() != null) {
                        ver_info = cet_validation_info.getText().toString().trim().intern();
                    }
                    if (cet_mem_name.getText() != null) {
                        mem_name = cet_mem_name.getText().toString().trim().intern();
                    }
                    //注意是groupIndex + 1  特别关心在0组 这里获取的groups不包括特别关心组
                    FriendVerification verification = new FriendVerification(fromUserId, toUserId, ver_info, mem_name, groupIndex + 1);
                    //LogUtils.d("发送数据" + verification);
                    sendInfo(verification);
                }
                break;
        }
    }

    private void sendInfo(FriendVerification verification) {
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.sendInfo(verification);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string().intern();
                        if (res.equals("success")) {
                            sendMsg(SEND_INFO_SUCCESS, verification);
                        } else {
                            sendMsg(SEND_INFO_FAIL);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtils.e("连接错误:" + response.code());
                    sendMsg(SEND_INFO_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败:" + t);
                sendMsg(SEND_INFO_FAIL);
            }
        });
    }

    //分组选择
    private void selectGroup() {
        OptionsPickerView<String> pvOptions = new OptionsPickerBuilder(AddInfoActivity.this, (view, options1, options2, options3) -> {
            String tx = groups[options1].intern();
            //ToastUtils.showShort(tx);
            groupIndex = options1;
            add_tv_group.setText(tx);
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