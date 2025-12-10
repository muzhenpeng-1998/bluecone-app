package com.bluecone.app.api.onboarding.dto;

/**
 * 门店基础信息请求。
 * 仅用于 H5 入驻引导流程创建首店草稿。
 */
public class StoreBasicInfoRequest {

    /**
     * 入驻会话 token。
     */
    private String sessionToken;

    /**
     * 门店名称。
     */
    private String storeName;

    /**
     * 城市。
     */
    private String city;

    /**
     * 区县。
     */
    private String district;

    /**
     * 详细地址。
     */
    private String address;

    /**
     * 经营场景：COFFEE / FOOD / BAKERY 等。
     */
    private String bizScene;

    /**
     * 门店联系电话，可选；为空时可回退使用会话中的手机号。
     */
    private String contactPhone;

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBizScene() {
        return bizScene;
    }

    public void setBizScene(String bizScene) {
        this.bizScene = bizScene;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}

