package com.ruoyi.business.service.DataPoolItem.impl;

import com.github.pagehelper.Page;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.business.enums.ItemStatus;
import com.ruoyi.business.mapper.DataPoolItem.DataPoolItemMapper;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.common.core.page.PageQuery;
import com.ruoyi.common.core.page.PageResult;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.PageQueryUtils;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据池热数据Service业务层处理
 * 
 * @author ruoyi
 */
@Service
public class DataPoolItemServiceImpl implements IDataPoolItemService {
    
    @Resource
    private DataPoolItemMapper dataPoolItemMapper;

    /**
     * 查询数据池热数据
     * 
     * @param id 数据池热数据主键
     * @return 数据池热数据
     */
    @Override
    public DataPoolItem selectDataPoolItemById(Long id) {
        return dataPoolItemMapper.selectDataPoolItemById(id);
    }

    /**
     * 查询数据池热数据列表
     * 
     * @param dataPoolItem 数据池热数据
     * @return 数据池热数据
     */
    @Override
    public List<DataPoolItem> selectDataPoolItemList(DataPoolItem dataPoolItem) {
        return dataPoolItemMapper.selectDataPoolItemList(dataPoolItem);
    }

    /**
     * 根据数据池ID查询热数据列表
     * 
     * @param poolId 数据池ID
     * @return 数据池热数据集合
     */
    @Override
    public List<DataPoolItem> selectDataPoolItemByPoolId(Long poolId) {
        return dataPoolItemMapper.selectDataPoolItemByPoolId(poolId);
    }

    /**
     * 根据状态查询热数据列表
     * 
     * @param status 数据状态
     * @return 数据池热数据集合
     */
    @Override
    public List<DataPoolItem> selectDataPoolItemByStatus(String status) {
        return dataPoolItemMapper.selectDataPoolItemByStatus(status);
    }

    /**
     * 获取待打印的数据项（按接收时间排序）
     * 
     * @param poolId 数据池ID（可选）
     * @param limit 限制数量
     * @return 待打印数据项列表
     */
    @Override
    public List<DataPoolItem> selectPendingItems(Long poolId, Integer limit) {
        return dataPoolItemMapper.selectPendingItems(poolId, limit);
    }

    /**
     * 获取待打印的数据项（分页格式）
     * 
     * @param queryItem 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    @Override
    public PageResult<DataPoolItem> selectPendingItemsPage(DataPoolItem queryItem, PageQuery pageQuery) {
        long startTime = System.currentTimeMillis();
        try {
            // 启动分页
            PageQueryUtils.startPage(pageQuery);
            
            // 执行查询
            List<DataPoolItem> list = dataPoolItemMapper.selectPendingItemsPage(queryItem);
            
            // 获取分页信息
            Page<DataPoolItem> page = (Page<DataPoolItem>) list;
            
            // 构建分页结果
            return PageResult.of(list, page.getTotal(), pageQuery);
        } finally {
            // 清理分页
            PageQueryUtils.clearPage();
            
            // 性能监控
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 3000) { // 超过3秒记录警告
                System.out.println("待打印数据分页查询耗时: " + duration + "ms");
            }
        }
    }

/**
 * 更新数据项状态（批量），此方法会根据 deviceId 对数据项进行分组，然后分别更新。
 *
 * @param itemList 待更新的数据项列表
 * @param status   要更新的目标新状态
 */
@Override
public void updateDataPoolItemsStatus(List<DataPoolItem> itemList, String status) {
    // 1. 边界条件检查：如果传入的列表为空或 null，直接返回 0
    if (itemList == null || itemList.isEmpty()) {
        return;
    }

    // 2. 核心步骤：使用 Stream API 的 groupingBy 按 deviceId 对 itemList 进行分组
    // 结果为一个 Map，Key 是 deviceId，Value 是该 deviceId 对应的 DataPoolItem 列表
    Map<String, List<DataPoolItem>> groupedByDeviceId = itemList.stream()
            .collect(Collectors.groupingBy(DataPoolItem::getDeviceId));

    // 3. 遍历分组后的 Map，对每个 deviceId 的分组执行一次批量更新
    for (Map.Entry<String, List<DataPoolItem>> entry : groupedByDeviceId.entrySet()) {
        Long currentDeviceId = Long.valueOf(entry.getKey());
        List<DataPoolItem> itemsInGroup = entry.getValue();

        // 提取当前分组中的所有 item 的 ID
        List<Long> idsInGroup = itemsInGroup.stream()
                                            .map(DataPoolItem::getId)
                                            .collect(Collectors.toList());

        // 确保当前分组中有 ID 才执行数据库操作
        if (!idsInGroup.isEmpty()) {
            // 调用 mapper 方法，传入当前分组的 ID 列表、状态和对应的 deviceId
            dataPoolItemMapper.updateDataPoolItemsStatus(idsInGroup, status, currentDeviceId);
        }
    }
}

