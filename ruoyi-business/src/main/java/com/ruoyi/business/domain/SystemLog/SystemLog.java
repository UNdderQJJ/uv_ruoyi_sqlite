package com.ruoyi.business.domain.SystemLog;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 系统日志实体
 * 对应表: system_log
 */
@Data
public class SystemLog {

    /** 主键ID */
    private Long id;

    /** 日志记录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date logTime;

    /** 日志类型 (PRINT/SCAN/SYSTEM)，建议使用枚举 SystemLogType 入库 */
    private String logType;

    /** 任务ID */
    private Long taskId;

    /** 任务名称 */
    private String taskName;

    /** 设备ID */
    private Long deviceId;

    /** 设备名称 */
    private String deviceName;

    /** 数据池ID */
    private Long poolId;

    /** 数据池名称 */
    private String poolName;

    /** 日志级别: INFO/ERROR/DEBUG/WARN，建议使用枚举 SystemLogLevel 入库 */
    private String logLevel;

    /** 事件描述 */
    private String content;
}


