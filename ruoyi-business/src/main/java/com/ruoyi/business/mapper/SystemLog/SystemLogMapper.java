package com.ruoyi.business.mapper.SystemLog;

import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.common.core.page.CursorPageQuery;
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

    /** 分页查询日志（支持多条件过滤和排序） */
    List<SystemLog> selectPageList(SystemLog query);

    /** 游标分页查询日志（高性能，适合大数据量） */
    List<SystemLog> selectCursorPageList(@Param("query") SystemLog query, @Param("cursorQuery") CursorPageQuery cursorQuery);

    /**
     * 查询需要归档的旧日志（按时间阈值，限制数量）
     */
    List<SystemLog> selectOldLogs(@Param("cutoff") String cutoff, @Param("limit") int limit);

    /**
     * 确保归档表存在（无则创建）
     */
    int ensureArchiveTable();
}


