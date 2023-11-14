package cn.edu.hbpu.nil.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.widget.button.roundbutton.RoundButton;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.entity.User;
import cn.edu.hbpu.nil.fragment.GroupContactsFragment;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.MaskUtil;
import cn.edu.hbpu.nil.util.other.TimeUtil;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddContactActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageButton btn_back_contact_add, btn_contact_add;
    private ImageView contact_add_et_clear;
    private EditText et_contact_add;
    private LinearLayout contact_add_container, addContactLoadingView, addContactBlankView;
    private RelativeLayout addContactUserView;
    //搜索成功  搜索结果空  网络请求错误
    private static final int SEARCH_SUCCESS = 1;
    private static final int SEARCH_EMPTY = 2;
    private static final int NET_ERROR = 3;
    private static final int LOAD_VALIDATION_INFORMATION = 4;

    private User fromUser, toUser;
    private ProgressDialog progressDialog;

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SEARCH_EMPTY:
                    addContactLoadingView.setVisibility(View.GONE);
                    addContactBlankView.setVisibility(View.VISIBLE);
                    addContactUserView.setVisibility(View.GONE);
                    break;
                case NET_ERROR:
                    addContactLoadingView.setVisibility(View.GONE);
                    addContactBlankView.setVisibility(View.GONE);
                    addContactUserView.setVisibility(View.GONE);
                    ToastUtils.showShort("网络异常");
                    break;
                case SEARCH_SUCCESS:
                    addContactLoadingView.setVisibility(View.GONE);
                    addContactBlankView.setVisibility(View.GONE);
                    addContactUserView.setVisibility(View.VISIBLE);
                    User user = (User) msg.obj;
                    Log.d("SEARCH_SUCCESS--", user.toString());
                    showUserInfo(user);
                    toUser = user;
                    break;
                case LOAD_VALIDATION_INFORMATION:
                    if (fromUser != null && toUser != null) {
                        Intent intent = new Intent(AddContactActivity.this, AddInfoActivity.class);
                        intent.putExtra("toUserId", toUser.getUid());
                        intent.putExtra("fromUserId", fromUser.getUid());
                        intent.putExtra("fromUserName", fromUser.getUserName());
                        intent.putExtra("toUserName", toUser.getUserName());
                        intent.putExtra("toUserHeader", toUser.getHeader());
                        startActivity(intent);
                    } else {
                        ToastUtils.showShort("数据拉取失败");
                    }
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    break;
            }
        }
    };

    @SuppressLint("SetTextI18n")
    private void showUserInfo(User user) {
        //初始化资源
        ShapeableImageView user_card_header;
        TextView user_card_username, user_card_locality, user_card_sex, user_card_age;
        LinearLayout user_card_info_container;
        RoundButton user_card_btn_add, user_card_btn_send_msg;
        user_card_header = addContactUserView.findViewById(R.id.user_card_header);
        user_card_username = addContactUserView.findViewById(R.id.user_card_username);
        user_card_locality = addContactUserView.findViewById(R.id.user_card_locality);
        user_card_sex = addContactUserView.findViewById(R.id.user_card_sex);
        user_card_age = addContactUserView.findViewById(R.id.user_card_age);
        user_card_info_container = addContactUserView.findViewById(R.id.user_card_info_container);
        user_card_btn_add = addContactUserView.findViewById(R.id.user_card_btn_add);
        user_card_btn_send_msg = addContactUserView.findViewById(R.id.user_card_btn_send_msg);


        user_card_btn_add.setOnClickListener(this);
        user_card_btn_send_msg.setOnClickListener(this);

        if (user != null) {
            //如果已经是好友则显示发消息按钮
            if (user.isFriend()) {
                user_card_btn_add.setVisibility(View.GONE);
                user_card_btn_send_msg.setVisibility(View.VISIBLE);
            } else if(user.getUid() == fromUser.getUid()) {
                user_card_btn_add.setVisibility(View.GONE);
                user_card_btn_send_msg.setVisibility(View.GONE);
            }
            //用户名头像
            user_card_username.setText(user.getUserName());
            Glide.with(this)
                    .setDefaultRequestOptions(new RequestOptions()
                            .centerCrop()
                            .placeholder(R.mipmap.loadingheader)
                            .fitCenter()
                    )
                    .load(IUserNetUtil.picIp + File.separator + user.getHeader())
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            user_card_header.setImageDrawable(resource);
                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
            //用户其他信息可以为空，为空则不显示
            if (!StringUtils.isEmpty(user.getSex()) || !StringUtils.isEmpty(user.getCity()) || !StringUtils.isEmpty(user.getProvince()) || !StringUtils.isEmpty(user.getBirth())) {
                user_card_info_container.setVisibility(View.VISIBLE);
                if (!StringUtils.isEmpty(user.getSex())) {
                    user_card_sex.setText(user.getSex());
                }
                if (!StringUtils.isEmpty(user.getBirth())) {
                    Date date = TimeUtils.string2Date(user.getBirth());
                    //根据生日获取年龄
                    user_card_age.setText(TimeUtil.getAge(date) + "岁");
                }
                if (!StringUtils.isEmpty(user.getProvince()) || !StringUtils.isEmpty(user.getCity())) {
                    String locality;
                    if (!StringUtils.isEmpty(user.getCity()) && StringUtils.isEmpty(user.getProvince())) {
                        locality = user.getCity();
                    } else if (StringUtils.isEmpty(user.getCity()) && !StringUtils.isEmpty(user.getProvince())) {
                        locality = user.getProvince();
                    } else {
                        locality = user.getProvince() + " " + user.getCity();
                    }
                    user_card_locality.setText(locality);
                }
            } else {
                user_card_info_container.setVisibility(View.GONE);
            }
        }

        //点击卡片进入用户主页
        addContactUserView.setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUI.initTheme(this);
        setContentView(R.layout.activity_add_contact);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        fromUser = AppActivity.getUser();
    }

    private void initEvent() {
        btn_back_contact_add.setOnClickListener(this);
        //监听输入框内容变化，显示搜索结果，文本长度大于0显示清空按钮
        et_contact_add.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                //输入框不为空显示清除按钮
                if (et_contact_add.getText().toString().equals("")) {
                    contact_add_et_clear.setVisibility(View.GONE);
                } else {
                    contact_add_et_clear.setVisibility(View.VISIBLE);
                }
            }
        });
        contact_add_et_clear.setOnClickListener(this);
        btn_contact_add.setOnClickListener(this);
        //回车开始搜索
        et_contact_add.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (keyEvent != null && KeyEvent.KEYCODE_ENTER == keyEvent.getKeyCode() && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                startSearchUser();
            }
            return false;
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        btn_back_contact_add = findViewById(R.id.btn_back_contact_add);
        contact_add_et_clear = findViewById(R.id.contact_add_et_clear);
        btn_contact_add = findViewById(R.id.btn_contact_add);
        et_contact_add = findViewById(R.id.et_contact_add);
        contact_add_container = findViewById(R.id.contact_add_container);
        addContactLoadingView = findViewById(R.id.addContactLoadingView);
        addContactBlankView = findViewById(R.id.addContactBlankView);
        addContactUserView = findViewById(R.id.addContactUserView);

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        contact_add_container.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.contact_add_et_clear:
                //清空输入框
                et_contact_add.setText("");
                break;
            case R.id.btn_back_contact_add:
                //返回
                finish();
                break;
            case R.id.btn_contact_add:
                startSearchUser();
                break;
            case R.id.user_card_btn_add:
                //ToastUtils.showShort("添加用户按钮");
                progressDialog = MaskUtil.showProgressDialog("拉取验证信息...", AddContactActivity.this);
                progressDialog.show();
                sendMsg(LOAD_VALIDATION_INFORMATION);
                break;
            case R.id.user_card_btn_send_msg:
                //ToastUtils.showShort("发送消息按钮");
                Intent chatIntent = new Intent(AddContactActivity.this, ChatActivity.class);
                chatIntent.putExtra("sender", fromUser.getUserNum());
                chatIntent.putExtra("contactName", toUser.getUserName());
                chatIntent.putExtra("contactHeader", toUser.getHeader());
                chatIntent.putExtra("mHeader", fromUser.getHeader());
                chatIntent.putExtra("receiver", fromUser.getUserNum());
                startActivity(chatIntent);
                finish();
                break;
            case R.id.addContactUserView:
                //ToastUtils.showShort("用户主页");
                if (toUser != null) {
                    Intent intent = new Intent(AddContactActivity.this, PersonalActivity.class);
                    if (toUser.isFriend()) {
                        intent.putExtra("type", PersonalActivity.PERSONAL_CONTACT);
                        intent.putExtra("contact", GsonUtils.toJson(toUser));
                    } else if(toUser.getUid().equals(fromUser.getUid())) {
                        intent.putExtra("type", PersonalActivity.PERSONAL_SELF);
                    } else {
                        intent.putExtra("type", PersonalActivity.PERSONAL_ADD);
                        intent.putExtra("toUser", GsonUtils.toJson(toUser));
                    }
                    startActivity(intent);
                }
                break;
        }
    }

    private void startSearchUser() {
        //loading...
        addContactLoadingView.setVisibility(View.VISIBLE);
        addContactBlankView.setVisibility(View.GONE);
        addContactUserView.setVisibility(View.GONE);
        //ToastUtils.showShort(et_contact_add.getText().toString().trim());
        String queryNum = et_contact_add.getText().toString().trim();
        //Nil号5-10位 号码11位 判断一下
        if (queryNum.length() < 5 || queryNum.length() > 11) {
            sendMsg(SEARCH_EMPTY);
        }

        searchUser(queryNum, SharedHelper.getInstance(AddContactActivity.this).getUid());
    }

    private void searchUser(String queryNum, int uid) {
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.getUserByAccountOrPhoneNum(queryNum, uid);
        String callTag = "---searchUser---";
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        User user = GsonUtils.fromJson(res, User.class);
                        if (user == null) {
                            sendMsg(SEARCH_EMPTY);
                        } else {
                            sendSuccessMsg(user);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(callTag, "错误码:" + response.code());
                    sendMsg(SEARCH_EMPTY);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(callTag, "连接失败");
                sendMsg(NET_ERROR);
            }
        });
    }

    private void sendSuccessMsg(User user) {
        //先判断活动是否空
        if (ActivityUtils.isActivityAlive(AddContactActivity.this)) {
            Message msg = Message.obtain();
            msg.what = SEARCH_SUCCESS;
            msg.obj = user;
            mHandler.sendMessage(msg);
        }
    }

    private void sendMsg(int state) {
        //先判断活动是否空
        if (ActivityUtils.isActivityAlive(AddContactActivity.this)) {
            Message msg = Message.obtain();
            msg.what = state;
            mHandler.sendMessage(msg);
        }
    }


}