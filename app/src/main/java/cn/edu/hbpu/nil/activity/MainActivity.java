package cn.edu.hbpu.nil.activity;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.dialogs.PopTip;
import com.kongzue.dialogx.dialogs.WaitDialog;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.entity.User;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.MaskUtil;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.other.ValidUtil;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import cn.edu.hbpu.nil.widget.ClearEditText;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ClearEditText et_account, et_password;
    private ImageView iv_visible;
    private ImageButton btn_login;
    private TextView tv_go_register;
    //响应数据
    private String token;
    String account, password;
    //SharedPreferences帮助类
    private SharedHelper sharedHelper;
    private ShapeableImageView user_header;
    private LinearLayout login_container;

    private static final int REQUEST_SUCCESS = 0;
    private static final int LOADED_HEADER = 1;
    private static final int ERROR_CONNECT = 2;
    private static final int USER_FORBADE = 3;

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REQUEST_SUCCESS:
                    String token = (String) msg.obj;
                    if (token != null && !token.equals("")) {
                        //ToastUtils.showShort("登录成功 用户：" + u.getUserName());
                        //存token入SharedPreferences
                        sharedHelper.setToken(token);
                        startActivity(new Intent(MainActivity.this, AppActivity.class));
                        sharedHelper.save(account, password);
                        if (!sharedHelper.getToken().equals("")) {
                            finish();
                        } else {
                            //token异常
                            ToastUtils.showShort("登录异常");
                        }
                    } else {
                        ToastUtils.showShort("用户名或密码错误");
                    }
                    //关闭遮罩层
                    WaitDialog.dismiss();
                    break;
                case LOADED_HEADER:
                    //加载用户头像
                    Bitmap bitmap = (Bitmap) msg.obj;
                    user_header.setImageBitmap(bitmap);
                    break;
                case ERROR_CONNECT:
                    ToastUtils.showShort("连接服务器失败");
                    WaitDialog.dismiss();
                    break;
                case USER_FORBADE:
                    WaitDialog.dismiss();
                    MessageDialog.show("账号被封禁", "请联系管理员\n邮箱：1824591386@qq.com", "确定", "取消");
                    break;
            }
        }
    };

    private final ActivityResultLauncher<Intent> intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == 13) {
                assert result.getData() != null;
                et_account.setText(result.getData().getStringExtra("account"));
                et_password.setText(result.getData().getStringExtra("password"));
            }
        }
    });

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DialogX.init(this);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Map<String, String> map = sharedHelper.read();
        et_account.setText(map.get("account"));
        et_password.setText(map.get("password"));
    }
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_go_register:
                //前往注册页
                //launch()方法，输入Intent,ActivityResultCallback:获取返回的数据，
                //ActivityResultContracts.StartActivityForResult 是官方提供用来处理回调数据的ActivityResultContract类
                //跳转到BActivity后，调用setResult()方法传递数据，这部分和以前一样
                intentActivityResultLauncher.launch(new Intent(MainActivity.this, RegisterActivity.class));
                break;
            case R.id.iv_visible:
                //如果editText.getInputType() 的值为128则代表目前是明文显示密码，为129则是隐藏密码)
                if (et_password.getInputType() == 128) {
                    iv_visible.setImageResource(R.mipmap.invisible);
                    et_password.setInputType(129);
                } else if (et_password.getInputType() == 129) {
                    iv_visible.setImageResource(R.mipmap.visible);
                    et_password.setInputType(128);
                }
                break;
            case R.id.btn_login:
                //登录
                //ToastUtils.showShort("click btn login");
                account = Objects.requireNonNull(et_account.getText()).toString();
                password = Objects.requireNonNull(et_password.getText()).toString();
                if (TextUtils.isEmpty(account)) {
                    ToastUtils.showShort("请输入账号！");break;
                } else if (TextUtils.isEmpty(password)) {
                    ToastUtils.showShort("请输入密码！");break;
                }
                //验证账号密码是否合法
                if (!ValidUtil.checkAccount(account)) {
                    PopTip.show("账号5-10位且必须为数字").autoDismiss(1000);
                    break;
                } else if(!ValidUtil.checkPassword(password)) {
                    PopTip.show("密码长度8-16位，包含英文字母和数字").autoDismiss(1000);
                    break;
                }
                User u = new User();
                u.setUserNum(account);
                u.setPassword(password);
                //显示遮罩层
                WaitDialog.show("登录中...");
                login(u);
                break;
        }
    }

    private void loadHeader() {
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.getHeaderByAccount(account);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                Bitmap bitmap = null;
                if (response.code() == 200) {
                    ResponseBody responseBody = response.body();
                    assert responseBody != null;
                    bitmap = BitmapFactory.decodeStream(responseBody.byteStream());
                    sendMsg(LOADED_HEADER, bitmap);
                } else {
                    Log.d("fan", "------code=" + response.code() + "------");
                    sendMsg(ERROR_CONNECT);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d("fan", "------请求失败-----" + t);
                sendMsg(ERROR_CONNECT);
            }
        });
    }

    private void login(User u) {
        //1.创建Retrofit对象
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(IUserNetUtil.baseUrl)
//                .addConverterFactory(GsonConverterFactory.create()) //添加转换器
//                .build();
        //2.创建网络请求接口的实例
//        IUserNetUtil request = retrofit.create(IUserNetUtil.class);
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        //3.对发送请求进行封装
        Call<ResponseBody> call = request.login(u);
        //4.发送网络请求，异步
        call.enqueue(new Callback<ResponseBody>() {
            //请求成功时回调
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                //处理返回的数据结果
                if (response.code() == 200) {
                    //使用转换器将Json转化成对象
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        if (res.equals("account_forbade")) {
                            sendMsg(USER_FORBADE);
                            return;
                        }
                        token = res;
                        sendMsg(REQUEST_SUCCESS, token);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d("fan", "----responseCode200------" + token);
                } else {
                    Log.d("fan", "----responseCode==" + response.code() + "-------");
                    sendMsg(ERROR_CONNECT);
                }
            }
            //请求失败时回调
            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                Log.d("fan", "连接失败！" + throwable);
                sendMsg(ERROR_CONNECT);
            }
        });
    }

    private void sendMsg(int what, Object obj) {
        if (ActivityUtils.isActivityAlive(this)) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            mHandler.sendMessage(msg);
        }
    }

    private void sendMsg(int what) {
        if (ActivityUtils.isActivityAlive(this)) {
            Message msg = Message.obtain();
            msg.what = what;
            mHandler.sendMessage(msg);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private void initEvent() {
        btn_login.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() ==  MotionEvent.ACTION_DOWN) {
                view.setBackgroundResource(R.drawable.btn_anam_hover_bg);
            } else if (motionEvent.getAction() ==  MotionEvent.ACTION_UP) {
                view.setBackgroundResource(R.drawable.btn_anam_bg);
            }
            return false;
        });
        tv_go_register.setOnClickListener(this);
        iv_visible.setOnClickListener(this);
        btn_login.setOnClickListener(this);
        et_account.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //通过账号获取头像
                account = Objects.requireNonNull(et_account.getText()).toString();
                String accountRegex = "[1-9][0-9]{4,9}";
                if (account.matches(accountRegex)) {
                    loadHeader();
                } else {
                    user_header.setImageBitmap(null);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("RestrictedApi")
    private void initView() {
        et_account = findViewById(R.id.et_account);
        et_password = findViewById(R.id.et_password);
        btn_login = findViewById(R.id.btn_login);
        tv_go_register = findViewById(R.id.tv_go_register);
        iv_visible = findViewById(R.id.iv_visible);
        sharedHelper = SharedHelper.getInstance(NilApplication.getContext());
        user_header = findViewById(R.id.user_header);
        login_container = findViewById(R.id.login_container);
        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        login_container.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

}