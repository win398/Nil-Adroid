package cn.edu.hbpu.nil.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ClipboardUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.donkingliang.imageselector.utils.ImageSelector;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.widget.dialog.DialogLoader;
import com.zs.easy.imgcompress.EasyImgCompress;
import com.zs.easy.imgcompress.listener.OnCompressSinglePicListener;
import com.zs.easy.imgcompress.util.GBMBKBUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.entity.User;
import cn.edu.hbpu.nil.fragment.GroupContactsFragment;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.other.TimeUtil;
import cn.edu.hbpu.nil.util.other.UIUtils;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import es.dmoral.toasty.Toasty;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonalActivity extends AppCompatActivity implements View.OnClickListener {
    private RelativeLayout personal_nav_container;
    private ShapeableImageView personal_header;
    private ImageView personal_edit_img;
    private LinearLayout personal_header_container, personal_account_container, personal_more, personal_more_exist, personal_signature_container;
    private ImageButton btn_personal_back;
    private TextView personal_username, personal_account, personal_signature, personal_fun_card_tv, personal_user_space_tv;
    private User user;
    private TextView personal_more_blank, personal_more_sex, personal_point_after_sex, personal_more_age, personal_point_after_age, personal_more_locality;
    private MaterialCardView personal_user_space, personal_fun_card;
    //底部弹出框
    private LinearLayout personal_bottom_sheet;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private TextView personal_bottom_sheet_collapse, personal_bottom_sheet_change, personal_bottom_sheet_camera;
    private CoordinatorLayout personal_bottom_sheet_container;
    //图片选择回调
    private static final int SELECTOR_REQUEST_CODE = 12;
    //图片上传success
    private static final int UPLOAD_IMG_SUCCESS = 1;
    //图片上传图片为空
    private static final int UPLOAD_IMG_EMPTY = 2;
    //图片上传其他错误
    private static final int UPLOAD_IMG_ERROR = 3;
    //用户个人页 添加联系人主页  好友主页
    public static final int PERSONAL_SELF = 4;
    public static final int PERSONAL_ADD = 5;
    public static final int PERSONAL_CONTACT = 6;
    //主页信息用户类型(上述三种)
    private int type;

    //修改信息类型 修改签名和头像在EditInfoActivity中定义
    public static final int MODIFY_INFO = 10;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPLOAD_IMG_SUCCESS:
                    //success
                    Toasty.success(PersonalActivity.this, "上传成功", Toasty.LENGTH_SHORT, true).show();
                    break;
                case UPLOAD_IMG_EMPTY:
                    Toasty.info(PersonalActivity.this, "未选择图片", Toasty.LENGTH_SHORT).show();
                    break;
                case UPLOAD_IMG_ERROR:
                    Toasty.error(PersonalActivity.this, "上传失败", Toasty.LENGTH_SHORT, true).show();
                    break;
            }
        }
    };

    private final ActivityResultLauncher<Intent> intentActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getData() == null) return;
            switch (result.getResultCode()) {
                case MODIFY_INFO:
                    data_changed();
                    refreshAppUserUI();
                    break;
                case EditInfoActivity.MODIFY_SIGNATURE:
                    String newSignature = result.getData().getStringExtra("newSignature");
                    personal_signature.setText(newSignature);
                    user.setSignature(newSignature);
                    AppActivity.setUser(user);
                    refreshAppUserUI();
                    break;
            }
        }
    });

    private void refreshAppUserUI() {
        List<Activity> activityList = ActivityUtils.getActivityList();
        for (Activity activity : activityList) {
            if (activity instanceof AppActivity) {
                ((AppActivity) activity).loadUserInfo();
            }
        }
    }

    //用户数据改变
    public void data_changed() {
        user = AppActivity.getUser();
        initUserInfo();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUI.initTheme(this);
        setContentView(R.layout.activity_personal);
        initData();
        initView();
        initEvent();
    }

    private void initData() {
        Intent intent = getIntent();
        type = intent.getIntExtra("type", -1);
        switch (type) {
            case PERSONAL_SELF:
                user = AppActivity.getUser();
                break;
            case PERSONAL_ADD:
                user = GsonUtils.fromJson(intent.getStringExtra("toUser"), User.class);
                break;
            case PERSONAL_CONTACT:
                user = GsonUtils.fromJson(intent.getStringExtra("contact"), User.class);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initEvent() {
        //返回
        btn_personal_back.setOnClickListener(this);
        //长按账号复制到剪切板
        personal_account_container.setOnLongClickListener(view ->{
            ClipboardUtils.copyText(personal_account.getText().toString());
            ToastUtils.showShort("已复制到剪切板");
            return true;
        });
        //设置头像
        personal_header_container.setOnClickListener(this);

        //设置点击事件
        personal_fun_card.setOnClickListener(this);
        if (type == PERSONAL_SELF) {
            personal_account_container.setOnClickListener(this);
            personal_nav_container.setOnClickListener(this);
            personal_bottom_sheet_collapse.setOnClickListener(this);
            personal_bottom_sheet_container.setOnClickListener(this);
            personal_bottom_sheet_change.setOnClickListener(this);
            personal_bottom_sheet_camera.setOnClickListener(this);
            personal_signature_container.setOnClickListener(this);

        }

        //初始化底部弹出框
        bottomSheetBehavior.setHideable(true); //是否可隐藏
        bottomSheetBehavior.setPeekHeight(0);  //隐藏高度
        bottomSheetBehavior.setSkipCollapsed(true);  //跳过折叠状态
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);  //设置为折叠状态
        personal_bottom_sheet_container.setVisibility(View.GONE);
    }

    private void initView() {
        personal_nav_container = findViewById(R.id.personal_nav_container);
        personal_header = findViewById(R.id.personal_header);
        personal_header_container = findViewById(R.id.personal_header_container);
        personal_signature_container = findViewById(R.id.personal_signature_container);
        btn_personal_back = findViewById(R.id.btn_personal_back);
        personal_username = findViewById(R.id.personal_username);
        personal_account = findViewById(R.id.personal_account);
        personal_more_blank = findViewById(R.id.personal_more_blank);
        personal_more = findViewById(R.id.personal_more);
        personal_signature = findViewById(R.id.personal_signature);
        personal_edit_img = findViewById(R.id.personal_edit_img);
        //用户其他信息
        personal_more_sex = findViewById(R.id.personal_more_sex);
        personal_point_after_sex = findViewById(R.id.personal_point_after_sex);
        personal_more_age = findViewById(R.id.personal_more_age);
        personal_point_after_age = findViewById(R.id.personal_point_after_age);
        personal_more_locality = findViewById(R.id.personal_more_locality);
        personal_more_exist = findViewById(R.id.personal_more_exist);

        personal_user_space = findViewById(R.id.personal_user_space);
        personal_user_space_tv = findViewById(R.id.personal_user_space_tv);
        personal_account_container = findViewById(R.id.personal_account_container);
        personal_bottom_sheet = findViewById(R.id.personal_bottom_sheet);
        personal_fun_card = findViewById(R.id.personal_fun_card);
        personal_fun_card_tv = findViewById(R.id.personal_fun_card_tv);
        personal_bottom_sheet_container = findViewById(R.id.personal_bottom_sheet_container);
        bottomSheetBehavior = BottomSheetBehavior.from(personal_bottom_sheet);
        personal_bottom_sheet_collapse = findViewById(R.id.personal_bottom_sheet_collapse);
        personal_bottom_sheet_change = findViewById(R.id.personal_bottom_sheet_change);
        personal_bottom_sheet_camera = findViewById(R.id.personal_bottom_sheet_camera);
        //设置背景容器高度 控制账号显示在左下角
        //取得控件布局参数
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) personal_nav_container.getLayoutParams();
        //图片宽高比例是3:2
        layoutParams.height = UIUtils.getScreenWidth(this) * 2 / 3;
        personal_nav_container.setLayoutParams(layoutParams);

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        personal_nav_container.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);

        //头像大小设置 屏幕宽度1/5
        LinearLayout.LayoutParams layoutParams1 = (LinearLayout.LayoutParams) personal_header.getLayoutParams();
        layoutParams1.height = UIUtils.getScreenWidth(this) / 5;
        layoutParams1.width = UIUtils.getScreenWidth(this) / 5;
        personal_header.setLayoutParams(layoutParams1);

        //设置头像marginTop偏移量 背景高度 - 头像高度/2 - 头像容器paddingTop
        //这里RelativeLayout指父布局
        RelativeLayout.LayoutParams layoutParams2 = (RelativeLayout.LayoutParams) personal_header_container.getLayoutParams();
        layoutParams2.topMargin = layoutParams.height  - layoutParams1.height / 2 - personal_header_container.getPaddingTop();
        //ToastUtils.showShort(personal_header_container.getPaddingTop());
        personal_header_container.setLayoutParams(layoutParams2);

        //用户名最大宽度屏幕宽度3/5
        personal_username.setMaxWidth(UIUtils.getScreenWidth(this) * 3 / 5);

        //初始化用户信息
        initUserInfo();

    }

    @SuppressLint("SetTextI18n")
    private void initUserInfo() {
        //初始化不同类型但共有的信息
        loadCommonInfo();
        switch (type) {
            case PERSONAL_SELF:
                //头像从本地加载 如本地无数据则从云端加载并保存本地
                loadAndSaveHeader();
                //功能按钮
                personal_fun_card_tv.setText("编辑资料");
                personal_user_space_tv.setText("我的空间");
                break;
            case PERSONAL_ADD:
                personal_fun_card_tv.setText("添加好友");
                personal_user_space_tv.setText(user.getUserName() + "的空间");
                personal_edit_img.setVisibility(View.GONE);
                //搜索用户主页，头像从云端加载，不保存本本地
                Glide.with(PersonalActivity.this)
                        .setDefaultRequestOptions(new RequestOptions()
                                .centerCrop()
                                .placeholder(R.mipmap.loadingheader)
                                .fitCenter()
                        )
                        .load(IUserNetUtil.picIp + user.getHeader())
                        .into(personal_header);
                break;
            case PERSONAL_CONTACT:
                loadAndSaveHeader();
                if (!StringUtils.isEmpty(user.getNameMem())) {
                    personal_username.setText(user.getNameMem());
                }
                personal_fun_card_tv.setText("发消息");
                personal_user_space_tv.setText(user.getUserName() + "的空间");
                personal_edit_img.setVisibility(View.GONE);
                break;
        }

    }

    private boolean isLocal;
    private void loadAndSaveHeader() {
        String path = AppUtil.getImgBasePath(PersonalActivity.this) + File.separator + user.getHeader();
        isLocal = true;
        if (!FileUtils.isFileExists(path)) {
            path = IUserNetUtil.picIp + user.getHeader();
            isLocal = false;
        }
        Glide.with(PersonalActivity.this)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(path)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        personal_header.setImageDrawable(resource);
                        if (!isLocal) {
                            try {
                                AppUtil.saveFile(ImageUtils.drawable2Bitmap(resource), user.getHeader());
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

    @SuppressLint("SetTextI18n")
    private void loadCommonInfo() {
        if (this.user != null) {
            personal_account.setText(this.user.getUserNum());
            personal_username.setText(this.user.getUserName());
            if (this.user.getSignature() != null && !this.user.getSignature().equals("")) {
                personal_signature.setText(this.user.getSignature());
            }
            //信息不为空
            if (!StringUtils.isEmpty(this.user.getBgImg())) {
                Glide.with(this)
                        .setDefaultRequestOptions(new RequestOptions()
                                .centerCrop()
                                .placeholder(R.mipmap.loadingheader)
                                .fitCenter()
                        )
                        .load(IUserNetUtil.picIp + File.separator + this.user.getBgImg())
                        .into(new CustomTarget<Drawable>() {
                            //设置到导航栏背景
                            @Override
                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                personal_nav_container.setBackground(resource);
                            }
                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });
            }
            if (!StringUtils.isEmpty(this.user.getSex()) || !StringUtils.isEmpty(this.user.getCity()) || !StringUtils.isEmpty(this.user.getProvince()) || !StringUtils.isEmpty(this.user.getBirth())) {
                personal_more_blank.setVisibility(View.GONE);
                personal_more_exist.setVisibility(View.VISIBLE);
                //显示用户信息
                boolean isSexExist, isAgeExist, isLocalityExist;
                if (!StringUtils.isEmpty(this.user.getSex())) {
                    personal_more_sex.setVisibility(View.VISIBLE);
                    personal_more_sex.setText(this.user.getSex());
                    isSexExist = true;
                } else {
                    personal_more_sex.setVisibility(View.GONE);
                    isSexExist = false;
                }
                //年龄显示
                if (!StringUtils.isEmpty(this.user.getBirth())) {
                    Date date = TimeUtils.string2Date(this.user.getBirth());
                    //根据生日获取年龄
                    personal_more_age.setVisibility(View.VISIBLE);
                    personal_more_age.setText(TimeUtil.getAge(date) + "岁");
                    isAgeExist = true;
                } else {
                    personal_more_age.setVisibility(View.GONE);
                    isAgeExist = false;
                }
                //地区显示
                if (!StringUtils.isEmpty(this.user.getProvince()) || !StringUtils.isEmpty(this.user.getCity())) {
                    String locality;
                    personal_more_locality.setVisibility(View.VISIBLE);
                    if (!StringUtils.isEmpty(this.user.getCity()) && StringUtils.isEmpty(this.user.getProvince())) {
                        locality = this.user.getCity();
                    } else if (StringUtils.isEmpty(this.user.getCity()) && !StringUtils.isEmpty(this.user.getProvince())) {
                        locality = this.user.getProvince();
                    } else {
                        locality = this.user.getProvince() + " " + this.user.getCity();
                    }
                    personal_more_locality.setText(locality);
                    isLocalityExist = true;
                } else {
                    personal_more_locality.setVisibility(View.GONE);
                    isLocalityExist = false;
                }

                //如果性别信息和生日信息都存在，则显示中间分隔点
                if (isSexExist && isAgeExist) {
                    personal_point_after_sex.setVisibility(View.VISIBLE);
                } else {
                    personal_point_after_sex.setVisibility(View.GONE);
                }
                //如果性别信息或生日信息存在，且地区信息存在 则显示中间分隔点
                if ((isSexExist || isAgeExist) && isLocalityExist) {
                    personal_point_after_age.setVisibility(View.VISIBLE);
                } else{
                    personal_point_after_age.setVisibility(View.GONE);
                }
            } else {
                //用户其他信息为空
                personal_more_blank.setVisibility(View.VISIBLE);
                personal_more_exist.setVisibility(View.GONE);
            }
        } else {
            ToastUtils.showShort("加载用户信息失败");
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_personal_back:
                this.finish();
                break;
            case R.id.personal_nav_container:
                personal_bottom_sheet_container.setVisibility(View.VISIBLE);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                break;
            case R.id.personal_bottom_sheet_container:
            case R.id.personal_bottom_sheet_collapse:
                closeBottomSheet();
                break;
            case R.id.personal_bottom_sheet_change:
                if (AppUtil.isGrantedWRPermissions(this)) {
                    openSingleImgSelector();
                } else {
                    //申请权限并设置回调
                    PermissionUtils.permission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                            .callback(new PermissionUtils.SimpleCallback() {
                                @Override
                                public void onGranted() {
                                    openSingleImgSelector();
                                }

                                @Override
                                public void onDenied() {
                                    //提示
                                    DialogLoader.getInstance().showConfirmDialog(
                                            PersonalActivity.this, "需要开启权限才能使用此功能", "设置",
                                            (dialog, which) -> {
                                                //引导用户到设置中去进行设置
                                                PermissionUtils.launchAppDetailsSettings();
                                                dialog.dismiss();
                                            },
                                            "取消",
                                            (dialog, which) -> {
                                                dialog.dismiss();
                                                closeBottomSheet();
                                            }
                                    );
                                }
                            })
                            .request();
                }
                break;
            case R.id.personal_bottom_sheet_camera:
                if (AppUtil.isGrantedCameraPermissions(this)) {
                    openCamera();
                } else {
                    //申请权限并设置回调
                    PermissionUtils.permission(Manifest.permission.CAMERA)
                            .callback(new PermissionUtils.SimpleCallback() {
                                @Override
                                public void onGranted() {
                                    openCamera();
                                }

                                @Override
                                public void onDenied() {
                                    //提示
                                    DialogLoader.getInstance().showConfirmDialog(
                                            PersonalActivity.this, "需要开启权限才能使用此功能", "设置",
                                            (dialog, which) -> {
                                                //引导用户到设置中去进行设置
                                                PermissionUtils.launchAppDetailsSettings();
                                                dialog.dismiss();
                                            },
                                            "取消",
                                            (dialog, which) -> {
                                                dialog.dismiss();
                                                closeBottomSheet();
                                            }
                                    );
                                }
                            })
                            .request();
                }
                break;
            case R.id.personal_fun_card:
                //功能按钮：编辑资料/添加好友/发消息
                switch (type) {
                    case PERSONAL_SELF:
                        //ToastUtils.showShort("编辑资料");
                        Intent intentEdit = new Intent(PersonalActivity.this, EditInfoActivity.class);
                        String signature = StringUtils.isEmpty(user.getSignature()) ? "" : user.getSignature();
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                        String birth = StringUtils.isEmpty(user.getBirth()) ? "" : formatter.format(TimeUtils.string2Date(user.getBirth()));
                        String province = StringUtils.isEmpty(user.getProvince()) ? "" : user.getProvince();
                        String city = StringUtils.isEmpty(user.getCity()) ? "" : user.getCity();
                        String sex = StringUtils.isEmpty(user.getSex()) ? "" : user.getSex();
                        intentEdit.putExtra("header", user.getHeader());
                        intentEdit.putExtra("username", user.getUserName());
                        intentEdit.putExtra("signature", signature);
                        intentEdit.putExtra("birth", birth);
                        intentEdit.putExtra("province", province);
                        intentEdit.putExtra("city", city);
                        intentEdit.putExtra("sex", sex);
                        intentEdit.putExtra("uid", user.getUid());
                        intentActivityResultLauncher.launch(intentEdit);;
                        break;
                    case PERSONAL_ADD:
                        User fromUser = AppActivity.getUser();
                        String[] groups = GroupContactsFragment.getGroupList();
                        if (fromUser != null && user != null && groups.length != 0) {
                            Intent intent = new Intent(PersonalActivity.this, AddInfoActivity.class);
                            intent.putExtra("toUserId", user.getUid());
                            intent.putExtra("fromUserId", fromUser.getUid());
                            intent.putExtra("fromUserName", fromUser.getUserName());
                            intent.putExtra("groups", groups);
                            startActivity(intent);
                        } else {
                            ToastUtils.showShort("数据拉取失败");
                        }
                        break;
                    case PERSONAL_CONTACT:
                        //ToastUtils.showShort("发送消息");
                        Intent intent = new Intent(PersonalActivity.this, ChatActivity.class);
                        intent.putExtra("sender", user.getUserNum());
                        if (StringUtils.isEmpty(user.getNameMem())) {
                            intent.putExtra("contactName", user.getUserName());
                        } else {
                            intent.putExtra("contactName", user.getNameMem());
                        }
                        intent.putExtra("contactHeader", user.getHeader());
                        intent.putExtra("mHeader", AppActivity.getUser().getHeader());
                        intent.putExtra("receiver", AppActivity.getUser().getUserNum());
                        startActivity(intent);
                        PersonalActivity.this.finish();
                        break;
                }
                break;
            case R.id.personal_signature_container:
                String signature = user.getSignature() == null ? "" : user.getSignature();
                Intent intentSignature = new Intent(PersonalActivity.this, EditSignatureActivity.class);
                intentSignature.putExtra("signature", signature);
                intentActivityResultLauncher.launch(intentSignature);
                break;
            case R.id.personal_header_container:
                if (type == PERSONAL_SELF) {
                    Intent intentHeader = new Intent(PersonalActivity.this, HeaderPageActivity.class);
                    intentHeader.putExtra("uid", user.getUid());
                    intentHeader.putExtra("header", user.getHeader());
                    intentActivityResultLauncher.launch(intentHeader);
                }
                break;
        }
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
            closeBottomSheet();
            if (data != null) {
                //获取选择器返回的数据
                ArrayList<String> images = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
                Log.d("----ImgSelectorResult---", images.toString());
                imgCompressAndUpload(images);
            }
        }
    }

    //图片压缩 200k以下忽略
    private void imgCompressAndUpload(List<String> images) {
        //场景一 把单张图片压缩到200k以内 同时像素不超过1200（宽、高都不大于1200）
        EasyImgCompress.withSinglePic(this, images.get(0))
                .maxPx(1200)
                .maxSize(200)
                .enableLog(true)
                .setOnCompressSinglePicListener(new OnCompressSinglePicListener() {
                    @Override
                    public void onStart() {
                        Log.i("EasyImgCompress", "onStart");
                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.i("EasyImgCompress", "onSuccess size = " + GBMBKBUtil.getSize(file.length()) + " getAbsolutePath= " + file.getAbsolutePath());
                        Glide.with(PersonalActivity.this)
                                .setDefaultRequestOptions(new RequestOptions()
                                        .centerCrop()
                                        .placeholder(R.mipmap.loadingheader)
                                        .fitCenter()
                                )
                                .load(file)
                                .into(new CustomTarget<Drawable>() {
                                    //设置到导航栏背景
                                    @Override
                                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                        personal_nav_container.setBackground(resource);
                                    }
                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {

                                    }
                                });
                        uploadBgImg(file);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("EasyImgCompress", "onError error = " + error);
                    }
                }).start();
    }

    //上传背景图片
    private void uploadBgImg(File file) {
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        //定义类型
        MediaType mediaType=MediaType.Companion.parse("multipart/form-data");
        RequestBody requestFile = RequestBody.Companion.create(file, mediaType);
        //参数名称为imgFile
        MultipartBody.Part body = MultipartBody.Part.createFormData("imgFile", file.getName(), requestFile);
        Call<ResponseBody> call = request.uploadBgImg(this.user.getUid(), body);
        Message msg = Message.obtain();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    String res = null;
                    try {
                        assert response.body() != null;
                        res = response.body().string();
                        if (res.equals("success")) {
                            handlerSendMsg(msg, UPLOAD_IMG_SUCCESS);
                            updateUserInfoBgImg();
                        } else if (res.equals("imgEmpty")) {
                            handlerSendMsg(msg, UPLOAD_IMG_EMPTY);
                        } else {
                            handlerSendMsg(msg, UPLOAD_IMG_ERROR);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    Log.d("----uploadBgImg----", String.valueOf(response.code()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d("----uploadBgImg----", "连接失败" + t);
            }
        });

    }

    //获取根据uid获取用户主页背景图名  因为上传时已经在服务器端重命名
    private void updateUserInfoBgImg() {
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.getUserBackground(user.getUid());
        call.enqueue(new Callback<ResponseBody>() {
            private final String mTag = "----updateUserInfoBgImg-----";
            @SuppressLint("LongLogTag")
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        user.setBgImg(response.body().string());
                        AppActivity.setUserBg(user.getBgImg());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.d(mTag, "连接错误---" + response.code());
                }
            }

            @SuppressLint("LongLogTag")
            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(mTag, "连接失败" + t);
            }
        });
    }

    private void handlerSendMsg(Message msg, int state) {
        msg.what = state;
        handler.sendMessage(msg);
    }


    private void closeBottomSheet() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            personal_bottom_sheet_container.setVisibility(View.GONE);
        }
    }

    private void openSingleImgSelector() {
        // 单选图片
        ImageSelector.builder()
                .setCrop(true) // 设置是否使用图片剪切功能。
                .setCropRatio(2/3f) // 图片剪切的宽高比,默认1.0f。宽固定为手机屏幕的宽。
                .useCamera(true) // 设置是否使用拍照
                .setSingle(true) //设置是否单选
                .canPreview(true) //是否可以预览图片，默认为true
                .start(this, SELECTOR_REQUEST_CODE); // 打开相册
    }
    private void openCamera() {
        //使用ImageSelector相机拍照功能
        ImageSelector.builder()
                .setCrop(true) // 设置是否使用图片剪切功能。
                .setCropRatio(2/3f) // 图片剪切的宽高比,默认1.0f。宽固定为手机屏幕的宽。
                .onlyTakePhoto(true)  // 仅拍照，不打开相册
                .start(this, SELECTOR_REQUEST_CODE);
    }
}