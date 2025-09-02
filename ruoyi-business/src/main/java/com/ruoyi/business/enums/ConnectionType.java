package com.ruoyi.business.enums;

import lombok.Data;
import lombok.Getter;

/**
 * 连接类型枚举
 * 
 * @author ruoyi
 */
public enum ConnectionType {
    
    /** TCP网络连接 */
    TCP("TCP", "TCP网络"),
    
    /** 串口连接 */
    SERIAL("SERIAL", "串口");

    private final String code;
    private final String info;

    ConnectionType(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public static ConnectionType fromCode(String code) {
        for (ConnectionType type : ConnectionType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的连接类型: " + code);
    }
}
