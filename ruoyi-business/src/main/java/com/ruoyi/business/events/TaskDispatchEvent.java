package com.ruoyi.business.events;

import org.springframework.context.ApplicationEvent;

/**
 * 任务调度事件基类
 */
public abstract class TaskDispatchEvent extends ApplicationEvent {
    
    private final Long taskId;
    private final String eventType;
    
    public TaskDispatchEvent(Object source, Long taskId, String eventType) {
        super(source);
        this.taskId = taskId;
        this.eventType = eventType;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public String getEventType() {
        return eventType;
    }
}
