package cn.edu.hbpu.nil.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * 
 * </p>
 *
 * @author hbpu
 * @since 2022-07-14
 */
@Getter
@Setter
@ToString
public class FriendVerification implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 验证信息编号
     */
    private Integer verificationId;

    /**
     * 发送用户编号
     */
    private Integer fromUid;

    /**
     * 接受用户编号
     */
    private Integer toUid;

    /**
     * 验证消息
     */
    private String content;
    /**
     * 状态
     */
    private Integer verifyState;

    private String sendTime;

    //备注信息
    private String nameMem;
    //分组下标
    private Integer groupIndex;

    private String userName;
    private String header;
    private String toUserName;
    private String toUserHeader;
    private Integer hasDeleted;

    public FriendVerification() {

    }


    public Integer getVerificationId() {
        return verificationId;
    }

    public void setVerificationId(Integer verificationId) {
        this.verificationId = verificationId;
    }

    public Integer getFromUid() {
        return fromUid;
    }

    public void setFromUid(Integer fromUid) {
        this.fromUid = fromUid;
    }

    public Integer getToUid() {
        return toUid;
    }

    public void setToUid(Integer toUid) {
        this.toUid = toUid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getVerifyState() {
        return verifyState;
    }

    public void setVerifyState(Integer verifyState) {
        this.verifyState = verifyState;
    }

    public String getSendTime() {
        return sendTime;
    }

    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
    }

    public String getNameMem() {
        return nameMem;
    }

    public void setNameMem(String nameMem) {
        this.nameMem = nameMem;
    }

    public Integer getGroupIndex() {
        return groupIndex;
    }

    public void setGroupIndex(Integer groupIndex) {
        this.groupIndex = groupIndex;
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

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getToUserHeader() {
        return toUserHeader;
    }

    public void setToUserHeader(String toUserHeader) {
        this.toUserHeader = toUserHeader;
    }

    public Integer getHasDeleted() {
        return hasDeleted;
    }

    public void setHasDeleted(Integer hasDeleted) {
        this.hasDeleted = hasDeleted;
    }

    public FriendVerification(Integer fromUid, Integer toUid, String content, String nameMem, Integer groupIndex) {
        this.fromUid = fromUid;
        this.toUid = toUid;
        this.content = content;
        this.nameMem = nameMem;
        this.groupIndex = groupIndex;
    }


}
