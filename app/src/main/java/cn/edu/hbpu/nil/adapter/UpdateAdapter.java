package cn.edu.hbpu.nil.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ConvertUtils;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.TimeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;
import com.xuexiang.xui.widget.dialog.DialogLoader;
import com.xuexiang.xui.widget.dialog.materialdialog.MaterialDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.activity.AppActivity;
import cn.edu.hbpu.nil.activity.HeaderPreviewActivity;
import cn.edu.hbpu.nil.entity.Comment;
import cn.edu.hbpu.nil.entity.Like;
import cn.edu.hbpu.nil.entity.SocialUpdate;
import cn.edu.hbpu.nil.fragment.MoreFragment;
import cn.edu.hbpu.nil.util.AppUtil;
import cn.edu.hbpu.nil.util.SharedHelper;
import cn.edu.hbpu.nil.util.other.NilApplication;
import cn.edu.hbpu.nil.util.other.TimeUtil;
import cn.edu.hbpu.nil.util.other.UIUtils;
import cn.edu.hbpu.nil.util.web.IUserNetUtil;

public class UpdateAdapter extends RecyclerView.Adapter<UpdateAdapter.ViewHolder> {
    private List<SocialUpdate> updateList;
    private Context mContext;
    private LayoutInflater inflater;
    private SharedHelper sh;
    private  PicAdapter picAdapter;
    private  CommentAdapter commentAdapter;
    private MoreFragment moreFragment;

