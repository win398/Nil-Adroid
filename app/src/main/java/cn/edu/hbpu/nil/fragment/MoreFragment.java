package cn.edu.hbpu.nil.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

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
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.xuexiang.xui.utils.WidgetUtils;
import com.xuexiang.xui.widget.dialog.LoadingDialog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.AppActivity;
import cn.edu.hbpu.nil.activity.PersonalActivity;
import cn.edu.hbpu.nil.activity.UpdatePostActivity;
import cn.edu.hbpu.nil.adapter.UpdateAdapter;
import cn.edu.hbpu.nil.entity.Comment;
import cn.edu.hbpu.nil.entity.Like;
import cn.edu.hbpu.nil.entity.Page;
import cn.edu.hbpu.nil.entity.SocialUpdate;
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

public class MoreFragment extends Fragment implements View.OnClickListener {
    private View mView;
    private ShapeableImageView user_update_header, user_update_new;
    private RelativeLayout user_update_nav, user_update_card;
    private TextView user_update_username;
    private Context mContext;
    private RecyclerView user_update_rv;
    private List<SocialUpdate> updateList;
    private UpdateAdapter mAdapter;
    private int uid, pageNo;
    private SharedHelper sh;
    private SmartRefreshLayout more_refresh;
    private NilDBHelper nilDBHelper;
    private LoadingDialog loadingDialog;
    private User user;
    private LinearLayout user_update_none;

