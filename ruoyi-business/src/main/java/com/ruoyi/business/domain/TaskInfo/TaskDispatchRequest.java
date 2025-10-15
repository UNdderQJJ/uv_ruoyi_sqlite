package com.ruoyi.business.domain.TaskInfo;

import lombok.Data;

/**
 * 任务调度请求
 * 用于启动新任务调度的参数封装
 */
@Data
public class TaskDispatchRequest {
    
    /** 任务ID */
    private Long taskId;
    
    /** 关联的设备ID数组 */
    private Long[] deviceIds;
    
    /** 数据池ID */
    private Long poolId;
    
    /** 预加载数量，数据池大小 */
    private Integer preloadCount;

    /** 计划打印数量  */
    private Integer printCount;

    /** 原已完成数量 */
    private Integer originalCount;

    /** 已发送指令数量 */
    private Integer sentCommandCount;

    /** 已接收指令数量 */
    private Integer receivedCommandCount;

    /** 批处理大小 */
    private Integer batchSize;
    
    /** 任务名称 */
    private String taskName;
    
    /** 任务描述 */
    private String description;
    
    /** 设备文件配置ID */
    private Long deviceFileConfigId;
    
    /** 数据池模板ID */
    private Long poolTemplateId;
    
    /** 分配给设备的打印数量 */
    private Integer assignedQuantity;

    /** 是否启用质检  */
    private Boolean enableCheck;
}
