package cn.edu.hbpu.nil.activity;

import static cn.bingoogolapple.qrcode.zxing.QRCodeEncoder.syncEncodeQRCode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.kongzue.dialogx.DialogX;
import com.kongzue.dialogx.dialogs.PopTip;

import java.util.UUID;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;

public class VisitingCardActivity extends AppCompatActivity {
    private TextView visiting_card_account;
    private ImageView visiting_card_code, visiting_card_back;
    private SharedHelper sh;
    private RelativeLayout visiting_card_save;
    private Bitmap bitmap;

    private static final int FINISH_LOAD = 0;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == FINISH_LOAD) {
                Bitmap bitmap = (Bitmap) msg.obj;
                visiting_card_code.setImageBitmap(bitmap);
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DialogX.init(this);
        setContentView(R.layout.activity_visiting_card);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        sh = SharedHelper.getInstance(NilApplication.getContext());
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("SetTextI18n")
    private void initView() {
        visiting_card_account = findViewById(R.id.visiting_card_account);
        visiting_card_code = findViewById(R.id.visiting_card_code);
        visiting_card_back = findViewById(R.id.visiting_card_back);
        visiting_card_save = findViewById(R.id.visiting_card_save);

        //生成二维码
        ThreadUtils.getSinglePool().submit(() -> {
            bitmap = syncEncodeQRCode(sh.getUser(), ConvertUtils.dp2px(200));
            if (ActivityUtils.isActivityAlive(VisitingCardActivity.this)) {
                //操作UI需要在主线程
                Message msg = Message.obtain();
                msg.what = FINISH_LOAD;
                msg.obj = bitmap;
                handler.sendMessage(msg);
            }
        });

        //账号
        visiting_card_account.setText("NIL:" + sh.getAccount());

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
    }

    private void initEvent() {
        visiting_card_back.setOnClickListener(view -> {
            finish();
        });
        visiting_card_save.setOnClickListener(view -> {
            //保存二维码到相册
            if (bitmap == null) {
                PopTip.show("二维码正在加载，请稍后");
                return;
            }
            AppUtil.savePhotoAlbum(bitmap, UUID.randomUUID() + ".jpg", VisitingCardActivity.this);
            PopTip.show("保存成功");
        });
    }
}