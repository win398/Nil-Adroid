package cn.edu.hbpu.nil.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.donkingliang.imageselector.utils.ImageSelector;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xuexiang.xui.XUI;
import com.xuexiang.xui.widget.dialog.DialogLoader;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cn.bingoogolapple.badgeview.BGABadgeRadioButton;
import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.adapter.AppFragmentPagerAdapter;
import cn.edu.hbpu.nil.entity.ChatMsg;
import cn.edu.hbpu.nil.entity.FriendVerification;
import cn.edu.hbpu.nil.entity.MsgCard;
import cn.edu.hbpu.nil.entity.User;
import cn.edu.hbpu.nil.fragment.AllContactsFragment;
import cn.edu.hbpu.nil.fragment.ContactFragment;
import cn.edu.hbpu.nil.fragment.GroupContactsFragment;
import cn.edu.hbpu.nil.fragment.MoreFragment;
import cn.edu.hbpu.nil.fragment.MsgFragment;
import cn.edu.hbpu.nil.receiver.ChatMsgReceiver;
import cn.edu.hbpu.nil.service.JWebSocketClientService;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.NilDBHelper;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.other.NotificationUtil;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.JWebSocketClient;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {
    //ViewPager 几个代表页面的常量
    public static final int PAGE_ONE = 0;
    public static final int PAGE_TWO = 1;
    public static final int PAGE_THREE = 2;
    //handler msg.what常量
    private static final int REQUEST_ERROR = 0;
    private static final int LOAD_USER_SUCCESS = 1;
    private static final int NEW_MESSAGE = 2;
    private static final int LOAD_VER_SUCCESS = 3;
    private static final int NEW_VER = 4;
    private static final int REFRESH_CONTACT = 5;
    private static final int UPDATE_VER_SUCCESS = 6;
    private static final int MODIFY_SIGNATURE_SUCCESS = 7;
    private static final int VER_AGREED = 8;
    private final String TAG = "----APP_LOG_TAG----";
    private BGABadgeRadioButton mRbMsg, mRbContact, mRbMore;
    private RadioGroup mRadioGroup;
    private ViewPager2 mViewPager;
    private AppFragmentPagerAdapter mAdapter;
    private List<BGABadgeRadioButton> rbs;

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private LinearLayout app_main;

    private static User user = null;
    private SharedHelper sh;
    private NilDBHelper nilDBHelper;
    private ShapeableImageView app_user_header;
    private TextView app_username;

    public String mAccount;
    //用户头像路径
    public String headerPath;
    private String token;

    //加载验证信息（加载新验证信息为基准）和联系人标记（加载分组联系人为基准）
    public boolean verLoaded, contactLoaded;

    //侧滑栏头部
    private TextView app_user_signature;
    private LinearLayout drawer_header_layout, app_user_signature_container;
    private MenuItem menu_setting, menu_scan;
    private ImageView app_user_qrcode;

    //绑定webSocket服务
    private JWebSocketClient client;
    private JWebSocketClientService.JWebSocketClientBinder binder;
    private JWebSocketClientService jWSClientService;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogUtils.d("服务与活动成功绑定");
            binder = (JWebSocketClientService.JWebSocketClientBinder) iBinder;
            jWSClientService = binder.getService();
            client = jWSClientService.client;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogUtils.d("服务与活动成功断开");
        }
    };

    //注册广播接收消息
    private class ChatMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra("message");
            ChatMsg chatMsg = GsonUtils.fromJson(msg, ChatMsg.class);
            LogUtils.d("收到来自" + chatMsg.getSendAccount() + "的消息:" + chatMsg.getMsgContent());
            Message message = Message.obtain();
            message.what = NEW_MESSAGE;
            handler.sendMessage(message);
        }
    }
    private class VerMessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.d("有新的验证消息");
            Message message = Message.obtain();
            message.what = NEW_VER;
            handler.sendMessage(message);
        }
    }
    private class RefreshContactsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.d("刷新联系人数据");
            Message message = Message.obtain();
            message.what = REFRESH_CONTACT;
            handler.sendMessage(message);
        }
    }
    private class verAgreedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = Message.obtain();
            message.what = VER_AGREED;
            handler.sendMessage(message);
        }
    }
    //动态注册广播
    private void doRegisterReceiver() {
        ChatMessageReceiver chatMessageReceiver = new ChatMessageReceiver();
        VerMessageReceiver verMessageReceiver = new VerMessageReceiver();
        RefreshContactsReceiver refreshContactsReceiver = new RefreshContactsReceiver();
        verAgreedReceiver verAgreedReceiver = new verAgreedReceiver();

        ChatMsgReceiver chatMsgReceiver = new ChatMsgReceiver();
        IntentFilter filter = new IntentFilter("cn.edu.hbpu.chatmsg");
        registerReceiver(chatMessageReceiver, filter);
        IntentFilter filter1 = new IntentFilter("cn.edu.hbpu.vermsg");
        registerReceiver(verMessageReceiver, filter1);
        IntentFilter refreshFilter = new IntentFilter("cn.edu.hbpu.refreshContacts");
        registerReceiver(refreshContactsReceiver, refreshFilter);
        IntentFilter agreedFilter = new IntentFilter("cn.edu.hbpu.veragreed");
        registerReceiver(verAgreedReceiver, agreedFilter);

        registerReceiver(chatMsgReceiver, new IntentFilter("cn.edu.hbpu.chatmsg"));
    }

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REQUEST_ERROR:
                    //异常
                    ToastUtils.showShort("服务器交互异常");
                    break;
                case LOAD_USER_SUCCESS:
                    if (user != null) {
                        sh.setUser(GsonUtils.toJson(user));
                        sh.setUid(user.getUid());
                        //加载用户信息
                        loadUserInfo();
                        updateMsgFgHeader();
                    } else {
                        Log.d(TAG, "用户信息为null");
                    }
                    break;
                case NEW_MESSAGE:
                    //发送信息
                    cardDataChanged();
                    showOrUpdateMsgBadge();
                    break;
                case LOAD_VER_SUCCESS:
                    List<FriendVerification> verifications = AppUtil.castList(msg.obj, FriendVerification.class);
                    if (!nilDBHelper.isOpen()) {
                        nilDBHelper.openWriteLink();
                    }
                    nilDBHelper.insertVer(verifications);
                    showOrUpdateContactBadge();
                    List<Fragment> fragments = getSupportFragmentManager().getFragments();
                    for (Fragment f : fragments) {
                        if (f instanceof ContactFragment) {
                            ((ContactFragment) f).setBadge(getBadge());
                        }
                    }
                    verLoaded = true;
                    break;
                case NEW_VER:
                    updateVerifications();
                    break;
                case REFRESH_CONTACT:
                    refreshContact();
                    break;
                case UPDATE_VER_SUCCESS:
                    List<FriendVerification> newLocalVerifications = AppUtil.castList(msg.obj, FriendVerification.class);
                    if (!nilDBHelper.isOpen()) {
                        nilDBHelper.openWriteLink();
                    }
                    for(FriendVerification verification : newLocalVerifications) {
                        nilDBHelper.updateVerState(verification.getVerificationId(), verification.getVerifyState());
                    }
                    showOrUpdateContactBadge();
                    List<Fragment> fragments1 = getSupportFragmentManager().getFragments();
                    for (Fragment f : fragments1) {
                        if (f instanceof ContactFragment) {
                            ((ContactFragment) f).setBadge(getBadge());
                        }
                    }
                    break;
                case MODIFY_SIGNATURE_SUCCESS:
                    //服务端响应成功 更新本地UI显示新签名  在方法调用前已将null处理成""
                    String newSignature = (String) msg.obj;
                    //修改用户信息后重新请求用户信息
                    getUserInfo();
                    setSignature(newSignature);
                    break;
                case VER_AGREED:
                    refreshContact();
                    updateVerifications();
                    break;
            }
        }
    };

    //设置签名（可以为空）
    private void setSignature(String newSignature) {
        if (newSignature == null || newSignature.equals("")) {
            app_user_signature.setText(R.string.app_signature_hint);
        } else {
            app_user_signature.setText(newSignature);
        }
    }

    //刷新联系人数据
    public void refreshContact() {
        List<Fragment> fragments1 = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments1) {
            if (fragment instanceof GroupContactsFragment) {
                ((GroupContactsFragment) fragment).getUserGroupInfo(sh.getUid());
            }
            if (fragment instanceof AllContactsFragment) {
                ((AllContactsFragment) fragment).dataSetChanged();
            }
        }
    }

    //刷新消息卡片数据
    public void cardDataChanged() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof MsgFragment) {
                MsgFragment msgFragment = (MsgFragment) fragment;
                msgFragment.dataChanged();
                showOrUpdateMsgBadge();
                break;
            }
        }
    }

    //重写系统返回
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            //侧边栏展开则关闭侧边菜单  否则返回桌面
            if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
                mDrawerLayout.closeDrawer(mNavigationView);
            } else {
                ActivityUtils.startHomeActivity();
            }
        }
        return false;
    }

    //标志用户头像是否已经与本地加载
    private boolean isLocal;
    //加载用户信息
    public void loadUserInfo() {
        app_username.setText(user.getUserName());
        setSignature(user.getSignature());

        //加载头像
        headerPath = AppUtil.getImgBasePath(AppActivity.this) + File.separator + user.getHeader();
        isLocal = true;
        if (!FileUtils.isFileExists(headerPath)) {
            headerPath = IUserNetUtil.picIp + user.getHeader();
            isLocal = false;
        }
        Glide.with(AppActivity.this)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(headerPath)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        app_user_header.setImageDrawable(resource);
                        if (!isLocal) {
                            //非本地加载则保存本地
                            try {
                                AppUtil.saveFile(ImageUtils.drawable2Bitmap(resource), user.getHeader());
                                headerPath = AppUtil.getImgBasePath(AppActivity.this) + File.separator + user.getHeader();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
        updateMsgFgHeader();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XUI.initTheme(this);
        setContentView(R.layout.activity_app);
        ActivityUtils.finishAllActivitiesExceptNewest();
        initData();
        initView();
        initEvent();
        AppUtil.checkPermissions(AppActivity.this);
        bindService();
        doRegisterReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (nilDBHelper.isOpen()) {
            nilDBHelper.closeLink();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initData() {
        if (SPUtils.getInstance().getString("isFirstLogin").equals("")) {
            SPUtils.getInstance().put("isFirstLogin", "true");
            //通知、自启动和电池优化
            if (!NotificationUtil.isNotificationEnabled(AppActivity.this)) {
                NotificationUtil.goToNotificationSetting(AppActivity.this);
            }
//            AutoStartUtil.getAutostartSettingIntent(AppActivity.this);
//            BatteryManagementUtil.ignoreBatteryOptimization(AppActivity.this);
            //悬浮窗
            //提示
//            if (!PermissionUtils.isGrantedDrawOverlays()) {
//                DialogLoader.getInstance().showConfirmDialog(
//                        AppActivity.this, "开启悬浮窗通知以更好接收消息", "设置",
//                        (dialog, which) -> {
//                            //引导用户到设置中去进行设置
//                            PermissionUtils.requestDrawOverlays(new PermissionUtils.SimpleCallback() {
//                                @Override
//                                public void onGranted() {
//                                }
//
//                                @Override
//                                public void onDenied() {
//                                    ToastUtils.showShort("拒绝权限, 可能影响部分功能使用");
//                                }
//                            });
//                            dialog.dismiss();
//                        },
//                        "取消",
//                        (dialog, which) -> {
//                            dialog.dismiss();
//                        }
//                );
//            }

        }
        //获取token
        sh = SharedHelper.getInstance(NilApplication.getContext());
        token = sh.getToken();
        //获取sh中本地用户信息
        String json = sh.getUser();
        if (!json.equals("")) {
            user = GsonUtils.fromJson(json, User.class);
        }

        //云端同步用户信息
        getUserInfo();
        //获取用户账号
        mAccount = sh.getAccount();
        //init DB helper
        nilDBHelper = NilDBHelper.getInstance(NilApplication.getContext(), mAccount, 0);
        //同步验证信息
        updateVerifications();
    }

    private void bindService() {
        Intent bindIntent = new Intent(AppActivity.this, JWebSocketClientService.class);
        //BIND_AUTO_CREATE 自动创建服务只要连接存在
        bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void initEvent() {
        mRadioGroup.setOnCheckedChangeListener(this);
        drawer_header_layout.setOnClickListener(this);
        //点击设置个性签名
        app_user_signature_container.setOnClickListener(this);
        //未读消息条数拖动删除代理
        mRbMsg.setDragDismissDelegate(badgeable -> {
            clearUnreadNum();
        });
        //设置边缘距离
        setDrawerLeftEdgeSize(AppActivity.this, mDrawerLayout, 0.2f);

        menu_setting.setOnMenuItemClickListener((view) -> {
           startActivity(new Intent(this, SettingActivity.class));
           return true;
        });
        menu_scan.setOnMenuItemClickListener((view) -> {
            //申请权限并设置回调
            PermissionUtils.permission(Manifest.permission.CAMERA)
                    .callback(new PermissionUtils.SimpleCallback() {
                        @Override
                        public void onGranted() {
                            startActivity(new Intent(AppActivity.this, ScanActivity.class));
                        }

                        @Override
                        public void onDenied() {
                            //提示
                            DialogLoader.getInstance().showConfirmDialog(
                                    AppActivity.this, "需要开启相机权限才能使用此功能", "设置",
                                    (dialog, which) -> {
                                        //引导用户到设置中去进行设置
                                        PermissionUtils.launchAppDetailsSettings();
                                        dialog.dismiss();
                                    },
                                    "取消",
                                    (dialog, which) -> {
                                    }
                            );
                        }
                    })
                    .request();
            return true;
        });

        //用户二维码名片
        app_user_qrcode.setOnClickListener(this);
    }

    private void setDrawerLeftEdgeSize (Activity activity, DrawerLayout drawerLayout, float displayWidthPercentage) {
        if (activity == null || drawerLayout == null)
            return;
        try {
            // 找到 ViewDragHelper 并设置 Accessible 为true
            Field leftDraggerField =
                    drawerLayout.getClass().getDeclaredField("mLeftDragger");//Right
            leftDraggerField.setAccessible(true);
            ViewDragHelper leftDragger = (ViewDragHelper) leftDraggerField.get(drawerLayout);

            // 找到 edgeSizeField 并设置 Accessible 为true
            Field edgeSizeField = null;
            if (leftDragger != null) {
                edgeSizeField = leftDragger.getClass().getDeclaredField("mEdgeSize");
                edgeSizeField.setAccessible(true);
                int edgeSize = edgeSizeField.getInt(leftDragger);
                // 设置新的边缘大小
                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                edgeSizeField.setInt(leftDragger, Math.max(edgeSize, (int) (displaySize.x *
                        displayWidthPercentage)));
            }

        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initView() {
        //底部导航
        mRbMsg = findViewById(R.id.rb_msg);
        mRbContact = findViewById(R.id.rb_contact);
        mRbMore = findViewById(R.id.rb_more);

        showOrUpdateMsgBadge();
        showOrUpdateContactBadge();
        mRbMore.showCirclePointBadge();

        //设置drawableTop大小
        rbs = new ArrayList<>();
        rbs.add(mRbMsg);
        rbs.add(mRbContact);
        rbs.add(mRbMore);
        for (BGABadgeRadioButton rb : rbs) {
            Drawable[] drawables = rb.getCompoundDrawables();
            Rect r  = new Rect(0, 0, drawables[1].getMinimumWidth()*3/5, drawables[1].getMinimumHeight()*3/5);
            //定义边界
            drawables[1].setBounds(r);
            //添加限制给控件
            rb.setCompoundDrawables(null, drawables[1], null, null);
            //拖动删除
            rb.setDragDismissDelegate(badgeable -> rb.hiddenBadge());
        }

        mRadioGroup = findViewById(R.id.rg_menu);
        mViewPager = findViewById(R.id.vp2_app);
        //初始化viewPager
        mAdapter = new AppFragmentPagerAdapter(this);
        //预加载
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.setAdapter(mAdapter);
        mAdapter.addFragment(new MsgFragment());
        mAdapter.addFragment(new ContactFragment());
        mAdapter.addFragment(new MoreFragment());
        mViewPager.setCurrentItem(PAGE_ONE);
        //监听 ViewPager 2 的界面变化
        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setPageSelectedStatus(position);
            }
        });
        //禁止ViewPager滑动 不然与侧滑菜单有冲突
        mViewPager.setUserInputEnabled(false);
        //页面主体
        app_main = findViewById(R.id.app_main);
        //侧滑菜单
        mDrawerLayout = findViewById(R.id.app_drawer);
        mNavigationView = findViewById(R.id.app_nv);
        mNavigationView.setItemIconTintList(null);
        app_user_header = mNavigationView.getHeaderView(0).findViewById(R.id.app_user_header);
        app_username = mNavigationView.getHeaderView(0).findViewById(R.id.app_username);
        app_user_signature = mNavigationView.getHeaderView(0).findViewById(R.id.app_user_signature);
        app_user_signature_container = mNavigationView.getHeaderView(0).findViewById(R.id.app_user_signature_container);
        app_user_qrcode = mNavigationView.getHeaderView(0).findViewById(R.id.app_user_qrcode);

        //头布局容器
        drawer_header_layout = mNavigationView.getHeaderView(0).findViewById(R.id.drawer_header_layout);
        //设置
        menu_setting = mNavigationView.getMenu().getItem(4);
        menu_scan = mNavigationView.getMenu().getItem(3);
        //加载本地用户信息到UI
        if (user != null) {
            loadUserInfo();
        }

        //状态栏设置
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);//实现状态栏文字颜色为暗色
        getWindow().setStatusBarColor(getResources().getColor(R.color.trans));
        mNavigationView.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);

        //预加载图片选择器
        ImageSelector.preload(NilApplication.getContext());
    }

    public void showOrUpdateContactBadge() {
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openReadLink();
        }
        int count = nilDBHelper.queryCountState2(sh.getUid());
        if (count == 0 || count == -1) {
            mRbContact.hiddenBadge();
        } else {
            mRbContact.showTextBadge(String.valueOf(count));
        }

    }

    public int getBadge() {
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openReadLink();
        }
        return nilDBHelper.queryCountState2(sh.getUid());
    }

    @Override
    protected void onResume() {
        super.onResume();
        showOrUpdateContactBadge();
    }

    //查询本地数据库中最新消息卡片数据，统计它们未读消息条数总和并显示
    public void showOrUpdateMsgBadge() {
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openReadLink();
        }
        List<MsgCard> cardList = nilDBHelper.card_query_all_by_time();
        int sumUnread = 0;
        for (MsgCard card : cardList) {
            sumUnread += card.getUnreadNum();
        }
        if (sumUnread == 0) {
            mRbMsg.hiddenBadge();
        } else {
            mRbMsg.showTextBadge(String.valueOf(sumUnread).intern());
        }
    }

    //清零未读数量
    private void clearUnreadNum() {
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openWriteLink();
        }
        List<MsgCard> cardList = nilDBHelper.card_query_all_by_time();
        for (MsgCard card : cardList) {
            nilDBHelper.clear_unread_by_id(card.getCardId());
        }
        //导航栏隐藏badge
        mRbMsg.hiddenBadge();
        //通知MsGFragment信息数据改变
        cardDataChanged();
    }

    //更新fragment中头像
    private void updateMsgFgHeader() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof MsgFragment) {
                ((MsgFragment) fragment).loadHeader();
            }
            if (fragment instanceof ContactFragment) {
                ((ContactFragment) fragment).loadHeader();
            }
            if (fragment instanceof MoreFragment) {
                ((MoreFragment) fragment).loadHeader();
            }
        }
    }


    //获取用户信息
    public void getUserInfo() {
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        if (token == null || token.equals("")) {
            //token空，异常
            sendHandlerMsg(REQUEST_ERROR);
            return;
        }
        Call<ResponseBody> call = request.getUserInfo(token);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try{
                        assert response.body() != null;
                        String res = response.body().string();
                        //json转entity
                        Gson gson = new Gson();
                        user = gson.fromJson(res, User.class);
                        Log.d(TAG, "success---" + res);
                        sendHandlerMsg(LOAD_USER_SUCCESS);
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendHandlerMsg(REQUEST_ERROR);
                    }

                } else {
                    Log.d(TAG, "failed---code:" + response.code());
                    sendHandlerMsg(REQUEST_ERROR);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.d(TAG, "连接失败" + t);
                sendHandlerMsg(REQUEST_ERROR);
            }
        });
    }


    public static User getUser() {
        return user;
    }
    public static void setUser(User newUser) {
        user = newUser;
    }
    public static void setUserBg(String bg) {
        user.setBgImg(bg);
    }

    private void sendHandlerMsg(int state) {
        if (ActivityUtils.isActivityAlive(AppActivity.this)) {
            Message msg = new Message();
            msg.what = state;
            handler.sendMessage(msg);
        }
    }

    private void sendHandlerMsg(int state, Object obj) {
        if (ActivityUtils.isActivityAlive(AppActivity.this)) {
            Message msg = new Message();
            msg.what = state;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }

    //设置radioButton选中状态
    private void setPageSelectedStatus(int i) {
        for (int j = 0; j < 3; j++) {
            if (j == i) {
                rbs.get(j).setSelected(true);
                continue;
            }
            rbs.get(j).setSelected(false);
        }
    }

    public void openDrawer() {
        if (!mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        switch (i) {
            case R.id.rb_msg:
                //第二个参数false表示不需要滑动动画
                mViewPager.setCurrentItem(PAGE_ONE, false);
                break;
            case R.id.rb_contact:
                mViewPager.setCurrentItem(PAGE_TWO, false);
                break;
            case R.id.rb_more:
                mViewPager.setCurrentItem(PAGE_THREE, false);
                break;
        }
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.drawer_header_layout:
                if (user == null) {
                    ToastUtils.showShort("用户数据正在加载...");
                    return;
                }
                //用户数据以及加载，跳转用户个人信息界面
                Intent intent = new Intent(this, PersonalActivity.class);
                intent.putExtra("type", PersonalActivity.PERSONAL_SELF);
                startActivity(intent);

                break;
            case R.id.app_user_signature_container:
                //ToastUtils.showShort("点击了个性签名栏");
                new MaterialDialog.Builder(AppActivity.this)
                        .title(R.string.signature_modify)
                        .inputType(InputType.TYPE_CLASS_TEXT)
                        .input(
                                "个性签名",
                                user.getSignature(),
                                true,
                                ((dialog, input) -> LogUtils.d("修改个性签名为:" + input.toString())))
                        .inputRange(0, 52)
                        .positiveText("完成")
                        .negativeText("取消")
                        .onPositive(((dialog, which) -> {
                            String newSignature;
                            if (dialog.getInputEditText() == null) newSignature = "";
                            else newSignature = dialog.getInputEditText().getText().toString();
                            modifySignature(newSignature);
                        }))
                        .cancelable(true)
                        .show();
                break;
            case R.id.app_user_qrcode:
                startActivity(new Intent(this, VisitingCardActivity.class));
                break;
        }
    }

    //修改个性签名
    private void modifySignature(String newSignature) {
        int uid = sh.getUid();
        if (uid == -1) return;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.modifySignature(uid, newSignature);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    sendHandlerMsg(MODIFY_SIGNATURE_SUCCESS, newSignature);
                } else {
                    LogUtils.e("连接错误：" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败" + t);
            }
        });
    }

    //查询有无新的验证信息
    public void updateVerifications() {
        int uid = sh.getUid();
        if (uid == -1) return;
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openReadLink();
        }
        int endVid = nilDBHelper.queryLastVerId();
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.getVerificationsByUid(uid, endVid);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        Type type = new TypeToken<ArrayList<FriendVerification>>(){}.getType();
                        List<FriendVerification> list = GsonUtils.fromJson(res, type);
                        if (list == null || list.size() == 0) {
                            verLoaded = true;
                            return;
                        }
                        sendHandlerMsg(LOAD_VER_SUCCESS, list);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtils.e("请求错误，错误码" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败" + t);
            }
        });
        int startVid = nilDBHelper.queryFirstVerId();
        if (startVid > endVid) return;
        Call<ResponseBody> call1 = request.updateLocalVerifications(uid, startVid, endVid);
        LogUtils.d(startVid + " " + endVid);
        call1.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        Type type = new TypeToken<ArrayList<FriendVerification>>(){}.getType();
                        List<FriendVerification> list = GsonUtils.fromJson(res, type);
                        if (list == null || list.size() == 0) {
                            return;
                        }
                        sendHandlerMsg(UPDATE_VER_SUCCESS, list);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtils.e("请求错误，错误码" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败" + t);
            }
        });

    }

    //刷新动态数据
    public void refreshUpdates() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof MoreFragment) {
                ((MoreFragment) fragment).getUpdates();
            }
        }
    }
}