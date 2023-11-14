package cn.edu.hbpu.nil.entity;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;
    private int groupId;
    private String groupName;
    private int online;
    private int total;
    private transient boolean isExpand;
    private List<Contact> contactList;
    private int groupIndex;

    public Group() {

    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getOnline() {
        return online;
    }

    public void setOnline(int online) {
        this.online = online;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public boolean isExpand() {
        return isExpand;
    }

    public void setExpand(boolean expand) {
        isExpand = expand;
    }

    public List<Contact> getContactList() {
        return contactList;
    }

    public void setContactList(List<Contact> contactList) {
        this.contactList = contactList;
    }

    public int getGroupIndex() {
        return groupIndex;
    }

    public void setGroupIndex(int groupIndex) {
        this.groupIndex = groupIndex;
    }



    public Group(int groupId, String groupName, int online, int total, List<Contact> contactList, boolean isExpand, int groupIndex) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.online = online;
        this.total = total;
        this.contactList = contactList;
        this.isExpand = isExpand;
        this.groupIndex= groupIndex;
    }
}
