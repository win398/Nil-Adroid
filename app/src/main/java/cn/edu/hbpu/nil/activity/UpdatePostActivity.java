package cn.edu.hbpu.nil.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.donkingliang.imageselector.utils.ImageSelector;
import com.google.android.material.imageview.ShapeableImageView;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.utils.WidgetUtils;
import com.xuexiang.xui.widget.dialog.LoadingDialog;
import com.xuexiang.xui.widget.edittext.ClearEditText;
import com.zs.easy.imgcompress.EasyImgCompress;
import com.zs.easy.imgcompress.bean.ErrorBean;
import com.zs.easy.imgcompress.listener.OnCompressMultiplePicsListener;
import com.zs.easy.imgcompress.util.GBMBKBUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.adapter.SelectAdapter;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdatePostActivity extends AppCompatActivity implements View.OnClickListener {
    private ShapeableImageView update_post_back;
    private TextView update_post_delivery;
    private RecyclerView update_post_content_image;
    private ClearEditText update_post_content_text;
    private RelativeLayout update_post_nav;
    private SharedHelper sh;
    private int uid;
    private List<String> images;
    private SelectAdapter mAdapter;
    private LoadingDialog loadingDialog;

    private static final int DELIVERY_SUCCESS = 0;
    private static final int DELIVERY_FAIL = 1;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DELIVERY_SUCCESS:
                    loadingDialog.dismiss();
                    refreshUpdates();
                    finish();
                    break;
                case DELIVERY_FAIL:
                    loadingDialog.dismiss();
                    ToastUtils.showShort("上传失败");
                    break;
            }
        }
    };

    private void refreshUpdates() {
        List<Activity> activityList = ActivityUtils.getActivityList();
        for (Activity activity : activityList) {
            if (activity instanceof AppActivity) {
                ((AppActivity) activity).refreshUpdates();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUI.initTheme(UpdatePostActivity.this);
        setContentView(R.layout.activity_update_post);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        sh = SharedHelper.getInstance(NilApplication.getContext());
        uid = sh.getUid();
        images = new ArrayList<>();
        images.add("trigger");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        update_post_nav = findViewById(R.id.update_post_nav);
        update_post_content_text = findViewById(R.id.update_post_content_text);
        update_post_content_image = findViewById(R.id.update_post_content_image);
        update_post_delivery = findViewById(R.id.update_post_delivery);
        update_post_back = findViewById(R.id.update_post_back);

        //init image rv
        mAdapter = new SelectAdapter(images, UpdatePostActivity.this);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4);
        update_post_content_image.setLayoutManager(gridLayoutManager);
        update_post_content_image.setAdapter(mAdapter);


        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        update_post_nav.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

    private void initEvent() {
        update_post_back.setOnClickListener(this);
        update_post_delivery.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.update_post_delivery:
                List<String> images = AppUtil.deepCopy(mAdapter.getImages());
                if (images != null) {
                    loadingDialog = WidgetUtils.getLoadingDialog(this)
                            .setLoadingSpeed(5)
                            .setIconScale(0.5F);
                    loadingDialog.updateMessage("提交数据…");
                    loadingDialog.show();
                    images.remove(images.size() - 1);
                    if (images.isEmpty()) {
                        addUpdateWithNoPart();
                    } else {
                        imgCompressAndDelivery(images);
                    }
                }
                break;
            case R.id.update_post_back:
                finish();
                break;
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (requestCode == SelectAdapter.COMPLETE_SELECT) {
            images.clear();
            images.addAll(data.getStringArrayListExtra(ImageSelector.SELECT_RESULT));
            images.add("trigger");
            mAdapter.notifyDataSetChanged();
        }
    }

    //图片压缩 200k以下忽略
    private void imgCompressAndDelivery(List<String> images) {
        //场景二 把多张图片每一张都压缩到200k以内 同时每张像素不超过1200（宽、高都不大于1200）
        EasyImgCompress.withMultiPics(this, images)
                .maxPx(1200)
                .maxSize(200)
                .enableLog(true)
                .setOnCompressMultiplePicsListener(new OnCompressMultiplePicsListener() {
                    @Override
                    public void onStart() {
                        Log.i("EasyImgCompress", "onStart");
                    }

                    @Override
                    public void onSuccess(List<File> successFiles) {
                        addUpdate(successFiles);
                    }

                    @Override
                    public void onHasError(List<File> successFiles, List<ErrorBean> errorImages) {
                        for (int i = 0; i < successFiles.size(); i++) {
                            Log.i("EasyImgCompress", "onHasError: successFile  size = " + GBMBKBUtil.getSize(successFiles.get(i).length()) + "path = " + successFiles.get(i).getAbsolutePath());
                        }
                        for (int i = 0; i < errorImages.size(); i++) {
                            Log.e("EasyImgCompress", "onHasError: errorImg url = " + errorImages.get(i).getErrorImgUrl());
                            Log.e("EasyImgCompress", "onHasError: errorImg msg = " + errorImages.get(i).getErrorMsg());
                        }
                    }
                }).start();

    }

    //上传图片
    private void addUpdate(List<File> files) {
        if (uid == -1) return;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        //内容
        Editable text = update_post_content_text.getText();
        String content = text == null ? "" : text.toString();
        //定义类型
        List<MultipartBody.Part> parts = new ArrayList<>();
        for (File file : files) {
            MediaType mediaType=MediaType.Companion.parse("multipart/form-data");
            RequestBody requestFile = RequestBody.Companion.create(file, mediaType);
            //参数名称为imgFile
            MultipartBody.Part body = MultipartBody.Part.createFormData("parts", file.getName(), requestFile);
            parts.add(body);
        }
        LogUtils.d(parts.toString());
        Call<ResponseBody> call = request.addUpdate(uid, content, parts);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    String res = null;
                    try {
                        if (response.body() == null) return;
                        res = response.body().string();
                        if (res.equals("success")) {
                            sendMsg(DELIVERY_SUCCESS);
                        } else {
                            sendMsg(DELIVERY_FAIL);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendMsg(DELIVERY_FAIL);
                    }

                } else {
                    LogUtils.d("连接错误:" + response.code());
                    sendMsg(DELIVERY_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
                sendMsg(DELIVERY_FAIL);
            }
        });

    }

    //没有图片
    private void addUpdateWithNoPart() {
        if (uid == -1) return;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        //内容
        Editable text = update_post_content_text.getText();
        String content = text == null ? "" : text.toString();
        if (content.isEmpty()) {
            ToastUtils.showShort("请输入内容");
            loadingDialog.dismiss();
            return;
        }
        Call<ResponseBody> call = request.addUpdateWithNoPart(uid, content);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    String res = null;
                    try {
                        if (response.body() == null) return;
                        res = response.body().string();
                        if (res.equals("success")) {
                            sendMsg(DELIVERY_SUCCESS);
                        } else {
                            sendMsg(DELIVERY_FAIL);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendMsg(DELIVERY_FAIL);
                    }

                } else {
                    LogUtils.d("连接错误:" + response.code());
                    sendMsg(DELIVERY_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
                sendMsg(DELIVERY_FAIL);
            }
        });
    }

    private void sendMsg(int what) {
        if (ActivityUtils.isActivityAlive(UpdatePostActivity.this)) {
            Message msg = Message.obtain();
            msg.what = what;
            handler.sendMessage(msg);
        }
    }
}