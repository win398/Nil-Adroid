package cn.edu.hbpu.nil.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.GsonUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.PersonalActivity;
import cn.edu.hbpu.nil.entity.Contact;
import cn.edu.hbpu.nil.entity.Group;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;

public class GroupedListAdapter extends GroupedRecyclerViewAdapter {
    //groupList用于显示，backup做数据备份来完成一些数据操作
    protected List<Group> groupList, backup;

    public GroupedListAdapter(Context context, List<Group> groupList) {
        super(context);
        this.groupList = groupList;
        //深拷贝
        this.backup = AppUtil.deepCopy(groupList);
    }

    public void updateBackup(List<Group> list) {
        backup = AppUtil.deepCopy(list);
    }

    public List<Group> getBackup() {
        return backup;
    }

    @Override
    public int getGroupCount() {
        return groupList == null ? 0 : groupList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        List<Contact> children = groupList.get(groupPosition).getContactList();
        return children == null ? 0 : children.size();
    }

    public void clear(){
        groupList.clear();
        backup.clear();
        notifyDataChanged();
    }

    public void setGroups(List<Group> groups){
        groupList = groups;
        updateBackup(groupList);
        notifyDataChanged();
    }

    @Override
    public boolean hasHeader(int groupPosition) {
        return true;
    }

    @Override
    public boolean hasFooter(int groupPosition) {
        return false;
    }

    @Override
    public int getHeaderLayout(int viewType) {
        return R.layout.item_contact_group;
    }

    @Override
    public int getFooterLayout(int viewType) {
        return 0;
    }

    @Override
    public int getChildLayout(int viewType) {
        return R.layout.item_contact;
    }

    @Override
    public void onBindHeaderViewHolder(BaseViewHolder holder, int groupPosition) {
        Group group = groupList.get(groupPosition);
        if (isExpand(groupPosition)) {
            holder.setImageResource(R.id.group_extended, R.mipmap.down_one);
        } else {
            holder.setImageResource(R.id.group_extended, R.mipmap.right_one);
        }
        holder.setText(R.id.group_name, group.getGroupName())
                .setText(R.id.group_online_num, group.getOnline() + "")
                .setText(R.id.group_total_num, group.getTotal() + "");
    }

    @Override
    public void onBindFooterViewHolder(BaseViewHolder holder, int groupPosition) {

    }

    private boolean isLocal = true;
    @Override
    public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
        List<Contact> contactList = groupList.get(groupPosition).getContactList();
        Contact contact = contactList.get(childPosition);
        holder.setText(R.id.contact_username, contact.getUserName())
                .setText(R.id.contact_state, contact.getState())
                .setText(R.id.contact_signature, contact.getSignature())
                .get(R.id.contact_index).setVisibility(View.GONE);

        if (!StringUtils.isEmpty(contact.getNameMem())) {
            holder.setText(R.id.contact_username, contact.getNameMem());
        }
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
                        holder.setImageDrawable(R.id.contact_header, resource);
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
        holder.get(R.id.view_contact_item).setOnClickListener((view) -> {
            Intent intent = new Intent(mContext, PersonalActivity.class);
            intent.putExtra("contact", GsonUtils.toJson(contact));
            intent.putExtra("type", PersonalActivity.PERSONAL_CONTACT);
            mContext.startActivity(intent);
        });
    }



    /**
     * 判断当前组是否展开
     *
     * @param groupPosition
     * @return
     */
    public boolean isExpand(int groupPosition) {
        Group group = groupList.get(groupPosition);
        return group.isExpand();
    }


    /**
     * 展开一个组
     *
     * @param groupPosition
     */
    public void expandGroup(int groupPosition) {
        expandGroup(groupPosition, true);
    }

    /**
     * 展开一个组
     *
     * @param groupPosition
     * @param animate
     */
    public void expandGroup(int groupPosition, boolean animate) {
        Group group = groupList.get(groupPosition);
        group.setContactList(AppUtil.deepCopy(backup.get(groupPosition).getContactList()));
        group.setExpand(true);
        if (animate) {
            // 通知一组里的所有子项插入
            notifyChildrenInserted(groupPosition);
        } else {
            notifyDataChanged();
        }
    }

    /**
     * 收起一个组
     *
     * @param groupPosition
     */
    public void collapseGroup(int groupPosition) {
        collapseGroup(groupPosition, false);
    }

    /**
     * 收起一个组
     *
     * @param groupPosition
     * @param animate
     */
    public void collapseGroup(int groupPosition, boolean animate) {
        Group group = groupList.get(groupPosition);
        group.getContactList().clear();
        group.setExpand(false);
        if (animate) {
            notifyChildrenRemoved(groupPosition);
        } else {
            notifyDataChanged();
        }
    }
}
