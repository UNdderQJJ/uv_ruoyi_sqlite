package com.ruoyi.business.enums;

/**
 * 串口校验位枚举
 * 
 * @author ruoyi
 */
public enum Parity {
    
    /** 无校验 */
    NONE("NONE", "无校验"),
    
    /** 奇校验 */
    ODD("ODD", "奇校验"),
    
    /** 偶校验 */
    EVEN("EVEN", "偶校验"),
    
    /** 标记校验 */
    MARK("MARK", "标记校验"),
    
    /** 空格校验 */
    SPACE("SPACE", "空格校验");

    private final String code;
    private final String info;

    Parity(String code, String info) {
        this.code = code;
        this.info = info;
    }

    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public static Parity fromCode(String code) {
        for (Parity parity : Parity.values()) {
            if (parity.getCode().equals(code)) {
                return parity;
            }
        }
        throw new IllegalArgumentException("未知的校验位类型: " + code);
    }
}
