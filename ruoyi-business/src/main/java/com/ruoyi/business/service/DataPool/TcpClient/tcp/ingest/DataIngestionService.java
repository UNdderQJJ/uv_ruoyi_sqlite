package com.ruoyi.business.service.DataPool.TcpClient.tcp.ingest;

import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * TCP 数据入库服务
 * 负责在一个事务中批量写入数据项，并更新数据池计数
 */
@Service
public class DataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DataIngestionService.class);

    @Resource
    private IDataPoolItemService dataPoolItemService;

    @Resource
    private IDataPoolService dataPoolService;

    @Transactional(rollbackFor = Exception.class)
    public void ingestItems(Long poolId, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        int inserted = dataPoolItemService.batchAddDataItems(poolId, items);
        // 读取最新统计后，更新计数
        var stat = dataPoolItemService.getDataPoolStatistics(poolId);
        Long total = ((Integer) stat.getOrDefault("totalCount", 0)).longValue();
        Long pending = ((Integer) stat.getOrDefault("pendingCount", 0)).longValue();
        dataPoolService.updateDataPoolCount(poolId, total, pending);
        log.info("[DataIngestion] poolId={} 批量入库 {} 条，pending={} total={}", poolId, inserted, pending, total);
    }
}


