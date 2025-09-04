package com.ruoyi.business.service.TaskInfo.impl;

import com.ruoyi.business.domain.TaskInfo.TaskDeviceLink;
import com.ruoyi.business.mapper.TaskInfo.TaskDeviceLinkMapper;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务-设备关联 服务实现
 */
@Service
public class TaskDeviceLinkServiceImpl implements ITaskDeviceLinkService {

    @Resource
    private TaskDeviceLinkMapper taskDeviceLinkMapper;

    /** 批量创建关联 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateLinks(List<TaskDeviceLink> links) {
        if (links == null || links.isEmpty()) {
            return 0;
        }
        return taskDeviceLinkMapper.batchInsertTaskDeviceLinks(links);
    }

    /** 更新关联 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateLink(TaskDeviceLink link) {
        return taskDeviceLinkMapper.updateTaskDeviceLink(link);
    }

    /** 按任务ID删除关联（软删除） */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByTaskId(Long taskId) {
        return taskDeviceLinkMapper.deleteByTaskId(taskId);
    }

    /** 列表查询 */
    @Override
    public List<TaskDeviceLink> list(TaskDeviceLink query) {
        return taskDeviceLinkMapper.selectTaskDeviceLinkList(query);
    }
}


