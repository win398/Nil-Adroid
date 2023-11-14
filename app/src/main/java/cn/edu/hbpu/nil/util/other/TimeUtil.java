package cn.edu.hbpu.nil.util.other;

import android.annotation.SuppressLint;

import com.blankj.utilcode.util.TimeUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
    /**
     * 获得当天零时零分零秒
     * @return
     */
    private static Calendar initDateByDay(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar;
    }

    //判断时间为上午，中午，下午，晚上，凌晨
    private static String getPeriod(Date date) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("HH");
        int a = Integer.parseInt(df.format(date));
        if (a >= 0 && a <= 6) {
            return "凌晨";
        }
        if (a > 6 && a <= 12) {
            return "上午";
        }
        if (a == 13) {
            return "中午";
        }
        if (a > 13 && a <= 18) {
            return "下午";
        }
        return "晚上";
    }

    public static String dateFormatByNow(Date date) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        String nowTime = getPeriod(date) + df.format(date);
        if (TimeUtils.isToday(date)) {
            return nowTime;
        }
        Calendar cal = initDateByDay(new Date());
        cal.add(Calendar.DATE, -1);
        if (date.after(cal.getTime())) {
            return "昨天 " + nowTime;
        }

        cal.add(Calendar.DATE, -1);
        if (date.after(cal.getTime())) {
            return "前天 " + nowTime;
        }

        cal.add(Calendar.DATE, -1);
        if (date.after(cal.getTime())) {
            return "三天前 " + nowTime;
        }
        //七天内显示星期
        cal.add(Calendar.DATE, -3);
        if (date.after(cal.getTime())) {
            return TimeUtils.getChineseWeek(date) + " " + nowTime;
        }
        //判断是否今年
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dfYear = new SimpleDateFormat("yyyy");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dfFinal = new SimpleDateFormat("MM-dd HH:mm");
        if (dfYear.format(date).equals(dfYear.format(new Date()))) {
            return dfFinal.format(date);
        }
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dfFinal1 = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        return dfFinal1.format(date);
    }

    public static String dateFormatByNowSimple(Date date) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        String nowTime = getPeriod(date) + df.format(date);
        if (TimeUtils.isToday(date)) {
            return nowTime;
        }
        Calendar cal = initDateByDay(new Date());
        cal.add(Calendar.DATE, -1);
        if (date.after(cal.getTime())) {
            return "昨天";
        }

        cal.add(Calendar.DATE, -1);
        if (date.after(cal.getTime())) {
            return "前天";
        }

        cal.add(Calendar.DATE, -1);
        if (date.after(cal.getTime())) {
            return "三天前";
        }
        //七天内显示星期
        cal.add(Calendar.DATE, -3);
        if (date.after(cal.getTime())) {
            return TimeUtils.getChineseWeek(date);
        }
        //判断是否今年
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dfYear = new SimpleDateFormat("yyyy");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dfFinal = new SimpleDateFormat("MM-dd");
        if (dfYear.format(date).equals(dfYear.format(new Date()))) {
            return dfFinal.format(date);
        }
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dfFinal1 = new SimpleDateFormat("yyyy-MM-dd");
        return dfFinal1.format(date);
    }

    //根据生日计算年龄
    public static int getAge(Date birth) {
        Calendar cal = Calendar.getInstance();
        int thisYear = cal.get(Calendar.YEAR);
        int thisMonth = cal.get(Calendar.MONTH);
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        cal.setTime(birth);
        int birthYear = cal.get(Calendar.YEAR);
        int birthMonth = cal.get(Calendar.MONTH);
        int birthdayOfMonth = cal.get(Calendar.DAY_OF_MONTH);

        int age = thisYear - birthYear;

        // 未足月
        if (thisMonth <= birthMonth) {
            // 当月
            if (thisMonth == birthMonth) {
                // 未足日
                if (dayOfMonth < birthdayOfMonth) {
                    age--;
                }
            } else {
                age--;
            }
        }
        return age;
    }

    public static String getTimeNow() {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        return formatter.format(curDate);
    }
}
