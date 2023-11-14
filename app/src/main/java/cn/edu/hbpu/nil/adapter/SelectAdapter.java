package cn.edu.hbpu.nil.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.donkingliang.imageselector.utils.ImageSelector;

import java.util.ArrayList;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.HeaderPreviewActivity;
import cn.edu.hbpu.nil.activity.UpdatePostActivity;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.other.UIUtils;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;

public class SelectAdapter extends RecyclerView.Adapter<SelectAdapter.HeaderHolder> {
    private List<String> images;
    private LayoutInflater inflater;
    private UpdatePostActivity mActivity;

    public static final int COMPLETE_SELECT = 0;

    public SelectAdapter(List<String> images, UpdatePostActivity mActivity) {
        this.images = images;
        this.mActivity = mActivity;
        inflater = LayoutInflater.from(mActivity);
    }

    public List<String> getImages() {
        return images;
    }

    @NonNull
    @Override
    public HeaderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_select_image, parent, false);
        return new HeaderHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderHolder holder, int position) {
        String path = images.get(position);
        //设置图片排版 宽高一致
        int height = (UIUtils.getScreenWidth(mActivity) - ConvertUtils.dp2px(16 * 2 + 2 * 4)) / 4;
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) holder.item_select_container.getLayoutParams();
        layoutParams.height = height;
        if (position % 4 == 0) {
            layoutParams.leftMargin = 0;
        } else if (position % 4 == 3) {
            layoutParams.rightMargin = 0;
        }
        if (path.equals("trigger")) {
            holder.item_select_trigger.setVisibility(View.VISIBLE);
            holder.item_select_image_container.setVisibility(View.GONE);
            holder.item_select_trigger.setOnClickListener(view -> {
                ArrayList<String> selected = (ArrayList<String>) AppUtil.deepCopy(images);
                if (selected != null) {
                    selected.remove(selected.size() - 1);
                }
                //不限数量的多选
                ImageSelector.builder()
                        .useCamera(true) // 设置是否使用拍照
                        .setSingle(false)  //设置是否单选
                        .setMaxSelectCount(100) // 图片的最大选择数量，小于等于0时，不限数量。
                        .setSelected(selected) // 把已选的图片传入默认选中。
                        .canPreview(true) //是否可以预览图片，默认为true
                        .start(mActivity, COMPLETE_SELECT); // 打开相册
            });
        } else {
            holder.item_select_trigger.setVisibility(View.GONE);
            holder.item_select_image_container.setVisibility(View.VISIBLE);
            Glide.with(mActivity)
                    .setDefaultRequestOptions(new RequestOptions()
                            .centerCrop()
                            .placeholder(R.mipmap.loadingheader)
                            .fitCenter()
                    )
                    .load(path)
                    .into(holder.item_select_image);
            holder.item_select_delete.setOnClickListener((view) -> {
                int newPosition = holder.getAdapterPosition();
                images.remove(newPosition);
                notifyItemRemoved(newPosition);
            });
            //点击预览
            holder.item_select_image_container.setOnClickListener((view) -> {
                Intent intent = new Intent(mActivity, HeaderPreviewActivity.class);
                intent.putExtra("type", HeaderPreviewActivity.SELECTED_PIC);
                intent.putExtra("path", path);
                mActivity.startActivity(intent);
            });
        }
    }


    @Override
    public int getItemCount() {
        return images == null ? 0 : images.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        LinearLayout item_select_container, item_select_trigger;
        RelativeLayout item_select_image_container;
        ImageView item_select_image, item_select_delete;
        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            item_select_container = itemView.findViewById(R.id.item_select_container);
            item_select_trigger = itemView.findViewById(R.id.item_select_trigger);
            item_select_image_container = itemView.findViewById(R.id.item_select_image_container);
            item_select_image = itemView.findViewById(R.id.item_select_image);
            item_select_delete = itemView.findViewById(R.id.item_select_delete);
        }
    }
}
