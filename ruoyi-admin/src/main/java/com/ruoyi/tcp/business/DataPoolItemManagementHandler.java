package com.ruoyi.tcp.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DataPoolItem;
import com.ruoyi.business.service.IDataPoolItemService;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据池热数据管理处理器
 * 处理热数据相关的TCP请求
 * 
 * @author ruoyi
 */
@Component
public class DataPoolItemManagementHandler {
    
    private static final Logger log = LoggerFactory.getLogger(DataPoolItemManagementHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IDataPoolItemService dataPoolItemService;

    /**
     * 处理热数据相关请求
     * 
     * @param path 请求路径
     * @param body 请求体
     * @return TCP响应
     */
    public TcpResponse handleDataPoolItemRequest(String path, String body) {
        try {
            switch (path) {
                case "/business/dataPoolItem/list":
                    return listDataPoolItems(body);
                case "/business/dataPoolItem/get":
                    return getDataPoolItem(body);
                case "/business/dataPoolItem/add":
                    return addDataPoolItem(body);
                case "/business/dataPoolItem/batchAdd":
                    return batchAddDataPoolItems(body);
                case "/business/dataPoolItem/update":
                    return updateDataPoolItem(body);
                case "/business/dataPoolItem/delete":
                    return deleteDataPoolItem(body);
                case "/business/dataPoolItem/lock":
                    return lockDataPoolItem(body);
                case "/business/dataPoolItem/batchLock":
                    return batchLockDataPoolItems(body);
                case "/business/dataPoolItem/markPrinted":
                    return markAsPrinted(body);
                case "/business/dataPoolItem/markFailed":
                    return markAsFailed(body);
                case "/business/dataPoolItem/releaseLock":
                    return releaseLock(body);
                case "/business/dataPoolItem/releaseLockByLockId":
                    return releaseLockByLockId(body);
                case "/business/dataPoolItem/getPending":
                    return getPendingItems(body);
                case "/business/dataPoolItem/statistics":
                    return getStatistics(body);
                case "/business/dataPoolItem/queueInfo":
                    return getQueueInfo(body);
                case "/business/dataPoolItem/resetFailed":
                    return resetFailedItems(body);
                case "/business/dataPoolItem/cleanPrinted":
                    return cleanPrintedData(body);
                default:
                    log.warn("[DataPoolItemManagement] 未知的热数据操作路径: {}", path);
                    return TcpResponse.error("未知的热数据操作: " + path);
            }
        } catch (Exception e) {
            log.error("[DataPoolItemManagement] 处理热数据请求时发生异常: {}", path, e);
            return TcpResponse.error("热数据操作失败: " + e.getMessage());
        }
    }

    /**
     * 查询热数据列表
     */
    private TcpResponse listDataPoolItems(String body) throws JsonProcessingException {
        DataPoolItem queryItem = objectMapper.readValue(body, DataPoolItem.class);
        
        List<DataPoolItem> items = dataPoolItemService.selectDataPoolItemList(queryItem);
        
        log.info("[DataPoolItemManagement] 查询热数据列表成功，数量: {}", items.size());
        return TcpResponse.success("查询热数据列表成功", items);
    }

    /**
     * 获取单个热数据
     */
    private TcpResponse getDataPoolItem(String body) throws JsonProcessingException {
        DataPoolItem queryItem = objectMapper.readValue(body, DataPoolItem.class);
        
        if (queryItem.getId() == null) {
            return TcpResponse.error("缺少必要参数：id");
        }
        
        DataPoolItem item = dataPoolItemService.selectDataPoolItemById(queryItem.getId());
        
        if (item != null) {
            log.info("[DataPoolItemManagement] 获取热数据成功，ID: {}", queryItem.getId());
            return TcpResponse.success("获取热数据成功", item);
        } else {
            log.warn("[DataPoolItemManagement] 热数据不存在，ID: {}", queryItem.getId());
            return TcpResponse.error("热数据不存在");
        }
    }

    /**
     * 添加热数据
     */
    private TcpResponse addDataPoolItem(String body) throws JsonProcessingException {
        DataPoolItem newItem = objectMapper.readValue(body, DataPoolItem.class);
        
        if (newItem.getPoolId() == null || StringUtils.isEmpty(newItem.getItemData())) {
            return TcpResponse.error("缺少必要参数：poolId 或 itemData");
        }
        
        newItem = dataPoolItemService.addDataItem(newItem.getPoolId(), newItem.getItemData());
        
        if (newItem != null) {
            log.info("[DataPoolItemManagement] 添加热数据成功，ID: {}", newItem.getId());
            return TcpResponse.success("添加热数据成功", newItem);
        } else {
            log.error("[DataPoolItemManagement] 添加热数据失败");
            return TcpResponse.error("添加热数据失败");
        }
    }

    /**
     * 批量添加热数据
     */
    private TcpResponse batchAddDataPoolItems(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long poolId = Long.valueOf(params.get("poolId").toString());
        @SuppressWarnings("unchecked")
        List<String> itemDataList = (List<String>) params.get("itemDataList");
        
        int addedCount = dataPoolItemService.batchAddDataItems(poolId, itemDataList);
        
        Map<String, Object> result = new HashMap<>();
        result.put("addedCount", addedCount);
        result.put("poolId", poolId);
        
        log.info("[DataPoolItemManagement] 批量添加热数据成功，数量: {}", addedCount);
        return TcpResponse.success("批量添加热数据成功", result);
    }

    /**
     * 更新热数据
     */
    private TcpResponse updateDataPoolItem(String body) throws JsonProcessingException {
        DataPoolItem item = objectMapper.readValue(body, DataPoolItem.class);
        
        int result = dataPoolItemService.updateDataPoolItem(item);
        
        if (result > 0) {
            log.info("[DataPoolItemManagement] 更新热数据成功，ID: {}", item.getId());
            return TcpResponse.success("更新热数据成功");
        } else {
            log.error("[DataPoolItemManagement] 更新热数据失败，ID: {}", item.getId());
            return TcpResponse.error("更新热数据失败");
        }
    }

    /**
     * 删除热数据
     */
    private TcpResponse deleteDataPoolItem(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        if (params.get("id") != null) {
            // 单个删除
            Long id = Long.valueOf(params.get("id").toString());
            int result = dataPoolItemService.deleteDataPoolItemById(id);
            
            if (result > 0) {
                log.info("[DataPoolItemManagement] 删除热数据成功，ID: {}", id);
                return TcpResponse.success("删除热数据成功");
            } else {
                log.error("[DataPoolItemManagement] 删除热数据失败，ID: {}", id);
                return TcpResponse.error("删除热数据失败");
            }
        } else if (params.get("ids") != null) {
            // 批量删除
            @SuppressWarnings("unchecked")
            List<Long> idList = (List<Long>) params.get("ids");
            Long[] ids = idList.toArray(new Long[0]);
            
            int result = dataPoolItemService.deleteDataPoolItemByIds(ids);
            
            log.info("[DataPoolItemManagement] 批量删除热数据成功，数量: {}", result);
            return TcpResponse.success("批量删除热数据成功");
        } else {
            return TcpResponse.error("缺少必要参数");
        }
    }

    /**
     * 锁定热数据
     */
    private TcpResponse lockDataPoolItem(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long id = Long.valueOf(params.get("id").toString());
        String lockId = params.get("lockId").toString();
        
        boolean result = dataPoolItemService.lockDataPoolItem(id, lockId);
        
        if (result) {
            log.info("[DataPoolItemManagement] 锁定热数据成功，ID: {}, lockId: {}", id, lockId);
            return TcpResponse.success("锁定热数据成功");
        } else {
            log.error("[DataPoolItemManagement] 锁定热数据失败，ID: {}, lockId: {}", id, lockId);
            return TcpResponse.error("锁定热数据失败，可能已被其他设备锁定");
        }
    }

    /**
     * 批量锁定热数据
     */
    private TcpResponse batchLockDataPoolItems(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) params.get("ids");
        String lockId = params.get("lockId").toString();
        
        int lockedCount = dataPoolItemService.batchLockDataPoolItems(ids, lockId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("lockedCount", lockedCount);
        result.put("totalCount", ids.size());
        result.put("lockId", lockId);
        
        log.info("[DataPoolItemManagement] 批量锁定热数据，成功: {}, 总数: {}", lockedCount, ids.size());
        return TcpResponse.success("批量锁定热数据完成", result);
    }

    /**
     * 标记为打印成功
     */
    private TcpResponse markAsPrinted(String body) throws JsonProcessingException {
        DataPoolItem item = objectMapper.readValue(body, DataPoolItem.class);
        
        if (item.getId() == null) {
            return TcpResponse.error("缺少必要参数：id");
        }
        
        boolean result = dataPoolItemService.markAsPrinted(item.getId());
        
        if (result) {
            log.info("[DataPoolItemManagement] 标记打印成功，ID: {}", item.getId());
            return TcpResponse.success("标记打印成功");
        } else {
            log.error("[DataPoolItemManagement] 标记打印成功失败，ID: {}", item.getId());
            return TcpResponse.error("标记打印成功失败");
        }
    }

    /**
     * 标记为打印失败
     */
    private TcpResponse markAsFailed(String body) throws JsonProcessingException {
        DataPoolItem item = objectMapper.readValue(body, DataPoolItem.class);
        
        if (item.getId() == null) {
            return TcpResponse.error("缺少必要参数：id");
        }
        
        boolean result = dataPoolItemService.markAsFailed(item.getId());
        
        if (result) {
            log.info("[DataPoolItemManagement] 标记打印失败，ID: {}", item.getId());
            return TcpResponse.success("标记打印失败");
        } else {
            log.error("[DataPoolItemManagement] 标记打印失败失败，ID: {}", item.getId());
            return TcpResponse.error("标记打印失败失败");
        }
    }

    /**
     * 释放锁定
     */
    private TcpResponse releaseLock(String body) throws JsonProcessingException {
        DataPoolItem item = objectMapper.readValue(body, DataPoolItem.class);
        
        if (item.getId() == null) {
            return TcpResponse.error("缺少必要参数：id");
        }
        
        boolean result = dataPoolItemService.releaseLock(item.getId());
        
        if (result) {
            log.info("[DataPoolItemManagement] 释放锁定成功，ID: {}", item.getId());
            return TcpResponse.success("释放锁定成功");
        } else {
            log.error("[DataPoolItemManagement] 释放锁定失败，ID: {}", item.getId());
            return TcpResponse.error("释放锁定失败");
        }
    }

    /**
     * 根据锁定ID释放锁定
     */
    private TcpResponse releaseLockByLockId(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        String lockId = params.get("lockId").toString();
        int releasedCount = dataPoolItemService.releaseLockByLockId(lockId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("releasedCount", releasedCount);
        result.put("lockId", lockId);
        
        log.info("[DataPoolItemManagement] 根据锁定ID释放锁定，释放数量: {}, lockId: {}", releasedCount, lockId);
        return TcpResponse.success("释放锁定成功", result);
    }

    /**
     * 获取待打印数据
     */
    private TcpResponse getPendingItems(String body) throws JsonProcessingException {
        DataPoolItem queryItem = objectMapper.readValue(body, DataPoolItem.class);
        
        Long poolId = queryItem.getPoolId();
        Integer limit = queryItem.getPrintCount(); // 复用printCount字段作为limit
        
        List<DataPoolItem> items = dataPoolItemService.selectPendingItems(poolId, limit);
        
        log.info("[DataPoolItemManagement] 获取待打印数据成功，数量: {}", items.size());
        return TcpResponse.success("获取待打印数据成功", items);
    }

    /**
     * 获取统计信息
     */
    private TcpResponse getStatistics(String body) throws JsonProcessingException {
        DataPoolItem queryItem = objectMapper.readValue(body, DataPoolItem.class);
        
        Long poolId = queryItem.getPoolId();
        
        Map<String, Object> statistics = dataPoolItemService.getDataPoolStatistics(poolId);
        
        log.info("[DataPoolItemManagement] 获取统计信息成功");
        return TcpResponse.success("获取统计信息成功", statistics);
    }

    /**
     * 获取队列信息
     */
    private TcpResponse getQueueInfo(String body) throws JsonProcessingException {
        DataPoolItem queryItem = objectMapper.readValue(body, DataPoolItem.class);
        
        Long poolId = queryItem.getPoolId();
        
        Map<String, Object> queueInfo = dataPoolItemService.getPrintQueueInfo(poolId);
        
        log.info("[DataPoolItemManagement] 获取队列信息成功");
        return TcpResponse.success("获取队列信息成功", queueInfo);
    }

    /**
     * 重置失败数据
     */
    private TcpResponse resetFailedItems(String body) throws JsonProcessingException {
        DataPoolItem queryItem = objectMapper.readValue(body, DataPoolItem.class);
        
        Long poolId = queryItem.getPoolId();
        
        int resetCount = dataPoolItemService.resetFailedItems(poolId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("resetCount", resetCount);
        result.put("poolId", poolId);
        
        log.info("[DataPoolItemManagement] 重置失败数据成功，数量: {}", resetCount);
        return TcpResponse.success("重置失败数据成功", result);
    }

    /**
     * 清理已打印数据
     */
    private TcpResponse cleanPrintedData(String body) throws JsonProcessingException {
        DataPoolItem queryItem = objectMapper.readValue(body, DataPoolItem.class);
        
        Long poolId = queryItem.getPoolId();
        java.util.Date beforeTime = queryItem.getReceivedTime(); // 复用receivedTime字段作为beforeTime
        
        int cleanedCount = dataPoolItemService.cleanPrintedData(poolId, beforeTime);
        
        Map<String, Object> result = new HashMap<>();
        result.put("cleanedCount", cleanedCount);
        result.put("poolId", poolId);
        
        log.info("[DataPoolItemManagement] 清理已打印数据成功，数量: {}", cleanedCount);
        return TcpResponse.success("清理已打印数据成功", result);
    }
}
