package cn.edu.hbpu.nil.adapter;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.StringUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.AddedFriendActivity;
import cn.edu.hbpu.nil.activity.AppActivity;
import cn.edu.hbpu.nil.entity.FriendVerification;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.MaskUtil;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;
import cn.edu.hbpu.nil.util.web.RetrofitManager;
import es.dmoral.toasty.Toasty;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewFriendAdapter extends RecyclerView.Adapter<NewFriendAdapter.ViewHolder> {
    private List<FriendVerification> verifications;
    private Context mContext;
    private LayoutInflater inflater;
    private IUserNetUtil request;
    private ProgressDialog progressDialog;
    private SharedHelper sh;

    private static final int DISMISS_DIALOG = 0;
    private static final int AGREED_DATA_CHANGED = 1;
    private static final int REFUSED_DATA_CHANGED = 2;
    private static final int REQUEST_FAILED = 3;

    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DISMISS_DIALOG:
                    dismissDialog();
                    break;
                case AGREED_DATA_CHANGED:
                    int position = (int) msg.obj;
                    verifications.get(position).setVerifyState(3);
                    notifyItemChanged(position);
                    /*可自定义
                      Toasty.custom(yourContext, "I'm a custom Toast", yourIconDrawable, tintColor, duration, withIcon,
                      shouldTint).show();
                     */
                    //广播，通知主页面刷新联系人数据
                    Intent intent = new Intent();
                    intent.setAction("cn.edu.hbpu.refreshContacts");
                    mContext.sendBroadcast(intent);
                    Toasty.success(mContext, "添加成功", Toast.LENGTH_SHORT, true).show();
                    Intent intent1 = new Intent(mContext, AddedFriendActivity.class);
                    FriendVerification verification = verifications.get(position);
                    intent1.putExtra("fromUid", verification.getFromUid());
                    intent1.putExtra("toUid", verification.getToUid());
                    intent1.putExtra("fromUserName", verification.getUserName());
                    intent1.putExtra("fromUserHeader", verification.getHeader());
                    intent1.putExtra("verContent", verification.getContent());
                    mContext.startActivity(intent1);
                    break;
                case REFUSED_DATA_CHANGED:
                    int position1 = (int) msg.obj;
                    verifications.get(position1).setVerifyState(4);
                    notifyItemChanged(position1);
                    Toasty.success(mContext, "已拒绝", Toast.LENGTH_SHORT, true).show();
                    break;
                case REQUEST_FAILED:
                    Toasty.error(mContext, "请求失败", Toast.LENGTH_SHORT, true).show();
                    break;
            }
        }
    };


    public NewFriendAdapter(List<FriendVerification> verifications, Context mContext) {
        this.verifications = verifications;
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
        request = RetrofitManager.getInstance().getRetrofit().create(IUserNetUtil.class);
        sh = SharedHelper.getInstance(NilApplication.getContext());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_new_friend_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final FriendVerification verification = verifications.get(position);
        String vName, vHeader, vContent;
        if (verification.getFromUid() == sh.getUid()) {
            vName = verification.getToUserName();
            vHeader = verification.getToUserHeader();
            vContent = "请求添加对方为好友";
            LogUtils.d(verification);
        } else {
            vName = verification.getUserName();
            vHeader = verification.getHeader();
            vContent = verification.getContent();
        }
        holder.new_friend_username.setText(vName);
        holder.new_friend_ver_info.setText(vContent);
        //头像不空
        if (!StringUtils.isEmpty(vHeader)) {
            Glide.with(mContext)
                    .setDefaultRequestOptions(new RequestOptions()
                            .centerCrop()
                            .placeholder(R.mipmap.loadingheader)
                            .fitCenter()
                    )
                    .load(IUserNetUtil.picIp + vHeader)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            holder.new_friend_header.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }
        //状态 1未验证 2已读 3通过 4拒绝
        int state = verification.getVerifyState();
        if (state == 1 || state == 2) {
            holder.new_friend_agreed.setVisibility(View.GONE);
            holder.new_friend_refused.setVisibility(View.GONE);
            if (verification.getFromUid() == sh.getUid()) {
                //如果是用户发送的请求，这里应该显示等待验证
                holder.new_friend_waiting_ver.setVisibility(View.VISIBLE);
                holder.new_friend_btn_agree.setVisibility(View.GONE);
                holder.new_friend_btn_refuse.setVisibility(View.GONE);
            } else {
                holder.new_friend_waiting_ver.setVisibility(View.GONE);
                holder.new_friend_btn_agree.setVisibility(View.VISIBLE);
                holder.new_friend_btn_refuse.setVisibility(View.VISIBLE);
            }
        } else if (state == 3) {
            holder.new_friend_btn_agree.setVisibility(View.GONE);
            holder.new_friend_btn_refuse.setVisibility(View.GONE);
            holder.new_friend_agreed.setVisibility(View.VISIBLE);
            holder.new_friend_refused.setVisibility(View.GONE);
            holder.new_friend_waiting_ver.setVisibility(View.GONE);
        } else if (state == 4) {
            holder.new_friend_btn_agree.setVisibility(View.GONE);
            holder.new_friend_btn_refuse.setVisibility(View.GONE);
            holder.new_friend_agreed.setVisibility(View.GONE);
            holder.new_friend_refused.setVisibility(View.VISIBLE);
            holder.new_friend_waiting_ver.setVisibility(View.GONE);
        }
        holder.new_friend_btn_refuse.setOnClickListener(view -> {
            refuseVerification(verification.getFromUid(), verification.getToUid(), position);
        });
        holder.new_friend_btn_agree.setOnClickListener(view -> {
            agreeVerification(verification.getFromUid(), verification.getToUid(), position);
        });
    }

    private void agreeVerification(int fromUid, int toUid, int position) {
        Call<ResponseBody> call = request.agreeVerification(fromUid, toUid);
        progressDialog = MaskUtil.showProgressDialog("同意中...", mContext);
        progressDialog.show();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        if (res.equals("success")) {
                            sendMsg(AGREED_DATA_CHANGED, position);
                        } else {
                            LogUtils.e("处理失败，请重试");
                            sendMsg(REQUEST_FAILED);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtils.e("请求错误，错误码:" + response.code());
                    sendMsg(REQUEST_FAILED);
                }
                sendMsg(DISMISS_DIALOG);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败" + t);
                sendMsg(DISMISS_DIALOG);
                sendMsg(REQUEST_FAILED);
            }
        });
    }

    private void refuseVerification(int fromUid, int toUid, int position) {
        Call<ResponseBody> call = request.refuseVerification(fromUid, toUid);
        progressDialog = MaskUtil.showProgressDialog("拒绝中...", mContext);
        progressDialog.show();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.code() == 200) {
                    try {
                        assert response.body() != null;
                        String res = response.body().string();
                        if (res.equals("success")) {
                            sendMsg(REFUSED_DATA_CHANGED, position);
                        } else {
                            LogUtils.e("处理失败，请重试");
                            sendMsg(REQUEST_FAILED);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    LogUtils.e("请求错误，错误码:" + response.code());
                    sendMsg(REQUEST_FAILED);
                }
                sendMsg(DISMISS_DIALOG);
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LogUtils.e("连接失败" + t);
                sendMsg(DISMISS_DIALOG);
                sendMsg(REQUEST_FAILED);
            }
        });
    }

    private void dismissDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void sendMsg(int state) {
        Message msg = Message.obtain();
        msg.what = state;
        mHandler.sendMessage(msg);
    }

    private void sendMsg(int state, Object obj) {
        Message msg = Message.obtain();
        msg.what = state;
        msg.obj = obj;
        mHandler.sendMessage(msg);
    }

    @Override
    public int getItemCount() {
        return verifications == null ? 0 : verifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView new_friend_header;
        TextView new_friend_username, new_friend_ver_info;
        MaterialCardView new_friend_btn_refuse, new_friend_btn_agree;
        TextView new_friend_refused, new_friend_agreed, new_friend_waiting_ver;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            new_friend_header = itemView.findViewById(R.id.new_friend_header);
            new_friend_username = itemView.findViewById(R.id.new_friend_username);
            new_friend_ver_info = itemView.findViewById(R.id.new_friend_ver_info);
            new_friend_btn_refuse = itemView.findViewById(R.id.new_friend_btn_refuse);
            new_friend_btn_agree = itemView.findViewById(R.id.new_friend_btn_agree);
            new_friend_refused = itemView.findViewById(R.id.new_friend_refused);
            new_friend_agreed = itemView.findViewById(R.id.new_friend_agreed);
            new_friend_waiting_ver = itemView.findViewById(R.id.new_friend_waiting_ver);
        }
    }
}