    private static final int REFRESH_SUCCESS = 0;
    private static final int REFRESH_FAIL = 1;
    private static final int LOAD_MORE_SUCCESS = 2;
    private static final int LOAD_MORE_FAIL = 3;
    private static final int DELETE_SUCCESS = 4;
    private static final int DELETE_FAIL = 5;
    private static final int COMMENT_SUCCESS = 6;
    private static final int COMMENT_FAIL = 7;
    private static final int LIKE_SUCCESS = 8;
    private static final int LIKE_FAIL = 9;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH_SUCCESS:
                    Page page = (Page) msg.obj;
                    pageNo++;
                    //更新数据库
                    if (!page.getRecords().isEmpty()) {
                        if (!nilDBHelper.isOpen()) nilDBHelper.openWriteLink();
                        nilDBHelper.socialUpdateDeleteAll();
                        long res = nilDBHelper.socialUpdateInsert(page.getRecords());
                    }
                    //更新UI
                    updateList.clear();
                    updateList.addAll(page.getRecords());
                    mAdapter.notifyDataSetChanged();
                    //完成刷新
                    if (pageNo > page.getPages()) {
                        //没有更多数据了
                        more_refresh.finishRefresh(0, true, true);
                    } else {
                        more_refresh.finishRefresh(500, true, false);
                    }
                    handleListEmpty();
                    break;
                case REFRESH_FAIL:
                    if (more_refresh.isRefreshing()) {
                        more_refresh.finishRefresh(false);
                    }
                    break;
                case LOAD_MORE_SUCCESS:
                    Page loadPage = (Page) msg.obj;
                    pageNo++;
                    //更新数据库
                    if (!loadPage.getRecords().isEmpty()) {
                        if (!nilDBHelper.isOpen()) nilDBHelper.openWriteLink();
                        nilDBHelper.socialUpdateInsert(loadPage.getRecords());
                    }
                    //更新UI
                    int start = updateList.size();
                    updateList.addAll(loadPage.getRecords());
                    mAdapter.notifyItemRangeInserted(start, loadPage.getRecords().size());
                    //完成加载
                    if (pageNo > loadPage.getPages()) {
                        //没有更多数据了
                        more_refresh.finishLoadMore(0, true, true);
                    } else {
                        more_refresh.finishLoadMore(0, true, false);
                    }
                    break;
                case LOAD_MORE_FAIL:
                    more_refresh.finishLoadMore(false);
                    break;
                case DELETE_SUCCESS:
                    //本地数据库更新
                    mAdapter.notifyDataSetChanged();
                    int[] params = (int[]) msg.obj;
                    int sid = params[0], position = params[1];
                    if (!nilDBHelper.isOpen()) nilDBHelper.openWriteLink();
                    nilDBHelper.socialUpdateDeleteBySid(sid);
                    updateList.remove(position);
                    mAdapter.notifyItemRemoved(position);
                    loadingDialog.dismiss();
                    break;
                case DELETE_FAIL:
                    ToastUtils.showShort("删除失败");
                    loadingDialog.dismiss();
                    break;
                case COMMENT_SUCCESS:
                    //0 sid 1 position 2content 3username
                    String[] strings = (String[]) msg.obj;
                    int commentSid = Integer.parseInt(strings[0]), commentPos = Integer.parseInt(strings[1]);
                    //保存数据库
                    if (!nilDBHelper.isOpen()) nilDBHelper.openWriteLink();
                    Comment comment = new Comment();
                    comment.setSid(commentSid);
                    comment.setContentText(strings[2]);
                    comment.setUsername(strings[3]);
                    ArrayList<Comment> comments = new ArrayList<>();
                    comments.add(comment);
                    nilDBHelper.updateCommentInsert(comments, commentSid);
                    //更新adapter数据
                    List<Comment> comList = updateList.get(commentPos).getComments();
                    comList.add(comment);
                    updateList.get(commentPos).setComments(comList);
                    //更新UI
                    mAdapter.notifyDataSetChanged();
                    loadingDialog.dismiss();
                    break;
                case COMMENT_FAIL:
                    ToastUtils.showShort("提交失败");
                    loadingDialog.dismiss();
                    break;
                case LIKE_SUCCESS:
                    LogUtils.d("操作成功");
                    break;
                case LIKE_FAIL:
                    LogUtils.d("操作失败");
                    int[] likeFail = (int[]) msg.obj;
                    opLocalData(likeFail[0], likeFail[1], likeFail[2]);
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fg_content_more, container, false);
        initData();
        initView();
        initEvent();
        return mView;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (nilDBHelper.isOpen()) {
            nilDBHelper.closeLink();
        }
    }

    private void initData() {
        sh = SharedHelper.getInstance(NilApplication.getContext());
        user = AppActivity.getUser();
        nilDBHelper = NilDBHelper.getInstance(NilApplication.getContext(), sh.getAccount(), 0);
        uid = sh.getUid();
        pageNo = 1;
        mContext = getContext();
        if (!nilDBHelper.isOpen()) {
            nilDBHelper.openReadLink();
        }
        updateList = nilDBHelper.updateQueryAll(sh.getUid());

        //云数据同步
        getUpdates();

    }

    private void initView() {
        user_update_header = mView.findViewById(R.id.user_update_header);
        user_update_username = mView.findViewById(R.id.user_update_username);
        user_update_nav = mView.findViewById(R.id.user_update_nav);
        user_update_card = mView.findViewById(R.id.user_update_card);
        user_update_new = mView.findViewById(R.id.user_update_new);
        user_update_rv = mView.findViewById(R.id.user_update_rv);
        more_refresh = mView.findViewById(R.id.more_refresh);
        user_update_none = mView.findViewById(R.id.user_update_none);

        //empty
        handleListEmpty();

        initRefreshLayout();

        //init rv
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        mAdapter = new UpdateAdapter(updateList, mContext, this);
        user_update_rv.setLayoutManager(layoutManager);
        user_update_rv.setAdapter(mAdapter);
        //防止notifyItemChanged时屏幕自动滑一下
        ((SimpleItemAnimator) Objects.requireNonNull(user_update_rv.getItemAnimator())).setSupportsChangeAnimations(false);

        if (user != null) {
            user_update_username.setText(user.getUserName());
        }
        loadHeader();

        //加载框
        loadingDialog = WidgetUtils.getLoadingDialog(mContext)
                .setLoadingSpeed(5)
                .setIconScale(0.5F);

        //状态栏设置
        user_update_nav.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
    }

    //处理列表空 显示空页面
    private void handleListEmpty() {
        if (updateList.isEmpty()) {
            user_update_none.setVisibility(View.VISIBLE);
            user_update_rv.setVisibility(View.GONE);
        } else {
            user_update_none.setVisibility(View.GONE);
            user_update_rv.setVisibility(View.VISIBLE);
        }
    }

    private void initRefreshLayout() {
        more_refresh.setOnRefreshListener(refreshLayout -> getUpdates());
        more_refresh.setEnableLoadMore(true);
        more_refresh.setOnLoadMoreListener(refreshLayout -> loadMoreUpdates());
    }

    //加载头像
    private boolean isLocal;
    public void loadHeader() {
        if (user == null) return;
        String header = user.getHeader();
        String path = AppUtil.getImgBasePath(mContext) + File.separator + header;
        isLocal = true;
        if (!FileUtils.isFileExists(path)) {
            isLocal = false;
            path = IUserNetUtil.picIp + header;
        }
        Glide.with(mContext)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(path)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        user_update_header.setImageDrawable(resource);
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
        user_update_card.setOnClickListener(this);
        user_update_new.setOnClickListener(this);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.user_update_card:
                Intent intent = new Intent(mContext, PersonalActivity.class);
                intent.putExtra("type", PersonalActivity.PERSONAL_SELF);
                startActivity(intent);
                break;
            case R.id.user_update_new:
                startActivity(new Intent(mContext, UpdatePostActivity.class));
                break;
        }
    }


    //云端获取好友动态数据
    public void getUpdates() {
        if (uid == -1) return;
        //刷新数据重新置1
        pageNo = 1;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.getUpdates(uid, pageNo);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        if (response.body() == null) return;
                        String res = response.body().string();
                        Page page = GsonUtils.fromJson(res, Page.class);
                        sendMsg(REFRESH_SUCCESS, page);
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendMsg(REFRESH_FAIL);
                    }
                } else {
                    LogUtils.d("连接错误：" + response.code());
                    sendMsg(REFRESH_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
                sendMsg(REFRESH_FAIL);
            }
        });

    }

    //加载更多
    private void loadMoreUpdates() {
        if (uid == -1) return;
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.getUpdates(uid, pageNo);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        if (response.body() == null) return;
                        String res = response.body().string();
                        Page page = GsonUtils.fromJson(res, Page.class);
                        sendMsg(LOAD_MORE_SUCCESS, page);
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendMsg(LOAD_MORE_FAIL);
                    }
                } else {
                    LogUtils.d("连接错误：" + response.code());
                    sendMsg(LOAD_MORE_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
                sendMsg(LOAD_MORE_FAIL);
            }
        });
    }

    public void deleteSocialUpdate(int sid, int position) {
        loadingDialog.updateMessage("删除中…");
        loadingDialog.show();
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.deleteSocialUpdate(sid);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    sendMsg(DELETE_SUCCESS, new int[]{sid, position});
                } else {
                    LogUtils.d("连接错误：" + response.code());
                    sendMsg(DELETE_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
                sendMsg(DELETE_FAIL);
            }
        });
    }

    public void submitComment(int sid, int position, String content) {
        if (uid == -1) return;
        loadingDialog.updateMessage("提交评论…");
        loadingDialog.show();
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call = request.submitComment(uid, sid, content);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    if (response.body() == null) return;
                    try {
                        String res = response.body().string();
                        if (res.equals("fail")) sendMsg(COMMENT_FAIL);
                        else {
                            sendMsg(COMMENT_SUCCESS, new String[]{String.valueOf(sid), String.valueOf(position), content, res});
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendMsg(COMMENT_FAIL);
                    }
                } else {
                    LogUtils.d("连接错误：" + response.code());
                    sendMsg(COMMENT_FAIL);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
                sendMsg(COMMENT_FAIL);
            }
        });
    }

    private void sendMsg(int what) {
        if (ActivityUtils.isActivityAlive(mContext)) {
            Message msg = Message.obtain();
            msg.what = what;
            handler.sendMessage(msg);
        }
    }

    private void sendMsg(int what, Object obj) {
        if (ActivityUtils.isActivityAlive(mContext)) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            handler.sendMessage(msg);
        }
    }

    //点赞
    public void likeUpdate(int uid, int sid, int position, boolean hasLiked) {
        //先更新本地数据库数据
        opLocalData(uid, sid, position);
        IUserNetUtil request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        Call<ResponseBody> call;
        if (hasLiked) {
            call = request.cancelLike(uid, sid);
        } else {
            call = request.likeDynamic(uid, sid);
        }
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    if (response.body() == null) return;
                    try {
                        String res = response.body().string();
                        LogUtils.d(res);
                        if (res.equals("true")) sendMsg(LIKE_SUCCESS);
                        else sendMsg(LIKE_FAIL, new int[]{uid, sid, position});
                    } catch (IOException e) {
                        e.printStackTrace();
                        sendMsg(LIKE_FAIL, new int[]{uid, sid, position});
                    }
                } else {
                    LogUtils.d("连接错误：" + response.code());
                    sendMsg(LIKE_FAIL, new int[]{uid, sid, position});
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.d("连接失败" + t);
                sendMsg(LIKE_FAIL, new int[]{uid, sid, position});
            }
        });
    }

    //点赞操作 本地数据变化
    private void opLocalData(int uid, int sid, int position) {
        if (!nilDBHelper.isOpen()) nilDBHelper.openWriteLink();
        if (updateList.get(position).isLike()) {
            nilDBHelper.updateLikeDeleteBySidAndUid(sid, uid);
        } else {
            Like like = new Like();
            like.setSid(sid);
            like.setUid(uid);
            like.setUsername(AppActivity.getUser().getUserName());
            List<Like> likes = new ArrayList<>();
            likes.add(like);
            nilDBHelper.updateLikeInsert(likes);
        }
        updateList.get(position).setLike(!updateList.get(position).isLike());
        updateList.get(position).setLikes(nilDBHelper.updateLikeQueryBySid(sid));
        mAdapter.notifyItemChanged(position);
    }
}
