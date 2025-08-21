package com.ruoyi.business.domain.config;

import lombok.Data;

/**
 * 触发配置类
 * 用于配置数据池的触发条件
 * 
 * @author ruoyi
 */
@Data
public class TriggerConfig {

    /**
     * 触发类型（THRESHOLD - 阈值触发，INTERVAL - 定时触发，MANUAL - 手动触发）
     */
    private String triggerType;

    /**
     * 阈值（当待打印数据少于此值时触发读取）
     */
    private Integer threshold;

    /**
     * 批次大小（每次最多读取的数据量）
     */
    private Integer batchSize;

    /**
     * 定时间隔（秒）
     */
    private Integer intervalSeconds;

    /**
     * 是否自动重试
     */
    private Boolean autoRetry;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 请求命令
     */
    private String requestCommand;

    /**
     * 订阅主题 (MQTT专用)
     */
    private String subscribeTopic;

    /**
     * 发布主题 (MQTT专用)
     */
    private String publishTopic;

    /**
     * 请求载荷 (MQTT专用)
     */
    private String requestPayload;


}