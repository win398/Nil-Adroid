package cn.edu.hbpu.nil.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.PersonalActivity;
import cn.edu.hbpu.nil.entity.Contact;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {
    private List<Contact> contactList;
    private Context mContext;
    private LayoutInflater inflater;

    public ContactAdapter(List<Contact> contactList, Context mContext) {
        this.contactList = contactList;
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
    }

    public void setContactList(List<Contact> contactList) {
        this.contactList = contactList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(view);
    }

    private boolean isLocal = true;
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Contact contact = contactList.get(position);
        if (contact.getNameMem() != null && !contact.getNameMem().equals("")) {
            holder.contact_username.setText(contact.getNameMem());
        } else {
            holder.contact_username.setText(contact.getUserName());
        }
        holder.contact_state.setText(contact.getState());
        holder.contact_signature.setText(contact.getSignature());
        if (position == getFirstPosition(contact.getStartChar())) {
            holder.contact_index.setVisibility(View.VISIBLE);
            holder.contact_index.setText(contact.getStartChar());
        } else {
            holder.contact_index.setVisibility(View.GONE);
        }

        //加载头像
        //加载头像 第一次加载保存到本地
        String path = AppUtil.getImgBasePath(mContext) + File.separator + contact.getHeader();
        if (!FileUtils.isFileExists(path)) {
            path = IUserNetUtil.picIp + contact.getHeader();
            isLocal = false;
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
                        holder.contact_header.setImageDrawable(resource);
                        if (!isLocal) {
                            try {
                                AppUtil.saveFile(ImageUtils.drawable2Bitmap(resource), contact.getHeader());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

        //点击联系人卡片进入联系人主页
        holder.view_contact_item.setOnClickListener((view) -> {
            Intent intent = new Intent(mContext, PersonalActivity.class);
            intent.putExtra("contact", GsonUtils.toJson(contact));
            intent.putExtra("type", PersonalActivity.PERSONAL_CONTACT);
            mContext.startActivity(intent);
        });
    }

    private int getFirstPosition(String index) {
        for (int i = 0; i < contactList.size(); i++) {
            if (contactList.get(i).getStartChar().equalsIgnoreCase(index)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return contactList == null ? 0 : contactList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView contact_index, contact_state, contact_username, contact_signature;
        ShapeableImageView contact_header;
        RelativeLayout view_contact_item;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            contact_index = itemView.findViewById(R.id.contact_index);
            contact_header = itemView.findViewById(R.id.contact_header);
            contact_state = itemView.findViewById(R.id.contact_state);
            contact_username = itemView.findViewById(R.id.contact_username);
            contact_signature = itemView.findViewById(R.id.contact_signature);
            view_contact_item = itemView.findViewById(R.id.view_contact_item);
        }
    }
}
