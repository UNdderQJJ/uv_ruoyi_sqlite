package com.ruoyi.business.service.TaskInfo.impl;

import com.github.pagehelper.Page;
import com.ruoyi.business.domain.TaskInfo.TaskInfo;
import com.ruoyi.business.enums.TaskStatus;
import com.ruoyi.business.mapper.TaskInfo.TaskInfoMapper;
import com.ruoyi.business.service.TaskInfo.ITaskInfoService;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.PageQueryUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务中心 TaskInfo 服务实现
 * 提供任务的增删改查和状态流转能力
 */
@Service
public class TaskInfoServiceImpl implements ITaskInfoService {

    @Resource
    private TaskInfoMapper taskInfoMapper;

    /** 新增任务 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTaskInfo(TaskInfo taskInfo) {
        if (taskInfo.getStatus() == null) {
            taskInfo.setStatus(TaskStatus.PENDING.getCode());
        }
        if (taskInfo.getDelFlag() == null) {
            taskInfo.setDelFlag(0);
        }
        return taskInfoMapper.insertTaskInfo(taskInfo);
    }

    /** 更新任务 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTaskInfo(TaskInfo taskInfo) {
        if (taskInfo.getId() == null) {
            throw new ServiceException("任务ID不能为空");
        }
        return taskInfoMapper.updateTaskInfo(taskInfo);
    }

    /** 批量删除任务（软删除） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTaskInfoByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return taskInfoMapper.deleteTaskInfoByIds(ids);
    }

    /** 根据ID查询任务 */
    @Override
    public TaskInfo selectTaskInfoById(Long id) {
        return taskInfoMapper.selectTaskInfoById(id);
    }

    /** 查询任务列表（可过滤） */
    @Override
    public List<TaskInfo> selectTaskInfoList(TaskInfo query) {
        return taskInfoMapper.selectTaskInfoList(query);
    }

    /** 查询任务列表（分页格式） */
    @Override
    public PageResult<TaskInfo> selectTaskInfoPageList(TaskInfo query, PageQuery pageQuery) {
        long startTime = System.currentTimeMillis();
        try {
            // 启动分页
            PageQueryUtils.startPage(pageQuery);
            
            // 执行查询
            List<TaskInfo> list = taskInfoMapper.selectTaskInfoPageList(query);
            
            // 获取分页信息
            Page<TaskInfo> page = (Page<TaskInfo>) list;
            
            // 构建分页结果
            return PageResult.of(list, page.getTotal(), pageQuery);
        } finally {
            // 清理分页
            PageQueryUtils.clearPage();
            
            // 性能监控
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 3000) { // 超过3秒记录警告
                System.out.println("任务列表分页查询耗时: " + duration + "ms");
            }
        }
    }

    /** 统计任务数量（可过滤） */
    @Override
    public Integer countTaskInfo(TaskInfo query) {
        return taskInfoMapper.countTaskInfo(query);
    }

    /** 更新任务状态 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTaskStatus(Long id, TaskStatus status) {
        if (id == null || status == null) {
            throw new ServiceException("任务ID和状态不能为空");
        }
        return taskInfoMapper.updateTaskStatus(id, status.getCode());
    }

    /** 启动任务（置为RUNNING） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int startTask(Long id) {
        return updateTaskStatus(id, TaskStatus.RUNNING);
    }

    /** 暂停任务（置为PAUSED） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pauseTask(Long id) {
        return updateTaskStatus(id, TaskStatus.PAUSED);
    }

    /** 恢复任务（从PAUSED置为RUNNING） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resumeTask(Long id) {
        return updateTaskStatus(id, TaskStatus.RUNNING);
    }

    /** 完成任务（置为COMPLETED） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int completeTask(Long id) {
        return updateTaskStatus(id, TaskStatus.COMPLETED);
    }

    /** 任务失败（置为ERROR） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int failTask(Long id) {
        return updateTaskStatus(id, TaskStatus.ERROR);
    }
}


