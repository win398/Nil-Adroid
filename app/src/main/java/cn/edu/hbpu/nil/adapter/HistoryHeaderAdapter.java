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
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.HeaderPageActivity;
import cn.edu.hbpu.nil.activity.HeaderPreviewActivity;
import cn.edu.hbpu.nil.util.other.UIUtils;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;

public class HistoryHeaderAdapter extends RecyclerView.Adapter<HistoryHeaderAdapter.HeaderHolder> {
    private List<String> headers;
    private LayoutInflater inflater;
    private Context mContext;

    public HistoryHeaderAdapter(List<String> headers, Context mContext) {
        this.headers = headers;
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    @NonNull
    @Override
    public HeaderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_header, parent, false);
        return new HeaderHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderHolder holder, int position) {
        String path = IUserNetUtil.picIp + headers.get(position);
        //设置图片排版 宽高一致
        int height = (UIUtils.getScreenWidth(mContext) - ConvertUtils.dp2px(4 * 2 + 2 * 2)) / 4;
        GridLayoutManager.LayoutParams layoutParams = (GridLayoutManager.LayoutParams) holder.header_item_container.getLayoutParams();
        layoutParams.height = height;
        if (position % 4 == 0) {
            layoutParams.leftMargin = 0;
        } else if (position % 4 == 3) {
            layoutParams.rightMargin = 0;
        }
        holder.header_item_container.setLayoutParams(layoutParams);
        Glide.with(mContext)
                .setDefaultRequestOptions(new RequestOptions()
                        .centerCrop()
                        .placeholder(R.mipmap.loadingheader)
                        .fitCenter()
                )
                .load(path)
                .into(holder.header_item);
        holder.header_item_container.setOnClickListener((view) -> {
            //点击预览
            Intent intent = new Intent(mContext, HeaderPreviewActivity.class);
            intent.putExtra("header", headers.get(position));
            intent.putExtra("type", HeaderPreviewActivity.HISTORY_HEADER);
            intent.putExtra("uid", ((HeaderPageActivity) mContext).uid);
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return headers == null ? 0 : headers.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        LinearLayout header_item_container;
        ImageView header_item;
        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            header_item_container = itemView.findViewById(R.id.header_item_container);
            header_item = itemView.findViewById(R.id.header_item);
        }
    }
}
