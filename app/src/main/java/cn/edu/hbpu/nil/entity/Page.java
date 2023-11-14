package cn.edu.hbpu.nil.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Page {
    private int pageNo;
    private int pages;
    private List<SocialUpdate> records;
    //{"records":[{"pics":[],"likes":[],"comments":[],"sid":8,"contentText":"23232232","uid":18,"sendTime":"2022-08-17 09:32:12"},{"pics":["e96c5373f8724fbc8e831e9ac55add16.jpg","f9c103a76810468ab428a9c339c1326d.jpg","a2104bfb5d98428fbda18e595f693b19.jpg"],"likes":[],"comments":[],"sid":7,"contentText":"","uid":18,"sendTime":"2022-08-17 09:32:04"},{"pics":["3301a2c7d720487f942f93b26c186b57.jpg","9b45696485bf4e889a14d4def4c713c5.jpg"],"likes":[],"comments":[],"sid":6,"contentText":"测试","uid":18,"sendTime":"2022-08-17 09:26:41"},{"pics":[],"likes":[],"comments":[],"sid":2,"contentText":"111112312","uid":18,"sendTime":"2022-08-16 21:07:49"},{"pics":[],"likes":[],"comments":[],"sid":1,"contentText":"1111","uid":18,"sendTime":"2022-08-16 21:07:18"}],
    // "total":5,"size":10,"current":1,"orders":[],"optimizeCountSql":true,"searchCount":true,"countId":null,"maxLimit":null,"pages":1}

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public List<SocialUpdate> getRecords() {
        return records;
    }

    public void setRecords(List<SocialUpdate> records) {
        this.records = records;
    }


}