    @Override
    public void updateItemsStatus(List<DataPoolItem> itemList, String status) {
     // 提取当前分组中的所有 item 的 ID
        List<Long> idsInGroup = itemList.stream()
                                            .map(DataPoolItem::getId)
                                            .toList();

        dataPoolItemMapper.updateItemsStatus(idsInGroup, status);
    }

    /**
     * 锁定数据项（设置状态为PRINTING并设置deviceId）
     * 
     * @param id 数据项ID
     * @param deviceId 设备ID
     * @return 是否成功
     */
    @Override
    public boolean lockDataPoolItem(Long id, String deviceId) {
        if (StringUtils.isEmpty(deviceId)) {
            return false;
        }
        return dataPoolItemMapper.lockDataPoolItem(id, deviceId) > 0;
    }

    /**
     * 批量锁定数据项
     * 
     * @param ids 数据项ID列表
     * @param deviceId 设备ID
     * @return 成功锁定的数量
     */
    @Override
    public int batchLockDataPoolItems(List<Long> ids, String deviceId) {
        if (ids == null || ids.isEmpty() || StringUtils.isEmpty(deviceId)) {
            return 0;
        }
        return dataPoolItemMapper.batchLockDataPoolItems(ids, deviceId);
    }

    /**
     * 更新数据项状态
     * 
     * @param id 数据项ID
     * @param status 新状态
     * @param printCount 打印次数
     * @return 是否成功
     */
    @Override
    public boolean updateDataPoolItemStatus(Long id, String status, Integer printCount) {
        // 验证状态是否有效
        if (!ItemStatus.isValidStatus(status)) {
            return false;
        }
        return dataPoolItemMapper.updateDataPoolItemStatus(id, status, printCount) > 0;
    }

    /**
     * 标记打印成功
     * 
     * @param id 数据项ID
     * @return 是否成功
     */
    @Override
    public boolean markAsPrinted(Long id) {
        // 获取当前数据项
        DataPoolItem item = selectDataPoolItemById(id);
        if (item == null) {
            return false;
        }
        
        // 增加打印次数并标记为成功
        Integer newPrintCount = (item.getPrintCount() == null ? 0 : item.getPrintCount()) + 1;
        return updateDataPoolItemStatus(id, ItemStatus.PRINTED.getCode(), newPrintCount);
    }

    /**
     * 标记打印失败（增加打印次数）
     * 
     * @param id 数据项ID
     * @return 是否成功
     */
    @Override
    public boolean markAsFailed(Long id) {
        // 获取当前数据项
        DataPoolItem item = selectDataPoolItemById(id);
        if (item == null) {
            return false;
        }
        
        // 增加打印次数并标记为失败
        Integer newPrintCount = (item.getPrintCount() == null ? 0 : item.getPrintCount()) + 1;
        return updateDataPoolItemStatus(id, ItemStatus.FAILED.getCode(), newPrintCount);
    }

    /**
     * 释放锁定（清空deviceId，状态改为PENDING）
     * 
     * @param id 数据项ID
     * @return 是否成功
     */
    @Override
    public boolean releaseLock(Long id) {
        return dataPoolItemMapper.releaseLock(id) > 0;
    }

    /**
     * 根据设备ID释放锁定
     * 
     * @param deviceId 设备ID
     * @return 影响的数据项数量
     */
    @Override
    public int releaseLockByLockId(String deviceId) {
        if (StringUtils.isEmpty(deviceId)) {
            return 0;
        }
        return dataPoolItemMapper.releaseLockByLockId(deviceId);
    }

