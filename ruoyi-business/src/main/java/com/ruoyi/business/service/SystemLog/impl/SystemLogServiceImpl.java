package com.ruoyi.business.service.SystemLog.impl;

import com.github.pagehelper.Page;
import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.mapper.SystemLog.ArchivedSystemLogMapper;
import com.ruoyi.business.mapper.SystemLog.SystemLogMapper;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import com.ruoyi.common.core.page.CursorPageQuery;
import com.ruoyi.common.core.page.CursorPageResult;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;
import com.ruoyi.common.utils.PageQueryUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 系统日志服务实现
 */
@Slf4j
@Service
public class SystemLogServiceImpl implements ISystemLogService {

    @Resource
    private SystemLogMapper systemLogMapper;

    @Resource
    private ArchivedSystemLogMapper archivedSystemLogMapper;

    @Value("${business.systemLog.archive.days:7}")
    private int archiveKeepDays;

    @Value("${business.systemLog.archive.batchSize:500}")
    private int archiveBatchSize;

    /** 新增一条日志 */
    @Override
    public int insert(SystemLog log) {
        return systemLogMapper.insertSystemLog(log);
    }

    @Override
    public int insert(String logType, String LogLEvel, Long taskId, Long deviceId, Long poolId, String content) {
        SystemLog log = new SystemLog();
        log.setLogType(logType);
        log.setLogLevel(LogLEvel);
        log.setTaskId(taskId);
        log.setDeviceId(deviceId);
        log.setPoolId(poolId);
        log.setContent(content);
        return insert(log);
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

    /** 分页查询日志（支持多条件过滤和排序） */
    @Override
    public PageResult<SystemLog> selectPageList(SystemLog query, PageQuery pageQuery) {
        long startTime = System.currentTimeMillis();
        try {
            // 启动分页
            PageQueryUtils.startPage(pageQuery);

            // 执行查询
            List<SystemLog> list = systemLogMapper.selectPageList(query);

            // 获取分页信息
            Page<SystemLog> page = (Page<SystemLog>) list;

            // 构建分页结果
            return PageResult.of(list, page.getTotal(), pageQuery);
        } finally {
            // 清理分页
            PageQueryUtils.clearPage();

            // 性能监控
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 5000) { // 超过5秒记录警告
                log.warn("慢查询检测: {}ms, 查询条件: {}, 分页参数: {}",
                    duration, query, pageQuery);
            }
        }
    }

    /** 游标分页查询日志（高性能，适合大数据量） */
    @Override
    public CursorPageResult<SystemLog> selectCursorPageList(SystemLog query, CursorPageQuery cursorQuery) {
        long startTime = System.currentTimeMillis();
        try {
            // 验证游标分页参数
            cursorQuery.validate();
            
            // 执行游标分页查询
            List<SystemLog> list = systemLogMapper.selectCursorPageList(query, cursorQuery);
            
            // 构建游标分页结果
            return CursorPageResult.of(list, cursorQuery.getPageSize());
        } finally {
            // 性能监控
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 3000) { // 超过3秒记录警告
                log.warn("游标分页慢查询检测: {}ms, 查询条件: {}, 游标参数: {}", 
                    duration, query, cursorQuery);
            }
        }
    }

    /** 日志归档任务（分批执行，确保归档表存在） */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void archiveOldLogs() {
        // 计算截止日期
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(Math.max(1, archiveKeepDays));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String cutoff = cutoffDate.format(formatter);

        int totalArchived = 0;
        while (true) {
            // 分批查询需归档的数据
            List<SystemLog> oldLogs = systemLogMapper.selectOldLogs(cutoff, archiveBatchSize);
            if (oldLogs == null || oldLogs.isEmpty()) {
                break;
            }

            // 批量写入归档表
            archivedSystemLogMapper.batchInsert(oldLogs);

            // 收集已归档ID并从主表删除
            Long[] ids = oldLogs.stream().map(SystemLog::getId).toArray(Long[]::new);
            systemLogMapper.deleteByIds(ids);

            totalArchived += oldLogs.size();

            // 若不足一批，说明已完成
            if (oldLogs.size() < archiveBatchSize) {
                break;
            }
        }

        if (totalArchived > 0) {
            log.info("归档完成：共归档 {} 条，截止时间 {}，批次大小 {}", totalArchived, cutoff, archiveBatchSize);
        } else {
            log.info("归档完成：无可归档数据，截止时间 {}", cutoff);
        }
    }
}


