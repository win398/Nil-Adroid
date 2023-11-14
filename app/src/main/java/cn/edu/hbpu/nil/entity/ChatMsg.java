package cn.edu.hbpu.nil.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor//全参构造
@NoArgsConstructor//无参构造
@Data//get set
public class ChatMsg implements Serializable {
    private static final long serialVersionUID = 1L;
    //消息编号
    private int msgId;
    private String sendAccount;
    //接受方信息
    private String receiveAccount;
    //最后一条消息时间
    private String sendTime;
    //内容
    private String msgContent;


    private String senderName;
    private String senderHeader;



    public int getMsgId() {
        return msgId;
    }

    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }

    public String getSendAccount() {
        return sendAccount;
    }

    public void setSendAccount(String sendAccount) {
        this.sendAccount = sendAccount;
    }

    public String getReceiveAccount() {
        return receiveAccount;
    }

    public void setReceiveAccount(String receiveAccount) {
        this.receiveAccount = receiveAccount;
    }

    public String getSendTime() {
        return sendTime;
    }

    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
    }

    public String getMsgContent() {
        return msgContent;
    }

    public void setMsgContent(String msgContent) {
        this.msgContent = msgContent;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderHeader() {
        return senderHeader;
    }

    public void setSenderHeader(String senderHeader) {
        this.senderHeader = senderHeader;
    }

    public String getNameMem() {
        return nameMem;
    }

    public void setNameMem(String nameMem) {
        this.nameMem = nameMem;
    }

    private String nameMem;

}
