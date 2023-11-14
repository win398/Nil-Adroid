package cn.edu.hbpu.nil.entity;

import java.io.Serializable;
import java.time.LocalDate;

import lombok.Data;

@Data
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private transient String checkCode;
    private String nameMem;
    /**
     * 用户ID
     */
    private Integer uid;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户账号
     */
    private String userNum;

    /**
     * 用户密码
     */
    private String password;


    /**
     * 头像
     */
    private String header;

    /**
     * 电话号码
     */
    private String phoneNum;


    //用户其他信息
    /**
     * 性别
     */
    private String sex;

    /**
     * 出生日期
     */
    private String birth;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 签名
     */
    private String signature;

    /**
     * 主页背景
     */
    private String bgImg;


    private boolean isFriend;

    public String getCheckCode() {
        return checkCode;
    }

    public void setCheckCode(String checkCode) {
        this.checkCode = checkCode;
    }

    public String getNameMem() {
        return nameMem;
    }

    public void setNameMem(String nameMem) {
        this.nameMem = nameMem;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getBirth() {
        return birth;
    }

    public void setBirth(String birth) {
        this.birth = birth;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getBgImg() {
        return bgImg;
    }

    public void setBgImg(String bgImg) {
        this.bgImg = bgImg;
    }

    public boolean isFriend() {
        return isFriend;
    }

    public void setFriend(boolean friend) {
        isFriend = friend;
    }


}

