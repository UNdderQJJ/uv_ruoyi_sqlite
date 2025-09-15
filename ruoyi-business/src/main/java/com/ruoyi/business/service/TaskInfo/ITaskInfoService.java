package com.ruoyi.business.service.TaskInfo;

import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import com.ruoyi.business.enums.TaskStatus;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;

import java.util.List;

/**
 * 任务中心 TaskInfo 服务接口
 * 定义任务的增删改查与状态流转能力
 */
public interface ITaskInfoService {

    /** 新增任务 */
    int insertTaskInfo(TaskInfo taskInfo);

    /** 更新任务 */
    int updateTaskInfo(TaskInfo taskInfo);

    /** 批量删除任务（软删除） */
    int deleteTaskInfoByIds(Long[] ids);

    /** 根据ID查询任务 */
    TaskInfo selectTaskInfoById(Long id);

    /** 查询任务列表（可过滤） */
    List<TaskInfo> selectTaskInfoList(TaskInfo query);

    /** 查询任务列表（分页格式） */
    PageResult<TaskInfo> selectTaskInfoPageList(TaskInfo query, PageQuery pageQuery);

    /** 统计任务数量（可过滤） */
    Integer countTaskInfo(TaskInfo query);

    /** 更新任务状态 */
    int updateTaskStatus(Long id, TaskStatus status);

    /** 启动任务（置为RUNNING） */
    int startTask(Long id);

    /** 暂停任务（置为PAUSED） */
    int pauseTask(Long id);

    /** 恢复任务（从PAUSED置为RUNNING） */
    int resumeTask(Long id);

    /** 完成任务（置为COMPLETED） */
    int completeTask(Long id);

    /** 任务失败（置为ERROR） */
    int failTask(Long id);
}


