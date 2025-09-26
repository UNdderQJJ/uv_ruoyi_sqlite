package com.ruoyi.business.domain.TaskInfo;

import lombok.Data;

/**
 * 设备任务状态
 * 用于跟踪设备在任务执行过程中的状态信息
 */
@Data
public class DeviceTaskStatus {
    
    /** 设备ID */
    private String deviceId;
    
    /** 设备状态 */
    private String status; // IDLE, PRINTING, ERROR, OFFLINE
    
    /** 在途指令数量 */
    private Integer inFlightCount;
    
    /** 最后心跳时间 */
    private Long lastHeartbeat;
    
    /** 当前任务ID */
    private Long currentTaskId;
    
    /** 已完成数量 */
    private Integer completedCount;

    /** 当前已完成数量（不包含之前启动的完成数量） */
    private Integer currentCompletedCount;
    
    /** 分配数量 */
    private Integer assignedCount;
    
    /** 设备名称 */
    private String deviceName;
    
    /** 设备IP地址 */
    private String ipAddress;
    
    /** 设备端口 */
    private Integer port;
    
    /** 连接状态 */
    private String connectionStatus; // CONNECTED, DISCONNECTED, CONNECTING
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 最后错误时间 */
    private Long lastErrorTime;
    
    /** 设备类型 */
    private String deviceType;
    
    /** 设备UUID */
    private String deviceUuid;

    /** 缓存池大小**/
    private Integer cachePoolSize;
}
