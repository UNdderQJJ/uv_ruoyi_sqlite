package com.ruoyi.business.events;

import com.ruoyi.business.domain.TaskInfo.TaskDispatchRequest;

public class TaskPauseEvent extends TaskDispatchEvent{


    public TaskPauseEvent(Object source, Long taskId) {
        super(source, taskId, "TASK_PAUSE");
    }

}
