package cn.edu.hbpu.nil.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.utils.WidgetUtils;
import com.xuexiang.xui.widget.dialog.LoadingDialog;
import com.xuexiang.xui.widget.edittext.ClearEditText;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditUsernameActivity extends AppCompatActivity implements View.OnClickListener {
    private RelativeLayout edit_username_nav;
    private TextView edit_username_ok;
    private ClearEditText edit_username_et;
    private ShapeableImageView edit_username_back;
    private LoadingDialog loadingDialog;
    private SharedHelper sh;
    private String username;

    private static final int MODIFY_USERNAME_SUCCESS = 0;
    private static final int MODIFY_USERNAME_FAIL = 1;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MODIFY_USERNAME_SUCCESS:
                    loadingDialog.dismiss();
                    Intent intent = new Intent();
                    intent.putExtra("newUsername", (String) msg.obj);
                    setResult(EditInfoActivity.MODIFY_USERNAME, intent);
                    finish();
                    break;
                case MODIFY_USERNAME_FAIL:
                    ToastUtils.showShort("修改失败");
                    loadingDialog.dismiss();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUI.initTheme(this);
        setContentView(R.layout.activity_edit_username);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        username = getIntent().getStringExtra("username");
        sh = SharedHelper.getInstance(NilApplication.getContext());
    }

    private void initView() {
        edit_username_nav = findViewById(R.id.edit_username_nav);
        edit_username_back = findViewById(R.id.edit_username_back);
        edit_username_ok = findViewById(R.id.edit_username_ok);
        edit_username_et = findViewById(R.id.edit_username_et);

        edit_username_et.setText(username);
        edit_username_et.setSelection(username.length());

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        edit_username_nav.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

    private void initEvent() {
        edit_username_back.setOnClickListener(this);
        edit_username_ok.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.edit_username_ok:
                if (edit_username_et.getText() == null || edit_username_et.getText().toString().trim().equals("")) {
                    ToastUtils.showShort("用户名不能为空");break;
                } else {
                    loadingDialog = WidgetUtils.getLoadingDialog(EditUsernameActivity.this)
                            .setIconScale(0.5F)
                            .setLoadingSpeed(5);
                    loadingDialog.updateMessage("修改用户名…");
                    loadingDialog.show();
                    modify_username(edit_username_et.getText().toString().trim());
                }
                break;
            case R.id.edit_username_back:
                finish();
                break;
        }
    }

    private void modify_username(String newUsername) {
        int uid = sh.getUid();
        if (uid == -1) return;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.modifyUsername(uid, newUsername);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    sendMsg(MODIFY_USERNAME_SUCCESS, newUsername);
                } else {
                    LogUtils.e("连接错误：" + response.code());
                    sendMsg(MODIFY_USERNAME_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败" + t);
                sendMsg(MODIFY_USERNAME_FAIL);
            }
        });
    }

    private void sendMsg(int what, Object obj) {
        if (ActivityUtils.isActivityAlive(EditUsernameActivity.this)) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }

    private void sendMsg(int what) {
        if (ActivityUtils.isActivityAlive(EditUsernameActivity.this)) {
            Message msg = Message.obtain();
            msg.what = what;
            handler.sendMessage(msg);
        }
    }
}