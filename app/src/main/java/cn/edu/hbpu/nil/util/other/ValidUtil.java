package cn.edu.hbpu.nil.util.other;

import com.blankj.utilcode.util.ToastUtils;

public class ValidUtil {
    //账号最短是5位、最长是10位。第一个数字在1-9之间，第二个数字在0-9之间
    public static final String accountRegex = "[1-9][0-9]{4,9}";
    //密码至少1个小写英文字母 至少1位数字 长度8-16
    public static final String passwordRegex = "^(?=.*?[a-z])(?=.*?[0-9]).{8,16}$";
    //2022手机号正则
    public static final String phoneNumRegex = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$";

    public static boolean checkAccount(String account) {
        return account.matches(accountRegex);
    }

    public static boolean checkPassword(String password) {
        return password.matches(passwordRegex);
    }

    public static boolean checkPhoneNum(String phoneNum) {
        return phoneNum.matches(phoneNumRegex);
    }




}
