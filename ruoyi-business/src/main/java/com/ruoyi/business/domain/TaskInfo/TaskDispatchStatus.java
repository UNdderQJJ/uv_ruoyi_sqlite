package com.ruoyi.business.domain.TaskInfo;

import lombok.Data;

/**
 * 任务调度状态
 * 用于跟踪任务调度过程的整体状态
 */
@Data
public class TaskDispatchStatus {
    
    /** 任务ID */
    private Long taskId;
    
    /** 调度状态 */
    private String status; // INITIALIZING, RUNNING, PAUSED, COMPLETED, FAILED, STOPPED
    
    /** 开始时间 */
    private Long startTime;
    
    /** 结束x时间 */
    private Long endTime;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 总指令数量 */
    private Integer totalCommandCount;
    
    /** 已发送指令数量 */
    private Integer sentCommandCount;

    /** 已接收指令数量 */
    private Integer receivedCommandCount;

    /** 计划打印数量 */
    private Integer plannedPrintCount;
    
    /** 已完成指令数量 */
    private Integer completedCommandCount;
    
    /** 失败指令数量 */
    private Integer failedCommandCount;

    /** 原已完成指令数量 */
    private Integer originalCommandCount;
    
    /** 关联设备数量 */
    private Integer deviceCount;
    
    /** 在线设备数量 */
    private Integer onlineDeviceCount;
    
    /** 数据池ID */
    private Long poolId;
    
    /** 任务名称 */
    private String taskName;
    
    /** 任务描述 */
    private String description;
    
    /** 进度百分比 */
    private Double progressPercentage;
    
    /** 预计完成时间 */
    private Long estimatedCompletionTime;
    
    /** 实际完成时间 */
    private Long actualCompletionTime;

    /** 是否启用质检 0-否 1-是  */
    private Integer enableCheck;
}
