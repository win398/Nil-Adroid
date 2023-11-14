package cn.edu.hbpu.nil.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.donkingliang.imageselector.utils.ImageSelector;
import com.github.ybq.android.spinkit.SpinKitView;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.utils.WidgetUtils;
import com.xuexiang.xui.widget.button.roundbutton.RoundButton;
import com.xuexiang.xui.widget.dialog.LoadingDialog;
import com.zs.easy.imgcompress.EasyImgCompress;
import com.zs.easy.imgcompress.listener.OnCompressSinglePicListener;
import com.zs.easy.imgcompress.util.GBMBKBUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.entity.User;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HeaderPreviewActivity extends AppCompatActivity implements View.OnClickListener {
    private RelativeLayout header_preview_container;
    private RoundButton header_preview_fun;
    private ImageView header_preview_img;
    private SpinKitView header_preview_loading;
    private LoadingDialog loadingDialog;

    private int type, uid;
    private String header, path;

    public static final int CURRENT_HEADER = 0;
    public static final int HISTORY_HEADER = 1;
    public static final int SELECTED_PIC = 2;
    public static final int UPDATE_PIC = 3;

    private static final int SELECTOR_REQUEST_CODE = 10;

    private static final int MODIFY_SUCCESS = 20;
    private static final int MODIFY_FAIL = 21;
    private static final int HISTORY_SUCCESS = 22;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MODIFY_SUCCESS:
                    //返回是新头像的文件名
                    String res = (String) msg.obj;
                    headerModified(res);
                    loadingDialog.dismiss();
                    finish();
                    break;
                case MODIFY_FAIL:
                    ToastUtils.showShort("设置失败");
                    loadingDialog.dismiss();
                    break;
                case HISTORY_SUCCESS:
                    loadingDialog.dismiss();
                    headerModified(header);
                    finish();
                    break;
            }
        }
    };

    private void headerModified(String header) {
        //更新本地缓存的用户信息 先在这里修改，以免PersonalActivity在APPActivity后面执行，数据还未更新
        User user = AppActivity.getUser();
        user.setHeader(header);
        AppActivity.setUser(user);

        List<Activity> activityList = ActivityUtils.getActivityList();
        for (Activity activity : activityList) {
            if (activity instanceof HeaderPageActivity) {
                HeaderPageActivity headerPageActivity = (HeaderPageActivity) activity;
                headerPageActivity.setHeader(header);
                headerPageActivity.data_changed();
            }
            if (activity instanceof PersonalActivity) {
                ((PersonalActivity) activity).data_changed();
            }
            if (activity instanceof AppActivity) {
                ((AppActivity) activity).loadUserInfo();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUI.initTheme(this);
        setContentView(R.layout.activity_header_preview);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        Intent intent = getIntent();
        type = intent.getIntExtra("type", -1);
        if (type == CURRENT_HEADER || type == HISTORY_HEADER) {
            uid = intent.getIntExtra("uid", -1);
            header = intent.getStringExtra("header");
        } else {
            path = header = intent.getStringExtra("path");
        }
    }

    private void initView() {
        //设置状态栏不可见
        BarUtils.setStatusBarVisibility(HeaderPreviewActivity.this, false);

        header_preview_container = findViewById(R.id.header_preview_container);
        header_preview_fun = findViewById(R.id.header_preview_fun);
        header_preview_img = findViewById(R.id.header_preview_img);
        header_preview_loading = findViewById(R.id.header_preview_loading);

        switch (type) {
            case CURRENT_HEADER:
                header_preview_fun.setText("更换头像");
                break;
            case HISTORY_HEADER:
                header_preview_fun.setText("设置为头像");
                break;
            case UPDATE_PIC:
                header_preview_fun.setText("保存到本地相册");
                break;
            default:
                header_preview_fun.setVisibility(View.GONE);
                break;
        }


        header_preview_loading.setVisibility(View.VISIBLE);
        if (type == CURRENT_HEADER || type == HISTORY_HEADER) {
            //加载头像
            loadHeader();
        } else {
            loadPic();
        }
    }

    private void loadPic() {
        Glide.with(HeaderPreviewActivity.this)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(path)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        header_preview_img.setImageDrawable(resource);
                        header_preview_loading.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    private void loadHeader() {
        Glide.with(HeaderPreviewActivity.this)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(IUserNetUtil.picIp + header)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        header_preview_img.setImageDrawable(resource);
                        header_preview_loading.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    private void initEvent() {
        header_preview_container.setOnClickListener(this);
        header_preview_fun.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.header_preview_container:
                finish();
                break;
            case R.id.header_preview_fun:
                switch (type) {
                    case CURRENT_HEADER:
                        openSingleImgSelector();
                        break;
                    case HISTORY_HEADER:
                        showLoadingDialog("提交修改…");
                        modifyHeaderByHistory();
                        break;
                    case UPDATE_PIC:
                        savePic2Album();
                        break;
                }
                break;
        }
    }

    //保存图片到相册
    private void savePic2Album() {
        Glide.with(HeaderPreviewActivity.this)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(path)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        AppUtil.savePhotoAlbum(ImageUtils.drawable2Bitmap(resource), UUID.randomUUID() + ".jpg", HeaderPreviewActivity.this);
                        ToastUtils.showShort("保存成功");
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    private void modifyHeaderByHistory() {
        if (uid == -1) return;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.modifyHeaderByHistory(uid, header);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    sendMsg(HISTORY_SUCCESS);
                } else {
                    LogUtils.d("连接错误：" + response.code());
                    sendMsg(MODIFY_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
                sendMsg(MODIFY_FAIL);
            }
        });

    }

    //打开图片选择器
    private void openSingleImgSelector() {
        // 单选图片
        ImageSelector.builder()
                .setCrop(true) // 设置是否使用图片剪切功能。
                .setCropRatio(1.0f) // 图片剪切的宽高比,默认1.0f。宽固定为手机屏幕的宽。
                .useCamera(true) // 设置是否使用拍照
                .setSingle(true) //设置是否单选
                .canPreview(true) //是否可以预览图片，默认为true
                .start(this, SELECTOR_REQUEST_CODE); // 打开相册
    }

    /**
     * todo onActivityResult 对拍照、相册选择图片的返回结果进行处理
     * @param requestCode 返回码，用于确定是哪个 Activity 返回的数据
     * @param resultCode 返回结果，一般如果操作成功返回的是 RESULT_OK
     * @param data 返回对应 activity 返回的数据
     */
    @SuppressLint("LongLogTag")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECTOR_REQUEST_CODE) {
            if (data != null) {
                //获取选择器返回的数据
                ArrayList<String> images = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
                Log.d("----ImgSelectorResult---", images.toString());
                imgCompressAndUpload(images);
            }
        }
    }

    //图片压缩 100k以下忽略
    private void imgCompressAndUpload(List<String> images) {
        //场景一 把单张图片压缩到100k以内 同时像素不超过1200（宽、高都不大于1200）
        EasyImgCompress.withSinglePic(this, images.get(0))
                .maxPx(1200)
                .maxSize(100)
                .enableLog(true)
                .setOnCompressSinglePicListener(new OnCompressSinglePicListener() {
                    @Override
                    public void onStart() {
                        Log.i("EasyImgCompress", "onStart");
                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.i("EasyImgCompress", "onSuccess size = " + GBMBKBUtil.getSize(file.length()) + " getAbsolutePath= " + file.getAbsolutePath());
                        showLoadingDialog("上传头像…");
                        modifyHeader(file);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("EasyImgCompress", "onError error = " + error);
                    }
                }).start();
    }

    private void showLoadingDialog(String tip) {
        loadingDialog = WidgetUtils.getLoadingDialog(HeaderPreviewActivity.this)
                .setIconScale(0.5F)
                .setLoadingSpeed(5);
        loadingDialog.updateMessage(tip);
        loadingDialog.show();
    }

    //从相册选择图片更改头像
    private void modifyHeader(File file) {
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        //定义类型
        MediaType mediaType=MediaType.Companion.parse("multipart/form-data");
        RequestBody requestFile = RequestBody.Companion.create(file, mediaType);
        //参数名称为imgFile
        MultipartBody.Part body = MultipartBody.Part.createFormData("imgFile", file.getName(), requestFile);
        Call<ResponseBody> call = request.modifyHeader(uid, body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    ResponseBody resBody = response.body();
                    if (resBody != null) {
                        try {
                            String res = resBody.string();
                            if (!res.equals("imgEmpty") && !res.equals("fail")) {
                                sendMsg(MODIFY_SUCCESS, res);
                            } else {
                                sendMsg(MODIFY_FAIL);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    LogUtils.d("连接错误:" + response.code());
                    sendMsg(MODIFY_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
                sendMsg(MODIFY_FAIL);
            }
        });
    }

    private void sendMsg(int what) {
        if (ActivityUtils.isActivityAlive(HeaderPreviewActivity.this)) {
            Message msg = Message.obtain();
            msg.what = what;
            handler.sendMessage(msg);
        }
    }

    private void sendMsg(int what, Object obj) {
        if (ActivityUtils.isActivityAlive(HeaderPreviewActivity.this)) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }

}