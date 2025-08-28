package com.ruoyi.business.enums;

import lombok.Data;
import lombok.Getter;

/**
 * 连接状态枚举
 */
@Getter
public enum ConnectionState {
    DISCONNECTED("DISCONNECTED", "已断开"),
    CONNECTING("CONNECTING", "正在连接"),
    CONNECTED("CONNECTED", "已连接"),
    LISTENING("LISTENING", "监听中"),
    ERROR("ERROR", "错误状态");

    private final String code;
    private final String info;

    ConnectionState(String code, String info) {
        this.code = code;
        this.info = info;
    }

    /**
     * 根据状态码获取枚举值
     */
    public static ConnectionState fromCode(String code) {
        for (ConnectionState state : values()) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return null;
    }
}


