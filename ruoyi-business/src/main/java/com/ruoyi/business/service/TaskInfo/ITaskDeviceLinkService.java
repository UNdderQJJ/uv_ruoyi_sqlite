package com.ruoyi.business.service.TaskInfo;

import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;

import java.util.List;

/**
 * 任务-设备关联 服务接口
 */
public interface ITaskDeviceLinkService {

    /** 批量创建关联 */
    int batchCreateLinks(List<TaskDeviceLink> links);

    /** 更新关联 */
    int updateLink(TaskDeviceLink link);

    /** 按任务ID删除关联（软删除） */
    int deleteByTaskId(Long taskId);

    /** 列表查询 */
    List<TaskDeviceLink> list(TaskDeviceLink query);
}


