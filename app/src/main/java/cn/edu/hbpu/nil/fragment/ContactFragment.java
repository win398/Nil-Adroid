package cn.edu.hbpu.nil.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.constant.RefreshState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import cn.bingoogolapple.badgeview.BGABadgeRelativeLayout;
import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.AddContactActivity;
import cn.edu.hbpu.nil.activity.AppActivity;
import cn.edu.hbpu.nil.activity.ContactSearchActivity;
import cn.edu.hbpu.nil.activity.NewFriendActivity;
import cn.edu.hbpu.nil.adapter.ContactPagerAdapter;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;

public class ContactFragment extends Fragment implements View.OnClickListener {
    private View mView;
    private AppActivity mActivity;
    private LinearLayout content_contact;
    public static ViewPager2 contact_vp2;
    private TabLayout contact_tab;
    private List<Fragment> fragmentList;
    private ContactPagerAdapter mAdapter;
    private List<String> tabsTitle;
    private EditText contact_search;
    private ImageView go_to_contact_add;
    private ShapeableImageView contact_nav_header;
    public boolean headerLoaded = false;
    private ExecutorService fixedPool;

    private RefreshLayout contact_refresh;

    private BGABadgeRelativeLayout contact_fun_new_friend;

    private static final int LOAD_HEADER_DIRECT = 0;
    private static final int LOADED_CONTACT = 1;
    private static final int FAIL_REFRESH = 2;

    public final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case LOAD_HEADER_DIRECT:
                    //操作UI需在主线程
                    if (ActivityUtils.isActivityAlive(mActivity)) {
                        loadHeader();
                    }
                    break;
                case LOADED_CONTACT:
                    if (contact_refresh.getState() == RefreshState.Refreshing) {
                        contact_refresh.finishRefresh(true);
                    }
                    break;
                case FAIL_REFRESH:
                    if (contact_refresh.getState() == RefreshState.Refreshing) {
                        contact_refresh.finishRefresh(false);
                    }
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fg_content_contact, container, false);
        initData();
        initView();
        initEvent();
        return mView;
    }

    private void initData() {
        fragmentList = new ArrayList<>();
        fragmentList.add(AllContactsFragment.newInstance());
        fragmentList.add(GroupContactsFragment.newInstance());
        tabsTitle = new ArrayList<>();
        tabsTitle.add("好友");
        tabsTitle.add("分组");
        fixedPool = ThreadUtils.getFixedPool(4);
    }


    private void initView() {
        contact_refresh = mView.findViewById(R.id.contact_refresh);

        //给标签栏让位置
        mActivity = (AppActivity) getActivity();
        content_contact = mView.findViewById(R.id.content_contact);
        content_contact.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
        //搜索栏
        contact_search = mView.findViewById(R.id.contact_search);
        //主体标签栏
        contact_vp2 = mView.findViewById(R.id.contact_vp2);
        contact_tab = mView.findViewById(R.id.contact_tab);
        //设置点击透明
        for (int i = 0; i < 2; i++) {
            contact_tab.addTab(contact_tab.newTab());
        }
        mAdapter = new ContactPagerAdapter(mActivity, fragmentList);
        contact_vp2.setAdapter(mAdapter);
        //默认显示分组列表（第2页，下标为1）
        contact_vp2.setCurrentItem(1, false);
        //导航栏
        go_to_contact_add = mView.findViewById(R.id.go_to_contact_add);
        contact_nav_header = mView.findViewById(R.id.contact_nav_header);
        //加载头像
        updateHeader();

        //新朋友
        contact_fun_new_friend = mView.findViewById(R.id.contact_fun_new_friend);
        setBadge(mActivity.getBadge());

        //下拉刷新
        contact_refresh.setOnRefreshListener(refreshLayout -> {
            mActivity.contactLoaded = false;
            mActivity.verLoaded = false;
            //刷新验证信息和联系人数据
            mActivity.updateVerifications();
            mActivity.refreshContact();
            checkLoaded();
        });
    }

    private void checkLoaded() {
        fixedPool.submit(() -> {
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (mActivity.verLoaded && mActivity.contactLoaded) {
                        Message msg = Message.obtain();
                        msg.what = LOADED_CONTACT;
                        handler.sendMessage(msg);
                        timer.cancel();;
                    }
                }
            };
            //0.2s看刷新好没有
            timer.schedule(task, 0, 200);
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Message msg = Message.obtain();
            msg.what = FAIL_REFRESH;
            handler.sendMessage(msg);
            //5s超时
            timer.cancel();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setBadge(mActivity.getBadge());
    }

    public void setBadge(int count) {
        if (count == 0 || count == -1) {
            contact_fun_new_friend.hiddenBadge();
        } else {
            contact_fun_new_friend.showTextBadge(String.valueOf(count));
        }
    }

    private void initEvent() {
        //使用TabLayoutMeditor将TabLayout和ViewPager组合起来。
        TabLayoutMediator mediator = new TabLayoutMediator(contact_tab, contact_vp2, (tab, position) -> tab.setText(tabsTitle.get(position)));
        mediator.attach();

        //跳转搜索页
        contact_search.setOnClickListener(this);
        //跳转添加联系人页
        go_to_contact_add.setOnClickListener(this);
        //打开抽屉
        contact_nav_header.setOnClickListener(this);
        //新朋友验证
        contact_fun_new_friend.setOnClickListener(this);
    }

    //更新头像
    public void updateHeader() {
        fixedPool.submit(() -> {
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (!headerLoaded) {
                        Message msg = Message.obtain();
                        msg.what = LOAD_HEADER_DIRECT;
                        handler.sendMessage(msg);
                    } else {
                        timer.cancel();
                    }
                }
            };
            //立即执行一次，以后1s加载一次头像(如未加载)
            timer.schedule(task, 0, 1000);
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //30s超时
            timer.cancel();
        });

    }

    //加载头像
    private boolean isLocal;
    public void loadHeader() {
        if (AppActivity.getUser() == null) return;
        String header = AppActivity.getUser().getHeader();
        String path = AppUtil.getImgBasePath(mActivity) + File.separator + header;
        isLocal = true;
        if (!FileUtils.isFileExists(path)) {
            isLocal = false;
            path = IUserNetUtil.picIp + header;
        }
        Glide.with(mActivity)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(path)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        contact_nav_header.setImageDrawable(resource);
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

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.contact_search:
                startActivity(new Intent(getContext(), ContactSearchActivity.class));
                break;
            case R.id.go_to_contact_add:
                startActivity(new Intent(getContext(), AddContactActivity.class));
                break;
            case R.id.contact_nav_header:
                mActivity.openDrawer();
                break;
            case R.id.contact_fun_new_friend:
                startActivity(new Intent(getActivity(), NewFriendActivity.class));
                break;

        }
    }
}
