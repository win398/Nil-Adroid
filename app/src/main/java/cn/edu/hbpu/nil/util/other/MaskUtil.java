package cn.edu.hbpu.nil.util.other;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

public class MaskUtil {
    /**
            * 遮罩层
     * @param message 遮罩层的文字显示
     * @param mContext 使用的activity
     */
    public static ProgressDialog showProgressDialog(String message, Context mContext) {
        ProgressDialog mProgressDialog = null;
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(mContext, ProgressDialog.THEME_HOLO_LIGHT);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.setMessage(message);
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
        return mProgressDialog;
    }
}
