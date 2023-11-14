package cn.edu.hbpu.nil.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.donkingliang.groupedadapter.widget.StickyHeaderLayout;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.AppActivity;
import cn.edu.hbpu.nil.adapter.GroupedListAdapter;
import cn.edu.hbpu.nil.entity.Contact;
import cn.edu.hbpu.nil.entity.Group;
import cn.edu.hbpu.nil.entity.User;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.NilDBHelper;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GroupContactsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GroupContactsFragment extends Fragment {
    private static List<Group> groupList;
    private RecyclerView rv_group_contacts;
    private LinearLayoutManager linearLayoutManager;
    private GroupedListAdapter mAdapter;
    private StickyHeaderLayout headerLayout;
    private NilDBHelper nilDBHelper;
    private SharedHelper sharedHelper;
    private ExecutorService fixedPool;

    private int loadCount;

    //volatile避免编译器优化：相对顺序不变，变量操作对于不同线程实时可见
    private volatile boolean groupFlag = false;


    private static final int REQUEST_GROUP_SUCCESS = 0;
    private static final int UPDATE_GROUP_UI = 1;
    private static final int REQUEST_CONTACTS_SUCCESS = 2;
    private static final int GROUP_EMPTY = 3;

    Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REQUEST_GROUP_SUCCESS:
                    groupList = AppUtil.castList(msg.obj, Group.class);
                    //保存到本地数据库 为方便操作直接删除用户分组表再将新数据全部插入
                    if (!nilDBHelper.isOpen()) {
                        nilDBHelper.openWriteLink();
                    }
                    int i = nilDBHelper.group_delete_all();
                    LogUtils.d("删除group表" + i + "条数据");
                    long j = nilDBHelper.group_insert(groupList);
                    LogUtils.d("最后插入group的行号：" + j + "（-1为插入失败）");
                    updateGroupUI();
                    break;
                case UPDATE_GROUP_UI:
                    mAdapter.setGroups(groupList);
                    mAdapter.notifyDataChanged();
                    break;
                case REQUEST_CONTACTS_SUCCESS:
                    List<Contact> contacts = AppUtil.castList(msg.obj, Contact.class);
                    int index = contacts.get(0).getGroupIndex();
                    if (!nilDBHelper.isOpen()) {
                        nilDBHelper.openWriteLink();
                    }
                    //更新本地数据库对应组的联系人数据
                    nilDBHelper.contact_delete_by_index(index);
                    nilDBHelper.contact_insert(contacts);
                    if (groupList == null) return;
                    List<Group> tempGroups = mAdapter.getBackup();
                    tempGroups.get(index).setTotal(contacts.size());
                    int online = 0;
                    for (Contact contact : contacts) {
                        if (contact.getState().equals("在线")) {
                            online++;
                        }
                    }
                    tempGroups.get(index).setOnline(online);
                    tempGroups.get(index).setContactList(contacts);
                    if (mAdapter.isExpand(index)) {
                        //如果正展开的话就更新groupList和backup 否则只更新backup并且要保持total和online数据的正确性
                        mAdapter.setGroups(tempGroups);
                    } else {
                        groupList.get(index).setTotal(contacts.size());
                        groupList.get(index).setOnline(online);
                    }
                    mAdapter.updateBackup(tempGroups);
                    mAdapter.notifyGroupChanged(index);
                    mAdapter.notifyChildrenChanged(index);
                    //通过loadCount计数器判断是否加载完了所有组
                    isFinishedLoad();
                    break;
                case GROUP_EMPTY:
                    //该组没有联系人
                    int groupIndex = (int) msg.obj;
                    //更新本地数据库对应组的联系人数据
                    if (!nilDBHelper.isOpen()) {
                        nilDBHelper.openWriteLink();
                    }
                    nilDBHelper.contact_delete_by_index(groupIndex);
                    if (groupList == null) return;
                    List<Group> tempGroups1 = mAdapter.getBackup();
                    tempGroups1.get(groupIndex).setTotal(0);
                    tempGroups1.get(groupIndex).setOnline(0);
                    tempGroups1.get(groupIndex).setContactList(new ArrayList<>());
                    if (mAdapter.isExpand(groupIndex)) {
                        //如果正展开的话就更新groupList和backup 否则只更新backup并且要保持total和online数据的正确性
                        mAdapter.setGroups(tempGroups1);
                    } else {
                        groupList.get(groupIndex).setTotal(0);
                        groupList.get(groupIndex).setOnline(0);
                    }
                    mAdapter.updateBackup(tempGroups1);
                    mAdapter.notifyGroupChanged(groupIndex);
                    mAdapter.notifyChildrenChanged(groupIndex);
                    isFinishedLoad();
                    break;
            }
        }
    };

    private void isFinishedLoad() {
        if (++loadCount >= mAdapter.getBackup().size()) {
            AppActivity activity = (AppActivity) getActivity();
            if (activity != null) {
                activity.contactLoaded = true;
            }

        }
    }

    private void updateGroupUI() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (rv_group_contacts != null && mAdapter != null) {
                    //更新分组数据  UI操作需交给主线程操作
                    Message msg = Message.obtain();
                    msg.what = UPDATE_GROUP_UI;
                    handler.sendMessage(msg);
                    LogUtils.d("更新分组数据");
                    timer.cancel();
                }
            }
        };
        //不用设置超时， 这里是等view初始化
        timer.schedule(task, 0, 500);
    }

    //向外提供方法得到分组数据
    public static String[] getGroupList() {
        String[] strings = new String[groupList.size() - 1];
        for (int i = 1; i < groupList.size(); i++) {
            strings[i - 1] = groupList.get(i).getGroupName().intern();
        }
        return strings;
    }

    public GroupContactsFragment() {
        // Required empty public constructor
    }

    public static GroupContactsFragment newInstance() {
        return new GroupContactsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
        initView(view);
    }
    /*
     * 当你的activity调用onStop()方法, activity不再可见，并且应该释放那些不再需要的所有资源。
     * 一旦你的activity停止了，系统会在不再需要这个activity时摧毁它的实例。
     * 在极端情况下，系统会直接杀死你的app进程，并且不执行activity的onDestroy()回调方法, 因此你需要使用onStop()来释放资源，从而避免内存泄漏。(这点需要注意)
    */
    @Override
    public void onStop() {
        super.onStop();
        if (nilDBHelper.isOpen()) {
            nilDBHelper.closeLink();
        }
    }

    private void initData() {
        fixedPool = ThreadUtils.getFixedPool(4);
        sharedHelper = SharedHelper.getInstance(NilApplication.getContext());
        nilDBHelper = NilDBHelper.getInstance(NilApplication.getContext(), sharedHelper.getAccount(), 0);
        nilDBHelper.openReadLink();
        groupList = nilDBHelper.group_query_all();
        LogUtils.d(groupList);
        if (groupList.isEmpty()) {
            groupList.add(new Group(groupList.size(), "特别关心", 0, 0, new ArrayList<>(), false, groupList.size()));
            groupList.add(new Group(groupList.size(), "我的好友", 0, 0, new ArrayList<>(), false, groupList.size()));
        }
        //云端同步 特别关心在0组
        updateGroups();
    }

    private void updateGroups() {
        fixedPool.submit(() -> {
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (!groupFlag) {
                        User user = AppActivity.getUser();
                        if (user != null) {
                            groupFlag = true;
                            getUserGroupInfo(user.getUid());
                            timer.cancel();
                            LogUtils.d("已发送云端同步请求");
                        }
                    } else {
                        //超时
                        LogUtils.d("timer time out");
                        timer.cancel();
                    }
                }
            };
            //每0.5秒看user信息加载好木有
            timer.schedule(task, 0, 500);
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //30s超时
            groupFlag = true;
        });
    }

    //获取分组信息
    public void getUserGroupInfo(Integer uid) {
        loadCount = 0;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.getUserGroupInfo(uid);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        Type type = new TypeToken<ArrayList<Group>>(){}.getType();
                        List<Group> list = GsonUtils.fromJson(res, type);
                        //首次空分组需创建
                        if (list == null || list.size() == 0) list = groupList;

                        //contactList先同步本地数据，再同步云端
                        for (Group group : list) {
                            group.setContactList(new ArrayList<>());
                            updateWebContactList(uid, group.getGroupIndex());
                        }
                        LogUtils.d("成功请求分组信息:" + list);
                        sendMsg(REQUEST_GROUP_SUCCESS, list);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtils.e("连接错误，code=" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败:" + t);
            }
        });
    }
    //云端联系人信息
    private void updateWebContactList(int uid, int groupId) {
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.getContactsByGroupIndex(uid, groupId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        Type type = new TypeToken<ArrayList<Contact>>(){}.getType();
                        List<Group> list = GsonUtils.fromJson(res, type);
                        LogUtils.d("成功请求"+groupId+"组联系人信息:" + list);
                        if (list == null || list.size() == 0) {
                            sendMsg(GROUP_EMPTY, groupId);
                            return;
                        }
                        sendMsg(REQUEST_CONTACTS_SUCCESS, list);
                    } catch (Exception e) {
                        LogUtils.e("处理错误:" + e);
                    }
                } else {
                    LogUtils.e("请求错误:" + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败");
            }
        });
    }

    private void sendMsg(int state, Object obj) {
        Message msg = Message.obtain();
        msg.what = state;
        msg.obj = obj;
        handler.sendMessage(msg);
    }

    private void initView(View view) {
        rv_group_contacts = view.findViewById(R.id.rv_group_contacts);
        headerLayout = view.findViewById(R.id.sticky_header_contacts);
        //设置是否吸顶。
        headerLayout.setSticky(true);
        //initAdapter
        linearLayoutManager = new LinearLayoutManager(getContext());
        rv_group_contacts.setLayoutManager(linearLayoutManager);
        mAdapter = new GroupedListAdapter(getContext(), groupList);
        for (Group group : groupList) {
            //默认收起
            mAdapter.collapseGroup(group.getGroupId());
        }
        mAdapter.setOnHeaderClickListener((adapter, holder, groupPosition) -> {
            if (mAdapter.isExpand(groupPosition)) {
                holder.setImageResource(R.id.group_extended, R.mipmap.right_one);
                mAdapter.collapseGroup(groupPosition);
            } else {
                holder.setImageResource(R.id.group_extended, R.mipmap.down_one);
                mAdapter.expandGroup(groupPosition, true);
            }
        });
        mAdapter.setOnChildClickListener(((adapter, holder, groupPosition, childPosition) -> {
            ToastUtils.showShort("点击联系人" + groupList.get(groupPosition).getContactList().get(childPosition).getUserName());
        }));
        rv_group_contacts.setAdapter(mAdapter);


        //初始化联系人
        List<Group> tempGroups = mAdapter.getBackup();
        for (int i = 0; i < tempGroups.size(); i++) {
            int index = tempGroups.get(i).getGroupIndex();
            List<Contact> list = nilDBHelper.contactQueryByGroupIndex(index);
            if (list.isEmpty()) continue;
            tempGroups.get(i).setContactList(list);
        }
        mAdapter.updateBackup(tempGroups);
    }
}