package cn.edu.hbpu.nil.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

import cn.edu.hbpu.nil.entity.User;

public class SharedHelper {
    private Context mContext;
    private static SharedHelper sharedHelper = null;

    public SharedHelper() {
    }

    public static SharedHelper getInstance(Context context) {
        if (sharedHelper == null) {
            sharedHelper = new SharedHelper(context);
        }
        return sharedHelper;
    }

    public SharedHelper(Context mContext) {
        this.mContext = mContext;
    }

    public void save(String account, String password) {
        SharedPreferences userRecord = mContext.getSharedPreferences("userRecord", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = userRecord.edit();
        editor.putString("account", account);
        editor.putString("password", password);
        editor.apply();
    }

    public String getAccount() {
        SharedPreferences userRecord = mContext.getSharedPreferences("userRecord", Context.MODE_PRIVATE);
        return userRecord.getString("account", "");
    }

    public Map<String, String> read() {
        HashMap<String, String> map = new HashMap<>();
        SharedPreferences userRecord = mContext.getSharedPreferences("userRecord", Context.MODE_PRIVATE);
        map.put("account", userRecord.getString("account", ""));
        map.put("password", userRecord.getString("password", ""));
        return map;
    }

    public void setToken(String token) {
        SharedPreferences sp = mContext.getSharedPreferences("userRecord", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("token", token);
        editor.apply();
    }

    public String getToken() {
        SharedPreferences sp = mContext.getSharedPreferences("userRecord", Context.MODE_PRIVATE);
        return sp.getString("token", "");
    }

    public int getUid() {
        SharedPreferences sp = mContext.getSharedPreferences("userRecord", Context.MODE_PRIVATE);
        return sp.getInt("uid", -1);
    }

    public void setUid(int uid) {
        SharedPreferences sp = mContext.getSharedPreferences("userRecord", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("uid", uid);
        editor.apply();
    }

    public String getUser() {
        SharedPreferences sp = mContext.getSharedPreferences("userRecord", Context.MODE_PRIVATE);
        return sp.getString("user", "");
    }

    public void setUser(String jsonUser) {
        SharedPreferences sp = mContext.getSharedPreferences("userRecord", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("user", jsonUser);
        editor.apply();
    }



    public void removeAll() {
        SharedPreferences userRecord = mContext.getSharedPreferences("userRecord", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = userRecord.edit();
        editor.remove("token");
        editor.remove("user");
        editor.remove("uid");
        editor.apply();
    }
}
