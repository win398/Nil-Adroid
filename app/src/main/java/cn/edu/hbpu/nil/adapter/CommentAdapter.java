package cn.edu.hbpu.nil.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ConvertUtils;

import java.util.List;

import cn.edu.hbpu.nil.R;
import cn.edu.hbpu.nil.entity.Comment;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.HeaderHolder> {
    private List<Comment> comments;
    private LayoutInflater inflater;
    private Context mContext;

    public CommentAdapter(List<Comment> comments, Context mContext) {
        this.comments = comments;
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
    }

    @NonNull
    @Override
    public HeaderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_comment, parent, false);
        return new HeaderHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderHolder holder, int position) {
        Comment comment = comments.get(position);
        String username = comment.getUsername();
        String commentStr = username + ":" + comment.getContentText();
        SpannableString spanStr = new SpannableString(commentStr);
        TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(
                null,  // textFamily 字体
                Typeface.BOLD,  // textStyle
                ConvertUtils.sp2px(14),  // textSizeInPixel
                ColorStateList.valueOf(mContext.getResources().getColor(R.color.black_light)),  // textColor
                null); // underlinedTextColor
        spanStr.setSpan(textAppearanceSpan, 0, username.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.item_comment_content.setText(spanStr);
    }

    @Override
    public int getItemCount() {
        return comments == null ? 0 : comments.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        TextView item_comment_content;
        public HeaderHolder(@NonNull View itemView) {
            super(itemView);
            item_comment_content = itemView.findViewById(R.id.item_comment_content);
        }
    }
}
