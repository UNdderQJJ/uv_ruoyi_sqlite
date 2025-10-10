package com.ruoyi.business.enums;

/**
 * 变量类型枚举
 * 
 * @author ruoyi
 */
public enum VariableType {
    
    /** 文本 */
    TEXT("TEXT", "文本"),
    
    /** 数字 */
    NUMBER("NUMBER", "数字"),
    
    /** 日期 */
    DATE("DATE", "日期"),
    
    /** 条型码 */
    SERIAL("SERIAL", "条型码"),
    
    /** 二维码 */
    QR_CODE("QR_CODE", "二维码");

    private final String code;
    private final String info;

    VariableType(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public static VariableType fromCode(String code) {
        for (VariableType type : VariableType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的变量类型: " + code);
    }
}
