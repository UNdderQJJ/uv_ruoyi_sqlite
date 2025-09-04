package com.ruoyi.business.mapper.TaskInfo;

import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务-设备关联 Mapper
 */
public interface TaskDeviceLinkMapper {

    /** 新增关联 */
    int insertTaskDeviceLink(TaskDeviceLink link);

    /** 批量新增关联 */
    int batchInsertTaskDeviceLinks(@Param("list") List<TaskDeviceLink> links);

    /** 更新关联 */
    int updateTaskDeviceLink(TaskDeviceLink link);

    /** 软删除（按任务ID） */
    int deleteByTaskId(@Param("taskId") Long taskId);

    /** 查询列表 */
    List<TaskDeviceLink> selectTaskDeviceLinkList(TaskDeviceLink query);
}


