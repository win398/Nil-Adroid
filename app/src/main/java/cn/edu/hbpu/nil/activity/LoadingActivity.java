package cn.edu.hbpu.nil.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.blankj.utilcode.util.UtilsTransActivity;

import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.AutoStartUtil;
import cn.edu.hbpu.nil.util.other.BatteryManagementUtil;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.other.NotificationUtil;

public class LoadingActivity extends AppCompatActivity {
    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        //状态栏设置
        LinearLayout loading_container = findViewById(R.id.loading_container);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        loading_container.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);

        //检查权限
        //必要权限
        PermissionUtils.permissionGroup(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                .callback(new PermissionUtils.SimpleCallback() {
                    @Override
                    public void onGranted() {
                        enterApp();
                    }

                    @Override
                    public void onDenied() {
                        finish();
                    }
                })
                .request();


    }

    private void enterApp() {
        SharedHelper sharedHelper = SharedHelper.getInstance(NilApplication.getContext());
        String token = sharedHelper.getToken();
        if (token.equals("")) {
            new Handler().postDelayed(() -> {
                startActivity(new Intent(LoadingActivity.this, MainActivity.class));
                finish();
            }, 1000);
        } else {
            new Handler().postDelayed(() -> {
                startActivity(new Intent(LoadingActivity.this, AppActivity.class));
                finish();
            }, 500);
        }
    }

}