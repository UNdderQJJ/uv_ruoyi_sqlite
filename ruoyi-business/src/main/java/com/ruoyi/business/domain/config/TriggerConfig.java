package com.ruoyi.business.domain.config;

/**
 * 触发配置类
 * 用于配置数据池的触发条件
 * 
 * @author ruoyi
 */
public class TriggerConfig {
    
    /** 触发类型（THRESHOLD - 阈值触发，INTERVAL - 定时触发，MANUAL - 手动触发） */
    private String triggerType;
    
    /** 阈值（当待打印数据少于此值时触发读取） */
    private Integer threshold;
    
    /** 批次大小（每次最多读取的数据量） */
    private Integer batchSize;
    
    /** 定时间隔（秒） */
    private Integer intervalSeconds;
    
    /** 是否自动重试 */
    private Boolean autoRetry;
    
    /** 最大重试次数 */
    private Integer maxRetries;

      /** 请求命令 */
    private String requestCommand;

    /** 订阅主题 (MQTT专用) */
    private String subscribeTopic;

    /** 发布主题 (MQTT专用) */
    private String publishTopic;

    /** 请求载荷 (MQTT专用) */
    private String requestPayload;


    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public Boolean getAutoRetry() {
        return autoRetry;
    }

    public void setAutoRetry(Boolean autoRetry) {
        this.autoRetry = autoRetry;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setRequestCommand(String requestCommand) {
        this.requestCommand = requestCommand;
    }

    public String getSubscribeTopic() {
        return subscribeTopic;
    }

    public void setSubscribeTopic(String subscribeTopic) {
        this.subscribeTopic = subscribeTopic;
    }

    public String getPublishTopic() {
        return publishTopic;
    }

    public void setPublishTopic(String publishTopic) {
        this.publishTopic = publishTopic;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }
}