package com.ruoyi.business.service.common;

import com.ruoyi.business.events.DataPoolCountChangedEvent;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 通用数据入库服务
 * 负责在一个事务中批量写入数据项，并更新数据池计数
 * 供所有数据源类型使用
 */
@Service
public class DataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DataIngestionService.class);

    @Resource
    private IDataPoolItemService dataPoolItemService;

    @Resource
    private IDataPoolService dataPoolService;

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 批量入库数据项（使用 ParsingRuleConfig）
     */
    @Transactional(rollbackFor = Exception.class)
    public void ingestItems(Long poolId, List<String> items) {

        // 入库前统计旧值
        var before = dataPoolItemService.getDataPoolStatistics(poolId);
        Long oldTotal = ((Integer) before.getOrDefault("totalCount", 0)).longValue();
        Long oldPending = ((Integer) before.getOrDefault("pendingCount", 0)).longValue();

        int inserted = 0;
        if (items != null && !items.isEmpty()) {
            inserted = dataPoolItemService.batchAddDataItems(poolId, items);
        }

        // 入库后统计新值
        var after = dataPoolItemService.getDataPoolStatistics(poolId);
        Long newTotal = ((Integer) after.getOrDefault("totalCount", 0)).longValue();
        Long newPending = ((Integer) after.getOrDefault("pendingCount", 0)).longValue();

        dataPoolService.updateDataPoolCount(poolId, newTotal, newPending);
        log.info("[DataIngestion] poolId={} 批量入库 {} 条，pending: {} -> {}, total: {} -> {}",
                poolId, inserted, oldPending, newPending, oldTotal, newTotal);

    }
    
    /**
     * 批量入库数据项（兼容旧版本，使用 Map 类型）
     */
    @Transactional(rollbackFor = Exception.class)
    public void ingestMapItems(Long poolId, List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        // 转换为字符串列表
        List<String> stringItems = items.stream()
                .map(this::convertItemToString)
                .toList();
        
        ingestItems(poolId, stringItems);
    }
    
    /**
     * 将 Map 类型的数据项转换为字符串
     */
    private String convertItemToString(Map<String, Object> item) {
        if (item == null) {
            return "";
        }
        
        try {
            StringBuilder content = new StringBuilder();
            for (Map.Entry<String, Object> entry : item.entrySet()) {
                if (content.length() > 0) {
                    content.append(", ");
                }
                content.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            return content.toString();
        } catch (Exception e) {
            log.warn("转换数据项内容失败，使用 toString() 方法", e);
            return item.toString();
        }
    }
}
