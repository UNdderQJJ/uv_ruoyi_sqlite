package com.ruoyi.business.service.SystemLog;

import com.ruoyi.business.domain.SystemLog.SystemLog;

import java.util.List;

/**
 * 系统日志服务接口
 * 提供日志的新增、删除、查询与统计能力。
 */
public interface ISystemLogService {

    /** 新增一条日志 */
    int insert(SystemLog log);

    /** 批量新增日志 */
    int batchInsert(List<SystemLog> list);

    /** 按ID数组批量删除日志 */
    int deleteByIds(Long[] ids);

    /** 根据ID查询日志 */
    SystemLog selectById(Long id);

    /** 列表查询日志（支持多条件过滤） */
    List<SystemLog> selectList(SystemLog query);

    /** 统计日志数量（支持多条件过滤） */
    Integer count(SystemLog query);
}


