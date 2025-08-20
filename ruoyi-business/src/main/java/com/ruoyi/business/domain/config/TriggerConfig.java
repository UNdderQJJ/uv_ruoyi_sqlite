package com.ruoyi.business.domain.config;

/**
 * 触发条件配置
 * 
 * @author ruoyi
 */
public class TriggerConfig {
    
    /** 触发类型 */
    private String triggerType;
    
    /** 阈值 */
    private Integer threshold;
    
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

    public String getRequestCommand() {
        return requestCommand;
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
