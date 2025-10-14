package com.ruoyi.business.enums;

/**
 * 数据源类型枚举
 * 
 * @author ruoyi
 */
public enum SourceType
{
    /** U盘导入 */
    U_DISK("U_DISK", "U盘导入"),
    
    /** TCP 服务端 */
    TCP_SERVER("TCP_SERVER", "TCP 服务端"),
    
    /** TCP 客户端 */
    TCP_CLIENT("TCP_CLIENT", "TCP 客户端"),
    
    /** HTTP */
    HTTP("HTTP", "HTTP"),
    
    /** MQTT */
    MQTT("MQTT", "MQTT"),
    
    /** WebSocket */
    WEBSOCKET("WEBSOCKET", "WebSocket"),

    /**固定数据 */
    FIXED_DATA("FIXED_DATA", "固定数据"),

    /** 扫描录入 */
    SCAN_CODE("SCAN_CODE", "扫码录入");

    private final String code;
    private final String info;

    SourceType(String code, String info)
    {
        this.code = code;
        this.info = info;
    }

    public String getCode()
    {
        return code;
    }

    public String getInfo()
    {
        return info;
    }
}
