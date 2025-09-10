package com.ruoyi.business.events;

import com.ruoyi.business.domain.TaskInfo.TaskDispatchRequest;

/**
 * 任务启动事件
 */
public class TaskStartEvent extends TaskDispatchEvent {
    
    private final TaskDispatchRequest request;
    
    public TaskStartEvent(Object source, TaskDispatchRequest request) {
        super(source, request.getTaskId(), "TASK_START");
        this.request = request;
    }
    
    public TaskDispatchRequest getRequest() {
        return request;
    }
}
