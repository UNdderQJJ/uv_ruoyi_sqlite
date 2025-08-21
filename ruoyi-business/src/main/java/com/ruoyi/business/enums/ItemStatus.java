package com.ruoyi.business.enums;

/**
 * 数据项状态枚举
 * 
 * @author ruoyi
 */
public enum ItemStatus {
    
    /** 待打印 */
    PENDING("PENDING", "待打印"),
    
    /** 正在打印 */
    PRINTING("PRINTING", "正在打印"),
    
    /** 打印成功 */
    PRINTED("PRINTED", "打印成功"),
    
    /** 打印失败 */
    FAILED("FAILED", "打印失败");
    
    private final String code;
    private final String info;
    
    ItemStatus(String code, String info) {
        this.code = code;
        this.info = info;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getInfo() {
        return info;
    }
    
    /**
     * 根据代码获取枚举
     */
    public static ItemStatus getByCode(String code) {
        for (ItemStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为有效状态
     */
    public static boolean isValidStatus(String code) {
        return getByCode(code) != null;
    }
}
