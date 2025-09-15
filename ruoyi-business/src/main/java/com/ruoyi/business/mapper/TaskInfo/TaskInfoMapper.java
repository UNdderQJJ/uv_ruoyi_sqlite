package com.ruoyi.business.mapper.TaskInfo;

import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务中心 TaskInfo Mapper
 * 提供任务信息的增删改查等数据库操作
 */
public interface TaskInfoMapper {

    /** 根据ID查询任务 */
    TaskInfo selectTaskInfoById(@Param("id") Long id);

    /** 查询任务列表（可按条件过滤） */
    List<TaskInfo> selectTaskInfoList(TaskInfo query);

    /** 查询任务列表（分页格式） */
    List<TaskInfo> selectTaskInfoPageList(TaskInfo query);

    /** 统计任务数量（可按条件过滤） */
    Integer countTaskInfo(TaskInfo query);

    /** 新增任务 */
    int insertTaskInfo(TaskInfo taskInfo);

    /** 更新任务 */
    int updateTaskInfo(TaskInfo taskInfo);

    /** 批量软删除任务（del_flag=2） */
    int deleteTaskInfoByIds(@Param("ids") Long[] ids);

    /** 更新任务状态 */
    int updateTaskStatus(@Param("id") Long id, @Param("status") String status);
}


