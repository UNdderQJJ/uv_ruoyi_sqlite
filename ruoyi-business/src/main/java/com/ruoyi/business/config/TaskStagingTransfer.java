package com.ruoyi.business.config;

import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.mapper.DataPoolItemStaging.DataPoolItemStagingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

/**
 * 将暂存表数据迁移到主表 + 后台分批真删除清理
 */
@Component
public class TaskStagingTransfer {

    private static final Logger log = LoggerFactory.getLogger(TaskStagingTransfer.class);

    @Resource
    private DataPoolItemStagingMapper stagingMapper;

    @Resource
    private IDataPoolItemService dataPoolItemService;

    @Scheduled(fixedRate = 5000)
    @Transactional(rollbackFor = Exception.class)
    public void transferStagingDataToMainTable() {
        int batchSize = 3000;
        int totalTransferred = 0;
        while (true) {
            int inserted = stagingMapper.insertFromStagingLimit(batchSize);
            if (inserted <= 0) {
                break;
            }
            stagingMapper.deleteFromStagingByRowIdLimit(inserted);
            totalTransferred += inserted;
            if (inserted < batchSize) {
                break;
            }
        }
        if (totalTransferred > 0) {
            log.info("[StagingTransfer] 分批迁移完成，共迁移 {} 条", totalTransferred);
        }
    }

    /**
     * 后台分批真删除：对已软删的数据执行LIMIT删除，降低锁持有时间。
     */
    public int hardDeleteBatch(Long poolId, int batchSize) {
        int totalDeleted = 0;
        while (true) {
            int affected = dataPoolItemService.hardDeleteByPoolIdLimit(poolId, batchSize);
            totalDeleted += affected;
            if (affected < batchSize) {
                break;
            }
        }
        if (totalDeleted > 0) {
            log.info("[HardDelete] poolId={} 硬删完成，删除 {} 条", poolId, totalDeleted);
        }
        return totalDeleted;
    }
}
