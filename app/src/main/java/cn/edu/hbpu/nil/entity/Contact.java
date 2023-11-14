package cn.edu.hbpu.nil.entity;

import java.io.Serializable;

import cn.edu.hbpu.nil.util.other.Cn2Spell;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Contact implements Serializable, Comparable<Contact> {
    private static final long serialVersionUID = 1L;
    //联系人状态
    private String state;
    //添加时间
    private String createdTime;
    //分组
    private int groupIndex;
    //特别关心
    private boolean isFavor;
    //备注
    private String nameMem;

    //用户信息，可通过Json转User
    //联系人昵称
    private String userName;
    //头像
    private String header;
    //个性签名
    private String signature;
    //好友账号
    private String userNum;
    //用户其他信息
    private String sex;
    private String birth;
    private String province;
    private String city;
    private String bgImg;

    //首字母，中文为拼音首字母
    private transient String startChar;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public int getGroupIndex() {
        return groupIndex;
    }

    public void setGroupIndex(int groupIndex) {
        this.groupIndex = groupIndex;
    }

    public boolean isFavor() {
        return isFavor;
    }

    public void setFavor(boolean favor) {
        isFavor = favor;
    }

    public String getNameMem() {
        return nameMem;
    }

    public void setNameMem(String nameMem) {
        this.nameMem = nameMem;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getUserNum() {
        return userNum;
    }

    public void setUserNum(String userNum) {
        this.userNum = userNum;
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

    public String getBgImg() {
        return bgImg;
    }

    public void setBgImg(String bgImg) {
        this.bgImg = bgImg;
    }

    public String getStartChar() {
        return startChar;
    }

    public void setStartChar(String startChar) {
        this.startChar = startChar;
    }


    @Override
    public int compareTo(Contact contact) {
        if (!startChar.equals("#") && !contact.getStartChar().equals("#")) {
            return startChar.charAt(0) - contact.getStartChar().charAt(0);
        } else if (startChar.equals("#") && !contact.getStartChar().equals("#")) {
            return 1;
        } else {
            return -1;
        }
    }
}
