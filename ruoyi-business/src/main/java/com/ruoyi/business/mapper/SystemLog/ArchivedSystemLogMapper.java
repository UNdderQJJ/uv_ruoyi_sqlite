package com.ruoyi.business.mapper.SystemLog;

import com.ruoyi.business.domain.SystemLog.SystemLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 归档的系统日志 Mapper
 */
public interface ArchivedSystemLogMapper {

    /** 批量插入归档日志 */
    int batchInsert(@Param("list") List<SystemLog> list);
}


