package cn.edu.hbpu.nil.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.donkingliang.imageselector.utils.ImageSelector;
import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.dialogs.PopTip;

import java.util.ArrayList;

import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;
import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.adapter.SelectAdapter;
import cn.edu.hbpu.nil.entity.User;
import cn.edu.hbpu.nil.util.NilDBHelper;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;

public class ScanActivity extends AppCompatActivity implements View.OnClickListener {
    private ZXingView scan_zxingview;
    private ImageView scan_back, scan_album, scan_flashlight;
    private boolean isOpenFlashlight, hasNotified;
    private SharedHelper sh;
    private NilDBHelper nilDBHelper;

    private static final int FINISH_SELECTED = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DialogX.init(this);
        setContentView(R.layout.activity_scan);
        initData();
        initView();
        initEvent();
    }

    @Override
    protected void onStop() {
        super.onStop();
        scan_zxingview.stopCamera();
        if (nilDBHelper.isOpen()) {
            nilDBHelper.closeLink();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scan_zxingview.onDestroy();
    }

    private void initData() {
        sh = SharedHelper.getInstance(NilApplication.getContext());
        nilDBHelper = NilDBHelper.getInstance(ScanActivity.this, sh.getAccount(), 0);
    }

    private void initView() {
        scan_zxingview = findViewById(R.id.scan_zxingview);
        scan_back = findViewById(R.id.scan_back);
        scan_album = findViewById(R.id.scan_album);
        scan_flashlight = findViewById(R.id.scan_flashlight);
        scan_zxingview.startCamera();
        scan_zxingview.startSpot();


        BarUtils.setStatusBarVisibility(this, false);
    }

    private void initEvent() {
        scan_back.setOnClickListener(this);
        scan_album.setOnClickListener(this);
        scan_flashlight.setOnClickListener(this);
        scan_zxingview.setDelegate(new QRCodeView.Delegate() {
            @Override
            public void onScanQRCodeSuccess(String result) {
                if (result == null) {
                    scan_zxingview.startCamera();
                    scan_zxingview.startSpotAndShowRect();
                    PopTip.show("无法识别");
                    return;
                }
                try {
                    User user = GsonUtils.fromJson(result, User.class);
                    if (user == null) {
                        scan_zxingview.startCamera();
                        scan_zxingview.startSpotAndShowRect();
                        PopTip.show("用户数据异常");
                        return;
                    }
                    Intent intent = new Intent(ScanActivity.this, PersonalActivity.class);
                    if (!nilDBHelper.isOpen()) {
                        nilDBHelper.openReadLink();
                    }
                    if (!nilDBHelper.getStateByAccount(user.getUserNum()).equals("")) {
                        intent.putExtra("type", PersonalActivity.PERSONAL_CONTACT);
                        intent.putExtra("contact", result);
                    } else if(user.getUid() == sh.getUid()) {
                        intent.putExtra("type", PersonalActivity.PERSONAL_SELF);
                    } else {
                        intent.putExtra("type", PersonalActivity.PERSONAL_ADD);
                        intent.putExtra("toUser", result);
                    }
                    startActivity(intent);
                    ScanActivity.this.finish();
                } catch (Exception e) {
                    PopTip.show("无效二维码");
                }
            }

            @Override
            public void onCameraAmbientBrightnessChanged(boolean isDark) {
                if (isDark && !hasNotified) {
                    PopTip.show("灯光过暗，建议打开闪光灯");
                    hasNotified = true;
                }
            }

            @Override
            public void onScanQRCodeOpenCameraError() {
                LogUtils.e("相机打开失败");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            scan_zxingview.startCamera();
            scan_zxingview.startSpotAndShowRect();
            return;
        }
        if (requestCode == FINISH_SELECTED) {
            ArrayList<String> res = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
            LogUtils.d(res.get(0));
            scan_zxingview.decodeQRCode(res.get(0));
        }

    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scan_album:
                //不限数量的多选
                ImageSelector.builder()
                        .useCamera(true) // 设置是否使用拍照
                        .setSingle(true)  //设置是否单选
                        .canPreview(true) //是否可以预览图片，默认为true
                        .start(ScanActivity.this, FINISH_SELECTED); // 打开相册
                break;
            case R.id.scan_back:
                finish();
                break;
            case R.id.scan_flashlight:
                if (isOpenFlashlight) {
                    scan_zxingview.closeFlashlight();
                } else {
                    scan_zxingview.openFlashlight();
                }
                isOpenFlashlight = !isOpenFlashlight;
                break;
        }
    }
}