    public UpdateAdapter(List<SocialUpdate> updateList, Context mContext, MoreFragment fragment) {
        this.updateList = updateList;
        this.mContext = mContext;
        this.moreFragment = fragment;
        inflater = LayoutInflater.from(mContext);
        sh = SharedHelper.getInstance(NilApplication.getContext());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_update, parent, false);
        return new ViewHolder(view);
    }

    private boolean isLocal = true, picIsLocal = true;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SocialUpdate update = updateList.get(position);
        holder.user_update_item_name.setText(update.getUsername());
        holder.user_update_item_time.setText(TimeUtil.dateFormatByNow(TimeUtils.string2Date(update.getSendTime())));
        if (update.getUid() == sh.getUid()) {
            holder.user_update_item_fun.setVisibility(View.VISIBLE);
            holder.user_update_item_fun.setOnClickListener(view -> {
                PopupMenu popupMenu = new PopupMenu(mContext, view);
                popupMenu.inflate(R.menu.menu_update);
                popupMenu.setGravity(Gravity.END);
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    switch (menuItem.getItemId()) {
                        case R.id.update_fun:
                            DialogLoader.getInstance().showConfirmDialog(
                                    mContext, "确认删除？", "确认",
                                    (dialog, which) -> {
                                        moreFragment.deleteSocialUpdate(update.getSid(), holder.getAdapterPosition());
                                        dialog.dismiss();
                                    },
                                    "取消",
                                    (dialog, which) -> {
                                        dialog.dismiss();
                                    }
                            );
                            break;
                    }
                    return true;
                });
            });
        } else {
            holder.user_update_item_fun.setVisibility(View.GONE);
        }
        if (update.getContentText().equals("")) {
            holder.user_update_item_content.setVisibility(View.GONE);
        } else {
            holder.user_update_item_content.setVisibility(View.VISIBLE);
            holder.user_update_item_content.setText(update.getContentText());
        }
        //init rv 禁止滑动
        List<String> pics = update.getPics();
        if (pics == null || pics.isEmpty()) {
            holder.user_update_item_pics.setVisibility(View.GONE);
            holder.user_update_item_pic.setVisibility(View.GONE);
        } else if (pics.size() == 1) {
            //只有一张图片
            String pic = pics.get(0);
            holder.user_update_item_pics.setVisibility(View.GONE);
            holder.user_update_item_pic.setVisibility(View.VISIBLE);
            picIsLocal = true;
            String path = AppUtil.getImgBasePath(mContext) + File.separator + pic;
            if (!FileUtils.isFileExists(path)) {
                path = IUserNetUtil.picIp + pic;
                picIsLocal = false;
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
                            holder.user_update_item_pic.setImageDrawable(resource);
                            Float per = resource.getIntrinsicHeight() / (resource.getIntrinsicWidth() + 0.0f);
                            int desHeight = (int) (per * (UIUtils.getScreenWidth(mContext) - ConvertUtils.dp2px(16 * 2)) + ConvertUtils.dp2px(8 * 2));
                            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.user_update_item_pic.getLayoutParams();
                            layoutParams.height = desHeight;
                            holder.user_update_item_pic.setLayoutParams(layoutParams);

                            if (!picIsLocal) {
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
            holder.user_update_item_pic.setOnClickListener((view) -> {
                //点击预览
                Intent intent = new Intent(mContext, HeaderPreviewActivity.class);
                intent.putExtra("type", HeaderPreviewActivity.UPDATE_PIC);
                intent.putExtra("path", AppUtil.getImgBasePath(mContext) + File.separator + pic);
                mContext.startActivity(intent);
            });
        } else {
            holder.user_update_item_pics.setVisibility(View.VISIBLE);
            holder.user_update_item_pic.setVisibility(View.GONE);
            GridLayoutManager layoutManager = new GridLayoutManager(mContext, 3) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
            };
            holder.user_update_item_pics.setLayoutManager(layoutManager);
            picAdapter = new PicAdapter(pics, mContext);
            holder.user_update_item_pics.setAdapter(picAdapter);
        }


        List<Comment> comments = update.getComments();
        if (comments == null || comments.isEmpty()) {
            holder.user_update_item_comments.setVisibility(View.GONE);
        } else {
            holder.user_update_item_comments.setVisibility(View.VISIBLE);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
            };
            holder.user_update_item_comments.setLayoutManager(linearLayoutManager);
            commentAdapter = new CommentAdapter(comments, mContext);
            holder.user_update_item_comments.setAdapter(commentAdapter);
        }

        //点赞人员
        List<Like> likes = update.getLikes();
        if (likes == null || likes.isEmpty()) {
            holder.user_update_item_likes.setVisibility(View.GONE);
        } else {
            holder.user_update_item_likes.setVisibility(View.VISIBLE);
            StringBuilder likeString = new StringBuilder();
            likeString.append(mContext.getResources().getString(R.string.heart));
            int size = likes.size();
            for (int i = 0; i < size - 1; i++) {
                likeString.append(likes.get(i).getUsername()).append("、");
            }
            likeString.append(likes.get(size - 1).getUsername());
            holder.user_update_item_likes.setText(likeString);
        }

        //加载头像 第一次加载保存到本地
        String path = AppUtil.getImgBasePath(mContext) + File.separator + update.getHeader();
        isLocal = true;
        if (!FileUtils.isFileExists(path)) {
            path = IUserNetUtil.picIp + update.getHeader();
            isLocal = false;
        }
        Glide.with(mContext)
                .load(path)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        holder.user_update_item_header.setImageDrawable(resource);
                        if (!isLocal) {
                            try {
                                AppUtil.saveFile(ImageUtils.drawable2Bitmap(resource), update.getHeader());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });

        //评论
        holder.user_update_item_comment.setOnClickListener((view -> {
            new MaterialDialog.Builder(mContext)
                    .title("评论动态")
                    .inputType(InputType.TYPE_CLASS_TEXT)
                    .input(
                            "发表你的评论",
                            "",
                            true,
                            ((dialog, input) -> LogUtils.d("评论动态:" + input.toString() + " sid:" + update.getSid())))
                    .inputRange(0, 1000)
                    .positiveText("提交")
                    .negativeText("取消")
                    .onPositive(((dialog, which) -> {
                        if (dialog.getInputEditText() == null || dialog.getInputEditText().getText().toString().trim().equals("")) {
                            ToastUtils.showShort("评论不能为空");
                        }
                        moreFragment.submitComment(update.getSid(), holder.getAdapterPosition(), dialog.getInputEditText().getText().toString());
                    }))
                    .cancelable(true)
                    .show();
        }));

        //点赞显示
        if (update.isLike()) {
            holder.user_update_item_like.setVisibility(View.GONE);
            holder.user_update_item_liked.setVisibility(View.VISIBLE);
            //取消点赞事件
            holder.user_update_item_liked.setOnClickListener((view -> {
                //避免快速点击
                if (AppUtil.isFastClick()) {
                    ToastUtils.showShort("操作太快");
                    return;
                }
                moreFragment.likeUpdate(sh.getUid(), update.getSid(), holder.getAdapterPosition(), true);
            }));
        } else {
            holder.user_update_item_like.setVisibility(View.VISIBLE);
            holder.user_update_item_liked.setVisibility(View.GONE);
            //点赞事件
            holder.user_update_item_like.setOnClickListener((view -> {
                //避免快速点击
                if (AppUtil.isFastClick()) {
                    ToastUtils.showShort("操作太快");
                }
                moreFragment.likeUpdate(sh.getUid(), update.getSid(), holder.getAdapterPosition(), false);
            }));
        }
    }

    @Override
    public int getItemCount() {
        return updateList == null ? 0 : updateList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView user_update_item_name, user_update_item_time, user_update_item_content, user_update_item_likes;
        ShapeableImageView user_update_item_header, user_update_item_fun, user_update_item_comment, user_update_item_liked, user_update_item_like, user_update_item_pic;
        RecyclerView user_update_item_pics, user_update_item_comments;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            user_update_item_name = itemView.findViewById(R.id.user_update_item_name);
            user_update_item_time = itemView.findViewById(R.id.user_update_item_time);
            user_update_item_content = itemView.findViewById(R.id.user_update_item_content);
            user_update_item_header = itemView.findViewById(R.id.user_update_item_header);
            user_update_item_fun = itemView.findViewById(R.id.user_update_item_fun);
            user_update_item_comment = itemView.findViewById(R.id.user_update_item_comment);
            user_update_item_liked = itemView.findViewById(R.id.user_update_item_liked);
            user_update_item_like = itemView.findViewById(R.id.user_update_item_like);
            user_update_item_pics = itemView.findViewById(R.id.user_update_item_pics);
            user_update_item_comments = itemView.findViewById(R.id.user_update_item_comments);
            user_update_item_likes = itemView.findViewById(R.id.user_update_item_likes);
            user_update_item_pic = itemView.findViewById(R.id.user_update_item_pic);
        }
    }
}
