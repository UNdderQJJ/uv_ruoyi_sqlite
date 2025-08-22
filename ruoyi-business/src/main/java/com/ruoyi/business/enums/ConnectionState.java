package com.ruoyi.business.enums;

/**
 * 连接状态枚举
 */
public enum ConnectionState {
    DISCONNECTED("DISCONNECTED", "已断开"),
    CONNECTING("CONNECTING", "正在连接"),
    CONNECTED("CONNECTED", "已连接");

    private final String code;
    private final String info;

    ConnectionState(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }
}


