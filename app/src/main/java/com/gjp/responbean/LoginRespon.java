package com.gjp.responbean;

/**
 * @author : huangm
 * @time : 2021/4/2
 * @subscri :
 */

public class LoginRespon {

    /**
     * accessToken : 用户票据
     * state : 用户的状态
     * orgId: 组织Id
     * userId : 用户的id
     * username : 用户的姓名
     */

    private String accessToken;
    private String state;
    private String orgId="";
    private String userId="";
    private String username;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