    /**
     * 新增数据池热数据
     * 
     * @param dataPoolItem 数据池热数据
     * @return 结果
     */
    @Override
    public int insertDataPoolItem(DataPoolItem dataPoolItem) {
        // 设置默认值
        if (StringUtils.isEmpty(dataPoolItem.getStatus())) {
            dataPoolItem.setStatus(ItemStatus.PENDING.getCode());
        }
        if (dataPoolItem.getPrintCount() == null) {
            dataPoolItem.setPrintCount(0);
        }
        if (dataPoolItem.getReceivedTime() == null) {
            dataPoolItem.setReceivedTime(DateUtils.getNowDate());
        }
        if (StringUtils.isEmpty(dataPoolItem.getDelFlag())) {
            dataPoolItem.setDelFlag("0");
        }
        
        dataPoolItem.setCreateTime(DateUtils.getNowDate());
        dataPoolItem.setUpdateTime(DateUtils.getNowDate());
        
        return dataPoolItemMapper.insertDataPoolItem(dataPoolItem);
    }

    /**
     * 批量新增数据池热数据
     * 
     * @param dataPoolItems 数据池热数据列表
     * @return 结果
     */
    @Override
    public int batchInsertDataPoolItems(List<DataPoolItem> dataPoolItems) {
        if (dataPoolItems == null || dataPoolItems.isEmpty()) {
            return 0;
        }
        
        Date now = new Date();
        for (DataPoolItem item : dataPoolItems) {
            // 设置默认值
            if (StringUtils.isEmpty(item.getStatus())) {
                item.setStatus(ItemStatus.PENDING.getCode());
            }
            if (item.getPrintCount() == null) {
                item.setPrintCount(0);
            }
            if (item.getReceivedTime() == null) {
                item.setReceivedTime(now);
            }
            if (StringUtils.isEmpty(item.getDelFlag())) {
                item.setDelFlag("0");
            }
            item.setCreateTime(now);
            item.setUpdateTime(now);
        }
        
        return dataPoolItemMapper.batchInsertDataPoolItems(dataPoolItems);
    }

    /**
     * 添加新的数据项（从数据源获取后调用）
     * 
     * @param poolId 数据池ID
     * @param itemData 数据内容
     * @return 新增的数据项
     */
    @Override
    public DataPoolItem addDataItem(Long poolId, String itemData) {
        if (poolId == null || StringUtils.isEmpty(itemData)) {
            return null;
        }
        
        DataPoolItem item = new DataPoolItem();
        item.setPoolId(poolId);
        item.setItemData(itemData);
        
        if (insertDataPoolItem(item) > 0) {
            return item;
        }
        return null;
    }

    /**
     * 批量添加数据项
     * 
     * @param poolId 数据池ID
     * @param itemDataList 数据内容列表
     * @return 新增的数据项数量
     */
    @Override
    public int batchAddDataItems(Long poolId, List<String> itemDataList) {
        if (poolId == null || itemDataList == null || itemDataList.isEmpty()) {
            return 0;
        }
        
        List<DataPoolItem> items = new ArrayList<>();
        for (String itemData : itemDataList) {
            if (StringUtils.isNotEmpty(itemData)) {
                DataPoolItem item = new DataPoolItem();
                item.setPoolId(poolId);
                item.setItemData(itemData);
                items.add(item);
            }
        }
        
        return batchInsertDataPoolItems(items);
    }

    /**
     * 修改数据池热数据
     * 
     * @param dataPoolItem 数据池热数据
     * @return 结果
     */
    @Override
    public int updateDataPoolItem(DataPoolItem dataPoolItem) {
        dataPoolItem.setUpdateTime(DateUtils.getNowDate());
        return dataPoolItemMapper.updateDataPoolItem(dataPoolItem);
    }

    /**
     * 批量删除数据池热数据
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    @Override
    public int deleteDataPoolItemByIds(Long[] ids) {
        return dataPoolItemMapper.deleteDataPoolItemByIds(ids);
    }

    /**
     * 删除数据池热数据信息
     * 
     * @param id 数据池热数据主键
     * @return 结果
     */
    @Override
    public int deleteDataPoolItemById(Long id) {
        return dataPoolItemMapper.deleteDataPoolItemById(id);
    }

    /**
     * 根据数据池ID删除热数据
     * 
     * @param poolId 数据池ID
     * @return 结果
     */
    @Override
    public int deleteDataPoolItemByPoolId(Long poolId) {
        return dataPoolItemMapper.deleteDataPoolItemByPoolId(poolId);
    }

    /**
     * 统计各状态的数据量
     * 
     * @param poolId 数据池ID（可选）
     * @return 状态统计结果
     */
    @Override
    public List<Map<String, Object>> countByStatus(Long poolId) {
        return dataPoolItemMapper.countByStatus(poolId);
    }

