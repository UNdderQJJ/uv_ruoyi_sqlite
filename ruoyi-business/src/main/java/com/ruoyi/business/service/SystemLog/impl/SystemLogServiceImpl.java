package com.ruoyi.business.service.SystemLog.impl;

import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.mapper.SystemLog.SystemLogMapper;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统日志服务实现
 */
@Service
public class SystemLogServiceImpl implements ISystemLogService {

    @Resource
    private SystemLogMapper systemLogMapper;

    /** 新增一条日志 */
    @Override
    public int insert(SystemLog log) {
        return systemLogMapper.insertSystemLog(log);
    }

    /** 批量新增日志 */
    @Override
    public int batchInsert(List<SystemLog> list) {
        if (list == null || list.isEmpty()) return 0;
        return systemLogMapper.batchInsert(list);
    }

    /** 按ID数组批量删除日志 */
    @Override
    public int deleteByIds(Long[] ids) {
        if (ids == null || ids.length == 0) return 0;
        return systemLogMapper.deleteByIds(ids);
    }

    /** 根据ID查询日志 */
    @Override
    public SystemLog selectById(Long id) {
        return systemLogMapper.selectById(id);
    }

    /** 列表查询日志（支持多条件过滤） */
    @Override
    public List<SystemLog> selectList(SystemLog query) {
        return systemLogMapper.selectList(query);
    }

    /** 统计日志数量（支持多条件过滤） */
    @Override
    public Integer count(SystemLog query) {
        return systemLogMapper.count(query);
    }
}


