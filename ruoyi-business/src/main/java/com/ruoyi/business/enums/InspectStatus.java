package com.ruoyi.business.enums;

/**
 * 质检状态枚举
 */
public enum InspectStatus {
    PENDING("PENDING", "待扫码"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    RECYCLED("RECYCLED", "已回收");

    private final String code;
    private final String info;

    InspectStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public static InspectStatus fromCode(String code) {
        for (InspectStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        return null;
    }
}


