package com.ruoyi.business.service.SystemLog;

import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.common.core.page.CursorPageQuery;
import com.ruoyi.common.core.page.CursorPageResult;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;

import java.util.List;

/**
 * 系统日志服务接口
 * 提供日志的新增、删除、查询与统计能力。
 */
public interface ISystemLogService {

    /** 新增一条日志 */
    int insert(SystemLog log);

    /** 新增一条日志
     * @param logType 日志类型
     * @param LogLEvel 日志级别
     * @param taskId 任务ID
     * @param deviceId 设备ID
     * @param poolId 数据池ID
     * @param content 日志内容
     */
    int insert(String logType,String LogLEvel,Long taskId,Long deviceId,Long poolId,String content);

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

    /** 分页查询日志（支持多条件过滤和排序） */
    PageResult<SystemLog> selectPageList(SystemLog query, PageQuery pageQuery);

    /** 游标分页查询日志（高性能，适合大数据量） */
   CursorPageResult<SystemLog> selectCursorPageList(SystemLog query, CursorPageQuery cursorQuery);
}


