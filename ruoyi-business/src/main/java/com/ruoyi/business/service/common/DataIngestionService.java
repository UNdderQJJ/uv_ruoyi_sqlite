package com.ruoyi.business.service.common;

import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.events.DataPoolCountChangedEvent;
import com.ruoyi.business.service.DataPool.DataPoolSchedulerService;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.mapper.DataPoolItemStaging.DataPoolItemStagingMapper;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用数据入库服务
 * 使用暂存表避免与实时任务更新发生锁竞争
 */
@Service
public class DataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DataIngestionService.class);


    @Resource
    private IDataPoolService dataPoolService;

    @Resource
    private DataPoolItemStagingMapper stagingMapper;

    /**
     * 批量入库数据项（使用 ParsingRuleConfig）
     */
    @Transactional(rollbackFor = Exception.class)
    public void ingestItems(Long poolId, List<String> items) {

        int inserted = 0;
        if (items != null && !items.isEmpty()) {
            try {
                Date now = DateUtils.getNowDate();
                List<DataPoolItem> toStage = items.stream().filter(s -> s != null && !s.isEmpty()).map(s -> {
                    DataPoolItem item = new DataPoolItem();
                    item.setPoolId(poolId);
                    item.setItemData(s);
                    item.setStatus("PENDING");
                    item.setPrintCount(0);
                    item.setReceivedTime(now);
                    item.setDelFlag("0");
                    item.setCreateTime(now);
                    item.setUpdateTime(now);
                    return item;
                }).collect(Collectors.toList());

                if (!toStage.isEmpty()) {
                    inserted = stagingMapper.batchInsertToStaging(toStage);
                }
            } catch (Exception e) {
                log.error("写入暂存表失败", e);
            }
        }
        try {
            //查询数据池
            DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
            int pingCount = 0;
            if (items != null) {
                pingCount = items.size();
            }
            dataPoolService.updateDataPoolCount(poolId, dataPool.getTotalCount()+pingCount, dataPool.getPendingCount()+pingCount);
        } catch (Exception e) {
            log.error("更新数据池计数失败", e);
        }
        if (inserted > 0) {
            log.info("成功写入 {} 条数据项到暂存表", inserted);
        }

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
