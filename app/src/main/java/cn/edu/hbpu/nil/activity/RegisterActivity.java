package cn.edu.hbpu.nil.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.dialogs.PopTip;
import com.kongzue.dialogx.dialogs.WaitDialog;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.entity.User;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.other.UIUtils;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.other.MaskUtil;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.ValidUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageButton ibtn_register_back;
    private Button btn_register, btn_check_code;
    private String resStr = null;
    private EditText et_username, et_pwd, et_phone, et_checkCode;
    private SharedHelper sharedHelper;
    private String username, password, phoneNum, checkCode;

    private LinearLayout register_main_container;

    private static final int CONNECT_SUCCESS = 0;
    private static final int CONNECT_FAIL = 1;
    private static final int ERROR_CHECK_CODE = 2;
    private static final int CHECK_CODE_EXPIRED = 3;

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            //接受子线程的消息
            switch (msg.what) {
                case CONNECT_SUCCESS:
                    String msgStr = (String) msg.obj;
                    //失败返回fail 成功返回账号
                    if (msgStr == null || msgStr.equals("fail")) {
                        ToastUtils.showShort("注册失败");
                    } else {
                        ToastUtils.showShort("注册成功，请注意保存账号");
                        //存入SharedPreferences
                        sharedHelper.save(msgStr, password);
                        Intent intent = new Intent();
                        intent.putExtra("account", msgStr);
                        intent.putExtra("password", password);
                        setResult(13, intent);
                        finish();
                    }
                    //关闭遮罩层
                    WaitDialog.dismiss();
                    break;
                case CONNECT_FAIL:
                    ToastUtils.showShort("连接服务器失败");
                    WaitDialog.dismiss();
                    break;
                case ERROR_CHECK_CODE:
                    PopTip.show("验证码错误").autoDismiss(1000);
                    WaitDialog.dismiss();
                    break;
                case CHECK_CODE_EXPIRED:
                    PopTip.show("验证码过期或未发送").autoDismiss(1000);
                    WaitDialog.dismiss();
                    break;
            }
        }
    };
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DialogX.init(this);
        setContentView(R.layout.activity_register);
        initView();
        initEvent();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initEvent() {
        ibtn_register_back.setOnClickListener(this);
        View.OnTouchListener listener = (view, motionEvent) -> {
            if (motionEvent.getAction() ==  MotionEvent.ACTION_DOWN) {
                view.setBackgroundResource(R.drawable.bg_register_btn_click);
            } else if (motionEvent.getAction() ==  MotionEvent.ACTION_UP) {
                view.setBackgroundResource(R.drawable.bg_register_btn);
            }
            return false;
        };
        btn_register.setOnTouchListener(listener);
        btn_register.setOnClickListener(this);
        btn_check_code.setOnTouchListener(listener);
        btn_check_code.setOnClickListener(this);
    }

    //注册请求
    private void register(User u, String checkCode) {
        //1.创建Retrofit对象
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(IUserNetUtil.baseUrl)
                .addConverterFactory(GsonConverterFactory.create()) //添加转换器
                .build();
        //2.创建网络请求接口的实例
        IUserNetUtil request = retrofit.create(IUserNetUtil.class);
        //3.对发送请求进行封装
        Call<ResponseBody> call = request.register(u, checkCode);
        //4.发送网络请求，异步
        call.enqueue(new Callback<ResponseBody>() {
            //请求成功时回调
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                //处理返回的数据结果
                if (response.code() == 200) {
                    //使用转换器将Json转化成对象
                    assert response.body() != null;
                    try {
                        resStr = response.body().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (resStr.equals("error_code")) {
                        sendMsg(ERROR_CHECK_CODE);
                        return;
                    } else if (resStr.equals("code_expired")) {
                        sendMsg(CHECK_CODE_EXPIRED);
                        return;
                    }
                    LogUtils.d("注册成功：" + resStr);
                    //使用Handler将结果传给主线程
                    sendMsg(CONNECT_SUCCESS, resStr);
                } else {
                    LogUtils.e("连接错误：" + response.code());
                    sendMsg(CONNECT_FAIL);
                }
            }
            //请求失败时回调
            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable throwable) {
                LogUtils.e("连接失败" + throwable);
                sendMsg(CONNECT_FAIL);
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

    private void usingTimer() {
        //使用Java的Timer配合TimerTask(定时任务)
        final int[] time = {60};
        final Timer timer = new Timer();
        TimerTask mTimerTask = new TimerTask() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (time[0] <= 0) {
                        cancel();
                        btn_check_code.setEnabled(true);
                        btn_check_code.setText("发送验证码");
                    } else {
                        btn_check_code.setEnabled(false);
                        btn_check_code.setText(time[0] + "秒后重发");
                    }
                    Log.i("fan", "time: " + time[0]);
                    time[0]--;
                });
            }
        };
        timer.schedule(mTimerTask, 0, 1000);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        ibtn_register_back = findViewById(R.id.ibtn_register_back);
        btn_register = findViewById(R.id.btn_register);
        btn_check_code = findViewById(R.id.btn_check_code);
        et_username = findViewById(R.id.et_username);
        et_pwd = findViewById(R.id.et_pwd);
        et_phone = findViewById(R.id.et_phone);
        et_checkCode = findViewById(R.id.et_checkCode);
        sharedHelper = SharedHelper.getInstance(NilApplication.getContext());
        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) ibtn_register_back.getLayoutParams();
        layoutParams.topMargin += BarUtils.getStatusBarHeight();

        register_main_container = findViewById(R.id.register_main_container);
        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        register_main_container.measure(w, h);
        int autoPaddingTop = UIUtils.getScreenHeight(this) - register_main_container.getMeasuredHeight();
        Log.d("autoPaddingTop-----", String.valueOf(autoPaddingTop));
        register_main_container.setPadding(0, autoPaddingTop, 0, 0);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ibtn_register_back:
                finish();
                break;
            case R.id.btn_check_code:
                phoneNum = et_phone.getText().toString();
                if (!ValidUtil.checkPhoneNum(phoneNum)) {
                    PopTip.show("手机号不合法").autoDismiss(1000);
                    break;
                }
                usingTimer();
                getCheckCode(phoneNum);
                break;
            case R.id.btn_register:
                //注册
                username = et_username.getText().toString().trim();
                password = et_pwd.getText().toString().trim();
                phoneNum = et_phone.getText().toString().trim();
                checkCode = et_checkCode.getText().toString().trim();
                if (TextUtils.isEmpty(username)) {
                    ToastUtils.showShort("请输入昵称");break;
                } else if (TextUtils.isEmpty(password)) {
                    ToastUtils.showShort("请输入密码");break;
                } else if (TextUtils.isEmpty(phoneNum)) {
                    ToastUtils.showShort("请输入手机号");break;
                } else if (TextUtils.isEmpty(checkCode)) {
                    ToastUtils.showShort("请输入验证码");break;
                }
                //验证密码是否合法
                if (!ValidUtil.checkPassword(password)) {
                    PopTip.show("密码长度8-16位，包含英文字母和数字").autoDismiss(1000);
                    break;
                }
                User u = new User();
                u.setUserName(username);
                u.setPassword(password);
                //u.setPhoneNum(finalPhoneNum);
                u.setPhoneNum(phoneNum);
                //显示遮罩层
                WaitDialog.show("注册中...");
                register(u, checkCode);
                break;
        }
    }

    //获取短信验证码
    private void getCheckCode(String phoneNum) {
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.getCheckCode(phoneNum);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        if (res.equals("hasSent")) {
                            PopTip.show("验证码5分钟内有效，请不要重复发送").autoDismiss(1000);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtils.d("连接错误：" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
            }
        });
    }
}