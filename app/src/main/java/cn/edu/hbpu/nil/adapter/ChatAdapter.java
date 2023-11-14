package cn.edu.hbpu.nil.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.constant.TimeConstants;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.entity.ChatMsg;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.other.TimeUtil;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    public static int TYPE_LEFT_ITEM = 0;
    public static int TYPE_RIGHT_ITEM = 1;
    private List<ChatMsg> msgList;
    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private String mAccount;
    private String mHeader, contactHeader;
    private String TAG = "---ChatAdapter---";
    private boolean isLoaded;


    public ChatAdapter(List<ChatMsg> list, Context context, String account, String mHeader, String contactHeader) {
        this.msgList = list;
        this.mContext = context;
        this.mAccount = account;
        this.mLayoutInflater = LayoutInflater.from(mContext);
        this.mHeader = mHeader;
        this.contactHeader = contactHeader;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMsg msg = msgList.get(position);
        if (msg.getSendAccount().equals(this.mAccount)) {
            return TYPE_RIGHT_ITEM;
        } else {
            return TYPE_LEFT_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        //Log.d(TAG + String.valueOf(viewType), "hhh");
        if (viewType == TYPE_LEFT_ITEM) {
            view = mLayoutInflater.inflate(R.layout.item_chat_left, null, false);
            return new LeftViewHolder(view);
        }
        view = mLayoutInflater.inflate(R.layout.item_chat_right, null, false);
        return new RightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMsg msg = msgList.get(position);
        boolean isGone = false;
        if (position >= 1) {
            Date datePre = TimeUtils.string2Date(msgList.get(position - 1).getSendTime());
            Date dateCur = TimeUtils.string2Date(msgList.get(position).getSendTime());
            //与上一个消息间隔时间小于3分钟不显示时间
            if (TimeUtils.getTimeSpan(dateCur, datePre, TimeConstants.MIN) <= 3) {
                isGone = true;
            }
        }
        //Log.d(TAG, "position:" + position + ", isGone:" + isGone);
        if (getItemViewType(position) == TYPE_LEFT_ITEM) {
            LeftViewHolder leftViewHolder = (LeftViewHolder) holder;
            leftViewHolder.left_content.setText(msg.getMsgContent());
            if (isGone) {
                leftViewHolder.chat_time.setVisibility(View.GONE);
            } else {
                leftViewHolder.chat_time.setVisibility(View.VISIBLE);
                //由于服务端ws用hutool格式化json，时间格式为字符串时间戳，这里需要转时间戳为date
                Date sendTimeDate = TimeUtils.string2Date(msg.getSendTime());
                leftViewHolder.chat_time.setText(TimeUtil.dateFormatByNow(sendTimeDate));
                Log.d(TAG, "position:" + position + ", isGone:" + isGone + ", time:" + TimeUtil.dateFormatByNow(sendTimeDate));
            }
            //加载头像  本地不存在则从云端加载并保存本地
            String path = AppUtil.getImgBasePath(mContext) + File.separator + contactHeader;
            isLoaded = true;
            if (!FileUtils.isFileExists(path)) {
                path = IUserNetUtil.picIp + contactHeader;
                isLoaded = false;
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
                            leftViewHolder.left_header.setImageDrawable(resource);
                            if (!isLoaded) {
                                try {
                                    AppUtil.saveFile(ImageUtils.drawable2Bitmap(resource), contactHeader);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        } else if (getItemViewType(position) == TYPE_RIGHT_ITEM) {
            RightViewHolder rightViewHolder = (RightViewHolder) holder;
            rightViewHolder.right_content.setText(msg.getMsgContent());
            if (isGone) {
                rightViewHolder.chat_time.setVisibility(View.GONE);
            } else {
                rightViewHolder.chat_time.setVisibility(View.VISIBLE);
                Date sendTimeDate = TimeUtils.string2Date(msg.getSendTime());
                rightViewHolder.chat_time.setText(TimeUtil.dateFormatByNow(sendTimeDate));
                Log.d(TAG, "position:" + position + ", isGone:" + isGone + ", time:" + TimeUtil.dateFormatByNow(sendTimeDate));
            }
            //加载头像  本地不存在则从云端加载并保存本地
            String path = AppUtil.getImgBasePath(mContext) + File.separator + mHeader;
            isLoaded = true;
            if (!FileUtils.isFileExists(path)) {
                path = IUserNetUtil.picIp + mHeader;
                isLoaded = false;
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
                            rightViewHolder.right_header.setImageDrawable(resource);
                            try {
                                AppUtil.saveFile(ImageUtils.drawable2Bitmap(resource), mHeader);
                                if (!isLoaded) {
                                    try {
                                        AppUtil.saveFile(ImageUtils.drawable2Bitmap(resource), mHeader);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return msgList.size();
    }

    static class LeftViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView left_header;
        TextView left_content, chat_time;

        public LeftViewHolder(@NonNull View itemView) {
            super(itemView);
            this.left_header = itemView.findViewById(R.id.left_header);
            this.left_content = itemView.findViewById(R.id.left_content);
            this.chat_time = itemView.findViewById(R.id.chat_time_left);
        }
    }

    static class RightViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView right_header;
        TextView right_content, chat_time;

        public RightViewHolder(@NonNull View itemView) {
            super(itemView);
            this.right_header = itemView.findViewById(R.id.right_header);
            this.right_content = itemView.findViewById(R.id.right_content);
            this.chat_time = itemView.findViewById(R.id.chat_time_right);
        }
    }
}
