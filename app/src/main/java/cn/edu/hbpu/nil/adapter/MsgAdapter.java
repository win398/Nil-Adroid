package cn.edu.hbpu.nil.adapter;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cn.bingoogolapple.badgeview.BGABadgeLinearLayout;
import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.AppActivity;
import cn.edu.hbpu.nil.activity.ChatActivity;
import cn.edu.hbpu.nil.entity.MsgCard;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.NilDBHelper;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;

public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.mHolder> {
    private List<MsgCard> mData;
    private LayoutInflater inflater;
    private AppActivity mContext;
    private NilDBHelper nilDBHelper;

    public MsgAdapter(List<MsgCard> cards, AppActivity mContext, NilDBHelper nilDBHelper) {
        this.mData = cards;
        this.mContext = mContext;
        this.inflater = LayoutInflater.from(mContext);
        this.nilDBHelper = nilDBHelper;
    }
    @NonNull
    @Override
    public mHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_msg, parent, false);
        return new mHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull mHolder holder, int position) {
        MsgCard msgCard = mData.get(position);
        holder.msg_item_time.setText(msgCard.getLastTime());
        holder.msg_item_name.setText(msgCard.getSenderName());
        holder.msg_item_content.setText(msgCard.getLastContent());
        holder.msg_item_container.setOnClickListener(view -> {
            //ToastUtils.showShort("来自" + msgCard.getSenderAccount() + "的消息");
            Intent intent = new Intent(mContext, ChatActivity.class);
            intent.putExtra("sender", msgCard.getSenderAccount());
            intent.putExtra("contactName", msgCard.getSenderName());
            intent.putExtra("contactHeader", msgCard.getSenderHeader());
            intent.putExtra("mHeader", AppActivity.getUser().getHeader());
            intent.putExtra("receiver", msgCard.getReceiveAccount());
            mContext.startActivity(intent);
        });
        //加载头像 本地没有就从云端加载并保存本地
        String localPath = AppUtil.getImgBasePath(mContext) + File.separator + msgCard.getSenderHeader();
        if (FileUtils.isFileExists(localPath)) {
            holder.msg_item_header.setImageBitmap(BitmapFactory.decodeFile(localPath));
        } else {
            Glide.with(mContext)
                    .setDefaultRequestOptions(new RequestOptions()
                            .centerCrop()
                            .placeholder(R.mipmap.loadingheader)
                            .fitCenter()
                    )
                    .load(IUserNetUtil.picIp + msgCard.getSenderHeader())
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            holder.msg_item_header.setImageDrawable(resource);
                            try {
                                AppUtil.saveFile(ImageUtils.drawable2Bitmap(resource), msgCard.getSenderName());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }
        //设置未读消息
        int unreadNum = msgCard.getUnreadNum();
        if (unreadNum > 0) {
            if (unreadNum > 99) {
                holder.msg_card_content_container.showTextBadge("99+");
            }
            holder.msg_card_content_container.showTextBadge(String.valueOf(msgCard.getUnreadNum()).intern());
        } else {
            holder.msg_card_content_container.hiddenBadge();
        }
        //绑定拖动删除事件
        holder.msg_card_content_container.setDragDismissDelegate(delegate -> {
            if (!nilDBHelper.isOpen()) {
                nilDBHelper.openWriteLink();
            }
            nilDBHelper.clear_unread_by_id(msgCard.getCardId());
            holder.msg_card_content_container.hiddenBadge();
            //更新导航栏消息条数
            mContext.showOrUpdateMsgBadge();
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    static class mHolder extends RecyclerView.ViewHolder {
        TextView msg_item_name, msg_item_time, msg_item_content;
        ShapeableImageView msg_item_header;
        ViewGroup msg_item_container;
        BGABadgeLinearLayout msg_card_content_container;
        public mHolder(@NonNull View itemView) {
            super(itemView);
            msg_item_name = itemView.findViewById(R.id.msg_item_name);
            msg_item_time = itemView.findViewById(R.id.msg_item_time);
            msg_item_content = itemView.findViewById(R.id.msg_item_content);
            msg_item_header = itemView.findViewById(R.id.msg_item_header);
            msg_item_container = itemView.findViewById(R.id.msg_item_container);
            msg_card_content_container = itemView.findViewById(R.id.msg_card_content_container);
        }

    }
}
