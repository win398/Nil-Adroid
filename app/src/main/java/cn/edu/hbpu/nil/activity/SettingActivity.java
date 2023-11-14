package cn.edu.hbpu.nil.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.widget.button.switchbutton.SwitchButton;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {
    private RelativeLayout setting_nav, setting_modify_password;
    private SwitchButton setting_sb;
    private MaterialCardView setting_logout;
    private ShapeableImageView setting_back;
    private SPUtils sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUI.initTheme(this);
        setContentView(R.layout.activity_setting);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        sp = SPUtils.getInstance();
    }

    private void initView() {
        setting_nav = findViewById(R.id.setting_nav);
        setting_modify_password = findViewById(R.id.setting_modify_password);
        setting_sb = findViewById(R.id.setting_sb);
        setting_logout = findViewById(R.id.setting_logout);
        setting_back = findViewById(R.id.setting_back);

        String notifySound = sp.getString("notifySound");
        setting_sb.setChecked(!notifySound.equals("") && !notifySound.equals("OFF"));

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        setting_nav.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

    private void initEvent() {
        setting_back.setOnClickListener(this);
        setting_logout.setOnClickListener(this);
        setting_modify_password.setOnClickListener(this);
        setting_sb.setAnimationDuration(200);
        setting_sb.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.setting_sb:
                String notifySound = sp.getString("notifySound");
                if (notifySound.equals("") || notifySound.equals("OFF")) {
                    sp.put("notifySound", "ON");
                } else {
                    sp.put("notifySound", "OFF");
                }
                break;
            case R.id.setting_back:
                finish();
                break;
            case R.id.setting_logout:
                SharedHelper.getInstance(NilApplication.getContext()).removeAll();
                startActivity(new Intent(this, MainActivity.class));
                ActivityUtils.finishAllActivitiesExceptNewest();
                break;
            case R.id.setting_modify_password:
                ToastUtils.showShort("修改密码");
                break;
        }
    }
}