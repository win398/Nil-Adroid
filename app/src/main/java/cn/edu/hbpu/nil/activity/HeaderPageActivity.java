package cn.edu.hbpu.nil.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.adapter.HistoryHeaderAdapter;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.other.UIUtils;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HeaderPageActivity extends AppCompatActivity implements View.OnClickListener {
    private RelativeLayout header_page_nav, header_page_current;
    private ShapeableImageView header_page_back;
    private RecyclerView header_page_rv;
    private ImageView current_header;

    private List<String> headers;
    private HistoryHeaderAdapter headerAdapter;
    private String header;
    public int uid;

    private static final int LOAD_HISTORY_SUCCESS = 0;
    private static final int LOAD_HISTORY_FAIL = 1;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case LOAD_HISTORY_SUCCESS:
                    headers = AppUtil.castList(msg.obj, String.class);
                    headerAdapter.setHeaders(headers);
                    headerAdapter.notifyDataSetChanged();
                    break;
                case LOAD_HISTORY_FAIL:
                    ToastUtils.showShort("加载失败");
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_header_page);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        header = getIntent().getStringExtra("header");
        uid = getIntent().getIntExtra("uid", -1);

        headers = new ArrayList<>();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        header_page_nav = findViewById(R.id.header_page_nav);
        header_page_back = findViewById(R.id.header_page_back);
        header_page_current = findViewById(R.id.header_page_current);
        header_page_rv = findViewById(R.id.header_page_rv);
        current_header = findViewById(R.id.current_header);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        headerAdapter = new HistoryHeaderAdapter(headers, this);
        header_page_rv.setLayoutManager(gridLayoutManager);
        header_page_rv.setAdapter(headerAdapter);

        //头像容器设置1:1
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) header_page_current.getLayoutParams();
        layoutParams.height = UIUtils.getScreenWidth(HeaderPageActivity.this);
        header_page_current.setLayoutParams(layoutParams);

        //加载当前头像
        loadCurrentHeader();
        //加载历史头像
        loadHistoryHeader();

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        header_page_nav.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

    //加载历史头像
    private void loadHistoryHeader() {
        if (uid == -1) return;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.getHistoryHeader(uid);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    ResponseBody body = response.body();
                    if (body != null) {
                        try {
                            String res = body.string();
                            Type type = new TypeToken<List<String>>() {}.getType();
                            List<String> list = GsonUtils.fromJson(res, type);
                            sendMsg(LOAD_HISTORY_SUCCESS, list);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                } else {
                    LogUtils.d("连接错误:" + response.code());
                    sendMsg(LOAD_HISTORY_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
                sendMsg(LOAD_HISTORY_FAIL);
            }
        });
    }

    private void sendMsg(int what) {
        if (ActivityUtils.isActivityAlive(HeaderPageActivity.this)) {
            Message msg = Message.obtain();
            msg.what = what;
            handler.sendMessage(msg);
        }
    }

    private void sendMsg(int what, Object obj) {
        if (ActivityUtils.isActivityAlive(HeaderPageActivity.this)) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }

    //加载当前头像
    private boolean isLocal;
    private void loadCurrentHeader() {
        isLocal = true;
        String path = AppUtil.getImgBasePath(HeaderPageActivity.this) + File.separator + header;
        if (!FileUtils.isFileExists(path)) {
            path = IUserNetUtil.picIp + header;
            isLocal = false;
        }
        Glide.with(HeaderPageActivity.this)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(path)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        current_header.setImageDrawable(resource);
                        if (!isLocal) {
                            try {
                                AppUtil.saveFile(ImageUtils.drawable2Bitmap(resource), header);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    private void initEvent() {
        header_page_back.setOnClickListener(this);
        current_header.setOnClickListener(this);
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void data_changed() {
        loadCurrentHeader();
        loadHistoryHeader();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.header_page_back:
                finish();
                break;
            case R.id.current_header:
                Intent intent = new Intent(HeaderPageActivity.this, HeaderPreviewActivity.class);
                intent.putExtra("type", HeaderPreviewActivity.CURRENT_HEADER);
                intent.putExtra("header", header);
                intent.putExtra("uid", uid);
                startActivity(intent);
                break;
        }
    }
}