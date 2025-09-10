package com.ruoyi.business.events;

/**
 * 任务停止事件
 */
public class TaskStopEvent extends TaskDispatchEvent {
    
    public TaskStopEvent(Object source, Long taskId) {
        super(source, taskId, "TASK_STOP");
    }
}
