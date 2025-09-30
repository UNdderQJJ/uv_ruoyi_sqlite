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

    /** 更新关联设备状态 */
    int updateDeviceStatus(Long taskId, Long deviceId, String status);

    /** 按任务ID更新状态 */
    int updateStatusByTaskId(Long taskId, String status);


    /** 列表查询 */
    List<TaskDeviceLink> list(TaskDeviceLink query);

    /** 根据任务ID查询关联列表 */
    List<TaskDeviceLink> listByTaskId(Long taskId);

    /** 根据任务ID和设备ID查询关联 */
    TaskDeviceLink selectByTaskIdAndDeviceId(Long taskId, Long deviceId);

    /** 按任务ID和设备ID删除关联（软删除） */
    int deleteByTaskIdAndDeviceId(Long taskId, Long deviceId);
}


