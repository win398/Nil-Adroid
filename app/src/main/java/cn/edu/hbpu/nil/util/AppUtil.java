package cn.edu.hbpu.nil.util;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.ToastUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.edu.hbpu.nil.util.other.NilApplication;

public class AppUtil {
    //public static final String basePath = Environment.getExternalStorageDirectory() + File.separator + "nil";
    //public static final String imgBasePath =  Environment.getExternalStorageDirectory() + File.separator + "nil" + File.separator + "img";


    private static final String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    //相机权限
    private static final String[] permissionCamera = new String[]{
            Manifest.permission.CAMERA,
    };

    //应用存储文件夹根目录
    public static String getBasePath(Context context) {
        String basePath;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);// 判断sd卡是否存在
        if (sdCardExist) {
            if (Build.VERSION.SDK_INT>=29){
                //Android10之后
                basePath = context.getExternalFilesDir(null).getAbsoluteFile() + File.separator + "nil";
            }else {
                basePath = Environment.getExternalStorageDirectory() + File.separator + "nil";// 获取SD卡根目录
            }
        } else {
            basePath = Environment.getRootDirectory() + File.separator + "nil";// 获取跟目录
        }
        return basePath;
    }
    //应用图片存储文件夹路径
    public static String getImgBasePath(Context context) {
        return getBasePath(context) + File.separator + "img";
    }

    //相册存储路径
    public static String getAlbumBasePath(Context context) {
        return getBasePath(context) + File.separator + "NIL" + File.separator;
    }

    //判断是否有使用相机权限
    public static boolean isGrantedCameraPermissions(Activity activity){
        return ContextCompat.checkSelfPermission(activity, permissionCamera[0]) == PackageManager.PERMISSION_GRANTED;
    }
    //检查相机权限
    public static void checkPermissionCamera(Activity activity){
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 权限是否已经 授权 GRANTED---授权  DENIED---拒绝
            // 如果没有授予该权限，就去提示用户请求
            if (ContextCompat.checkSelfPermission(activity, permissionCamera[0]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, permissionCamera, 322);
            }
        }
    }

    //判断是否有读写内存权限
    public static boolean isGrantedWRPermissions(Activity activity){
            int i = ContextCompat.checkSelfPermission(activity, permissions[0]);
            int j = ContextCompat.checkSelfPermission(activity, permissions[1]);
            return i == PackageManager.PERMISSION_GRANTED && j == PackageManager.PERMISSION_GRANTED;
    }
    //检查写读内存权限
    public static void checkPermissions(Activity activity){
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(activity, permissions[0]);
            int j = ContextCompat.checkSelfPermission(activity, permissions[1]);
            // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
            // 如果没有授予该权限，就去提示用户请求
            if (i != PackageManager.PERMISSION_GRANTED || j != PackageManager.PERMISSION_GRANTED) {
                startRequestPermission(activity);
            }
        }
    }
    private static void startRequestPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, permissions, 321);
    }

    //深拷贝
    public static <T> List<T> deepCopy(List<T> src) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(src);

            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            ObjectInputStream in = new ObjectInputStream(byteIn);
            @SuppressWarnings("unchecked")
            List<T> dest = (List<T>) in.readObject();
            return dest;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //List转换
    public static <T> List<T> castList(Object obj, Class<T> clazz)
    {
        List<T> result = new ArrayList<T>();
        if(obj instanceof List<?>)
        {
            for (Object o : (List<?>) obj)
            {
                result.add(clazz.cast(o));
            }
            return result;
        }
        return null;
    }

    //保存图片到SD卡
    public static void saveFile(Bitmap bm, String fileName) throws IOException {
        if (PermissionUtils.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            File base = new File(AppUtil.getImgBasePath(NilApplication.getContext()));
            if (!base.exists()) {
                boolean isMake = base.mkdirs();
                Log.e("----made?-----", String.valueOf(isMake));
            }
            File file = new File(base + File.separator + fileName);
            //Log.d("-----头像存在------", String.valueOf(FileUtils.createOrExistsFile(file)));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } else {
            //申请权限并设置回调
            PermissionUtils.permission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .callback(new PermissionUtils.SimpleCallback() {
                        @Override
                        public void onGranted() {
                            try {
                                saveFile(bm, fileName);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onDenied() {
                            //提示
                            ToastUtils.showShort("保存失败，请授予权限");
                        }
                    })
                    .request();
        }

    }

    /**
     * 保存到相册
     *
     * @param src  源图片
     * @param fileName 要保存到的文件
     */
    public static void savePhotoAlbum(Bitmap src, String fileName, Context context) {
        if (PermissionUtils.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            if (src == null) {
                return;
            }
            File base = new File(getAlbumBasePath(context));
            if (!base.exists()) {
                boolean isMake = base.mkdirs();
                Log.e("----made?-----", String.valueOf(isMake));
            }
            File file = new File(base + fileName);
            LogUtils.d(file);
            //先保存到文件
            //先保存到文件
            OutputStream outputStream;
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(file));
                src.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                if (!src.isRecycled()) {
                    src.recycle();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            //再更新图库
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
                values.put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(file));
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);
                ContentResolver contentResolver = context.getContentResolver();
                Uri uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,  values);
                if (uri == null) {
                    return;
                }
                try {
                    outputStream = contentResolver.openOutputStream(uri);
                    FileInputStream fileInputStream = new FileInputStream(file);
                    FileUtils.copy(fileInputStream, outputStream);
                    fileInputStream.close();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                MediaScannerConnection.scanFile(
                        NilApplication.getContext(),
                        new String[]{file.getAbsolutePath()},
                        new String[]{"image/jpeg"},
                        (path, uri) -> {
                            // Scan Completed
                        });
            }
        } else {
            //申请权限并设置回调
            PermissionUtils.permission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .callback(new PermissionUtils.SimpleCallback() {
                        @Override
                        public void onGranted() {
                            try {
                                saveFile(src, fileName);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onDenied() {
                            //提示
                            ToastUtils.showShort("保存失败，请授予权限");
                        }
                    })
                    .request();
        }
    }

    private static String getSuffix(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return null;
        }
        String fileName = file.getName();
        if (fileName.equals("") || fileName.endsWith(".")) {
            return null;
        }
        int index = fileName.lastIndexOf(".");
        if (index != -1) {
            return fileName.substring(index + 1).toLowerCase(Locale.US);
        } else {
            return null;
        }
    }

    private static String getMimeType(File file){
        String suffix = getSuffix(file);
        if (suffix == null) {
            return "file/*";
        }
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        if (type != null && !type.isEmpty()) {
            return type;
        }
        return "file/*";
    }

    /**
     * 播放通知声音
     */
    public static void playNotificationRing(Context context) {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone rt = RingtoneManager.getRingtone(context, uri);
        rt.play();
    }

    // 两次点击按钮之间的点击间隔不能少于800毫秒
    public static int MIN_CLICK_DELAY_TIME = 800;
    private static long lastClickTime;

    public synchronized static boolean isFastClick() {
        boolean flag = true;
        long curClickTime = System.currentTimeMillis();
        if ((curClickTime - lastClickTime) >= MIN_CLICK_DELAY_TIME) {
            flag = false;
        }
        lastClickTime = curClickTime;
        return flag;
    }
}
