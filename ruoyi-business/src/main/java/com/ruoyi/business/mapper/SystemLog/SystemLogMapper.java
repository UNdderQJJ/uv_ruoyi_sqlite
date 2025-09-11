package com.ruoyi.business.mapper.SystemLog;

import com.ruoyi.business.domain.SystemLog.SystemLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统日志 Mapper
 */
public interface SystemLogMapper {

    /** 新增日志 */
    int insertSystemLog(SystemLog log);

    /** 批量新增日志 */
    int batchInsert(@Param("list") List<SystemLog> list);

    /** 根据ID删除日志 */
    int deleteByIds(@Param("ids") Long[] ids);

    /** 根据ID查询 */
    SystemLog selectById(@Param("id") Long id);

    /** 列表查询（可过滤） */
    List<SystemLog> selectList(SystemLog query);

    /** 统计数量（可过滤） */
    Integer count(SystemLog query);
}