    @Override
    public int countByStatus(Long poolId, String status) {
        return  dataPoolItemMapper.countIntStatus(poolId, status);
    }

    /**
     * 获取数据池统计信息
     * 
     * @param poolId 数据池ID
     * @return 统计信息
     */
    @Override
    public Map<String, Object> getDataPoolStatistics(Long poolId) {
        List<Map<String, Object>> statusCounts = countByStatus(poolId);
        
        Map<String, Object> statistics = new HashMap<>();
        int totalCount = 0;
        int pendingCount = 0;
        int printingCount = 0;
        int printedCount = 0;
        int failedCount = 0;
        
        for (Map<String, Object> statusCount : statusCounts) {
            String status = (String) statusCount.get("status");
            Integer count = (Integer) statusCount.get("count");
            
            totalCount += count;
            
            switch (status) {
                case "PENDING":
                    pendingCount = count;
                    break;
                case "PRINTING":
                    printingCount = count;
                    break;
                case "PRINTED":
                    printedCount = count;
                    break;
                case "FAILED":
                    failedCount = count;
                    break;
            }
        }
        
        statistics.put("totalCount", totalCount);
        statistics.put("pendingCount", pendingCount);
        statistics.put("printingCount", printingCount);
        statistics.put("printedCount", printedCount);
        statistics.put("failedCount", failedCount);
        statistics.put("poolId", poolId);
        
        return statistics;
    }

    /**
     * 清理已打印成功的数据（可选功能）
     * 
     * @param poolId 数据池ID（可选）
     * @param beforeTime 时间限制（清理此时间之前的数据）
     * @return 清理的数据量
     */
    @Override
    public int cleanPrintedData(Long poolId, Date beforeTime) {
        return dataPoolItemMapper.cleanPrintedData(poolId, beforeTime);
    }

    /**
     * 重置失败的数据项（将FAILED状态改为PENDING，清空deviceId）
     * 
     * @param poolId 数据池ID（可选）
     * @return 重置的数据项数量
     */
    @Override
    public int resetFailedItems(Long poolId) {
        // 查询失败的数据项
        DataPoolItem queryItem = new DataPoolItem();
        queryItem.setPoolId(poolId);
        queryItem.setStatus(ItemStatus.FAILED.getCode());
        
        List<DataPoolItem> failedItems = selectDataPoolItemList(queryItem);
        
        int resetCount = 0;
        for (DataPoolItem entity : failedItems) {
            // 重置状态为PENDING，清空deviceId
            entity.setStatus(ItemStatus.PENDING.getCode());
            entity.setDeviceId(null);
            if (updateDataPoolItem(entity) > 0) {
                resetCount++;
            }
        }
        
        return resetCount;
    }

    /**
     * 获取打印队列信息
     * 
     * @param poolId 数据池ID（可选）
     * @return 队列信息
     */
    @Override
    public Map<String, Object> getPrintQueueInfo(Long poolId) {
        Map<String, Object> queueInfo = getDataPoolStatistics(poolId);
        
        // 获取最早的待打印数据
        List<DataPoolItem> pendingItems = selectPendingItems(poolId, 1);
        if (!pendingItems.isEmpty()) {
            queueInfo.put("earliestPendingTime", pendingItems.get(0).getReceivedTime());
        }
        
        // 获取正在打印的数据项（按锁定ID分组）
        DataPoolItem queryItem = new DataPoolItem();
        queryItem.setPoolId(poolId);
        queryItem.setStatus(ItemStatus.PRINTING.getCode());
        
        List<DataPoolItem> printingItems = selectDataPoolItemList(queryItem);
        Set<String> activeDeviceIds = new HashSet<>();
        for (DataPoolItem item : printingItems) {
            if (StringUtils.isNotEmpty(item.getDeviceId())) {
                activeDeviceIds.add(item.getDeviceId());
            }
        }
        queueInfo.put("activeDeviceIds", activeDeviceIds);
        queueInfo.put("activePrinterCount", activeDeviceIds.size());
        
        return queueInfo;
    }

    @Override
    public Long getCompletedCount(String deviceId, Long poolId, String code) {
        return dataPoolItemMapper.getCompletedCount(deviceId, poolId, code);
    }

    @Override
    public void updateToPendingItem(Long poolId) {
        dataPoolItemMapper.updateToPendingItem(poolId);
    }

    @Override
    public int countByNotPending(Long id) {
      return  dataPoolItemMapper.countByNotPending(id);
    }
}
