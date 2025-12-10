package com.bluecone.app.infra.wechat.openplatform;

import java.util.List;

/**
 * 使用授权码查询授权结果。
 */
public class QueryAuthResult {

    private Integer errcode;
    private String errmsg;

    private String authorizerAppId;
    private String authorizerRefreshToken;
    private Integer funcInfoCount;
    private List<Integer> funcScopeCategories;

    public boolean isSuccess() {
        return errcode == null || errcode == 0;
    }

    public Integer getErrcode() {
        return errcode;
    }

    public void setErrcode(Integer errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

    public String getAuthorizerAppId() {
        return authorizerAppId;
    }

    public void setAuthorizerAppId(String authorizerAppId) {
        this.authorizerAppId = authorizerAppId;
    }

    public String getAuthorizerRefreshToken() {
        return authorizerRefreshToken;
    }

    public void setAuthorizerRefreshToken(String authorizerRefreshToken) {
        this.authorizerRefreshToken = authorizerRefreshToken;
    }

    public Integer getFuncInfoCount() {
        return funcInfoCount;
    }

    public void setFuncInfoCount(Integer funcInfoCount) {
        this.funcInfoCount = funcInfoCount;
    }

    public List<Integer> getFuncScopeCategories() {
        return funcScopeCategories;
    }

    public void setFuncScopeCategories(List<Integer> funcScopeCategories) {
        this.funcScopeCategories = funcScopeCategories;
    }
}

