package com.ruoyi.business.mapper.DataPoolItemStaging;

import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import java.util.List;

/**
 * 数据池热数据暂存表 Mapper 接口
 */
public interface DataPoolItemStagingMapper {
    /**
     * 批量插入到暂存表（原接口，保留以兼容）
     */
    int batchInsertToStaging(List<DataPoolItem> dataPoolItems);

    /**
     * 查询暂存表全部数据（不再在迁移中使用，保留以兼容）
     */
    List<DataPoolItem> selectAll();

    /**
     * 清空暂存表（保留以兼容）
     */
    int deleteAll();

    /**
     * 分批从暂存表插入到主表（INSERT…SELECT + LIMIT）
     * 返回受影响行数
     */
    int insertFromStagingLimit(int batchSize);

    /**
     * 分批按 rowid 从暂存表删除（与 insertFromStagingLimit 配套）
     * 返回受影响行数
     */
    int deleteFromStagingByRowIdLimit(int batchSize);
}
