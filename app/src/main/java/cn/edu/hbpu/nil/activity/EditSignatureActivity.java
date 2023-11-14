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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ConvertUtils;
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

public class EditSignatureActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView edit_signature_ok;
    private ShapeableImageView edit_signature_back;
    private ClearEditText edit_signature_et;
    private SharedHelper sh;
    private LoadingDialog loadingDialog;
    private String signature;

    private static final int MODIFY_SIGNATURE_SUCCESS = 0;
    private static final int MODIFY_SIGNATURE_FAIL = 1;

    private final Handler handler = new Handler(Looper.myLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MODIFY_SIGNATURE_SUCCESS:
                    loadingDialog.dismiss();
                    String str = (String) msg.obj;
                    Intent intent = new Intent();
                    intent.putExtra("newSignature", str);
                    setResult(EditInfoActivity.MODIFY_SIGNATURE, intent);
                    finish();
                    break;
                case MODIFY_SIGNATURE_FAIL:
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
        setContentView(R.layout.activity_edit_signature);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        sh = SharedHelper.getInstance(NilApplication.getContext());
        signature = getIntent().getStringExtra("signature");
    }

    private void initView() {
        RelativeLayout edit_signature_nav = findViewById(R.id.edit_signature_nav);
        edit_signature_back = findViewById(R.id.edit_signature_back);
        edit_signature_ok = findViewById(R.id.edit_signature_ok);
        edit_signature_et = findViewById(R.id.edit_signature_et);
        //设置et可以正好显示3行文字 textSize:16sp  paddingTop/Bottom:16dp extraSpacing:5dp layoutParams单位：px
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) edit_signature_et.getLayoutParams();
        layoutParams.height = ConvertUtils.sp2px(16 * 3) + ConvertUtils.dp2px(16 * 2 + 5 * 6);
        edit_signature_et.setLayoutParams(layoutParams);

        edit_signature_et.setText(signature);
        edit_signature_et.setSelection(signature.length());

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        edit_signature_nav.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

    private void initEvent() {
        edit_signature_back.setOnClickListener(this);
        edit_signature_ok.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.edit_signature_ok:
                String newSignature;
                if (edit_signature_et.getText() == null || edit_signature_et.getText().toString().trim().equals("")) {
                    newSignature = "";
                } else {
                    newSignature = edit_signature_et.getText().toString().trim();
                }
                modifySignature(newSignature);
                loadingDialog = WidgetUtils.getLoadingDialog(EditSignatureActivity.this)
                        .setIconScale(0.5F)
                        .setLoadingSpeed(5);
                loadingDialog.updateMessage("修改个性签名…");
                loadingDialog.show();
                break;
            case R.id.edit_signature_back:
                finish();
                break;
        }
    }

    private void modifySignature(String newSignature) {
        int uid = sh.getUid();
        if (uid == -1) return;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.modifySignature(uid, newSignature);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    sendMsg(MODIFY_SIGNATURE_SUCCESS, newSignature);
                } else {
                    LogUtils.e("连接错误：" + response.code());
                    sendMsg(MODIFY_SIGNATURE_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败" + t);
                sendMsg(MODIFY_SIGNATURE_FAIL);
            }
        });
    }

    private void sendMsg(int what, Object obj) {
        if (ActivityUtils.isActivityAlive(EditSignatureActivity.this)) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }

    private void sendMsg(int what) {
        if (ActivityUtils.isActivityAlive(EditSignatureActivity.this)) {
            Message msg = Message.obtain();
            msg.what = what;
            handler.sendMessage(msg);
        }
    }
}