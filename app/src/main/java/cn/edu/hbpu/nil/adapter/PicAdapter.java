package cn.edu.hbpu.nil.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.HeaderPageActivity;
import cn.edu.hbpu.nil.activity.HeaderPreviewActivity;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.other.UIUtils;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;

public class PicAdapter extends RecyclerView.Adapter<PicAdapter.HeaderHolder> {
    private List<String> pics;
    private LayoutInflater inflater;
    private Context mContext;

    public PicAdapter(List<String> pics, Context mContext) {
        this.pics = pics;
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
    }

    @NonNull
    @Override
    public HeaderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_pic, parent, false);
        return new HeaderHolder(view);
    }

    private boolean isLocal;
    @Override
    public void onBindViewHolder(@NonNull HeaderHolder holder, int position) {
        String pic = pics.get(position);
        //设置图片排版 宽高一致
        int height = (UIUtils.getScreenWidth(mContext) - ConvertUtils.dp2px(16 * 2 + 2 * 4)) / 3;
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) holder.item_pic_container.getLayoutParams();
        layoutParams.height = height;
        if (position % 3 == 0) {
            layoutParams.leftMargin = 0;
        } else if (position % 3 == 2) {
            layoutParams.rightMargin = 0;
        }
        holder.item_pic_container.setLayoutParams(layoutParams);
        isLocal = true;
        String path = AppUtil.getImgBasePath(mContext) + File.separator + pic;
        if (!FileUtils.isFileExists(path)) {
            path = IUserNetUtil.picIp + pic;
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
                        holder.item_pic.setImageDrawable(resource);
                        if (!isLocal) {
                            try {
                                AppUtil.saveFile(ImageUtils.drawable2Bitmap(resource), pic);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
        holder.item_pic_container.setOnClickListener((view) -> {
            //点击预览
            Intent intent = new Intent(mContext, HeaderPreviewActivity.class);
            intent.putExtra("type", HeaderPreviewActivity.UPDATE_PIC);
            intent.putExtra("path", AppUtil.getImgBasePath(mContext) + File.separator + pic);
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return pics == null ? 0 : pics.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        LinearLayout item_pic_container;
        ImageView item_pic;
        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            item_pic_container = itemView.findViewById(R.id.item_pic_container);
            item_pic = itemView.findViewById(R.id.item_pic);
        }
    }
}
