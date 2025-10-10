package com.ruoyi.business.domain.TaskInfo;

import lombok.Data;

/**
 * 打印指令
 * 用于在调度中心和各服务之间传递的指令对象
 */
@Data
public class PrintCommand {
    
    /** 指令唯一ID */
    private Long id;
    
    /** 目标设备ID - 由调度器动态分配 */
    private String deviceId;
    
    /** 指令内容 */
    private String command;
    
    /** 指令数据 */
    private String data;
    
    /** 所属任务ID */
    private Long taskId;
    
    /** 优先级 */
    private Integer priority;
    
    /** 创建时间 */
    private Long createTime;
    
    /** 指令状态 */
    private String status; // PENDING, SENT, COMPLETED, FAILED
    
    /** 重试次数 */
    private Integer retryCount;
    
    /** 最大重试次数 */
    private Integer maxRetryCount;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 完成时间 */
    private Long completedTime;
    
    /** 发送时间 */
    private Long sentTime;
}
