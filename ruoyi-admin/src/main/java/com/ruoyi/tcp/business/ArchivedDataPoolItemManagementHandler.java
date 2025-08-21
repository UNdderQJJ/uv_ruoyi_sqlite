package com.ruoyi.tcp.business;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.ArchivedDataPoolItem;
import com.ruoyi.business.service.IArchivedDataPoolItemService;
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
 * 归档数据池项目管理处理器
 * 处理归档数据相关的TCP请求
 * 
 * @author ruoyi
 */
@Component
public class ArchivedDataPoolItemManagementHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ArchivedDataPoolItemManagementHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IArchivedDataPoolItemService archivedDataPoolItemService;

    /**
     * 处理归档数据相关请求
     * 
     * @param path 请求路径
     * @param body 请求体
     * @return TCP响应
     */
    public TcpResponse handleArchivedDataPoolItemRequest(String path, String body) {
        try {
            switch (path) {
                case "/business/archivedDataPoolItem/list":
                    return listArchivedDataPoolItems(body);
                case "/business/archivedDataPoolItem/get":
                    return getArchivedDataPoolItem(body);
                case "/business/archivedDataPoolItem/update":
                    return updateArchivedDataPoolItem(body);
                case "/business/archivedDataPoolItem/delete":
                    return deleteArchivedDataPoolItem(body);
                case "/business/archivedDataPoolItem/getByPoolId":
                    return getArchivedDataPoolItemByPoolId(body);
                case "/business/archivedDataPoolItem/getByTimeRange":
                    return getArchivedDataPoolItemByTimeRange(body);
                case "/business/archivedDataPoolItem/getByStatus":
                    return getArchivedDataPoolItemByStatus(body);
                case "/business/archivedDataPoolItem/getByDeviceId":
                    return getArchivedDataPoolItemByDeviceId(body);
                case "/business/archivedDataPoolItem/getByVerificationStatus":
                    return getArchivedDataPoolItemByVerificationStatus(body);
                case "/business/archivedDataPoolItem/statistics":
                    return getArchivedDataStatistics(body);
                case "/business/archivedDataPoolItem/cleanBeforeTime":
                    return cleanArchivedDataBeforeTime(body);
                case "/business/archivedDataPoolItem/archiveByPrintStatus":
                    return archiveByPrintStatus(body);
                case "/business/archivedDataPoolItem/updateVerificationInfo":
                    return updateVerificationInfo(body);
                case "/business/archivedDataPoolItem/updateVerificationStatusByDeviceId":
                    return updateVerificationStatusByDeviceId(body);
                case "/business/archivedDataPoolItem/exportData":
                    return getExportData(body);
                default:
                    log.warn("[ArchivedDataPoolItemManagement] 未知的归档数据操作路径: {}", path);
                    return TcpResponse.error("未知的归档数据操作: " + path);
            }
        } catch (Exception e) {
            log.error("[ArchivedDataPoolItemManagement] 处理归档数据请求时发生异常: {}", path, e);
            return TcpResponse.error("归档数据操作失败: " + e.getMessage());
        }
    }

    /**
     * 查询归档数据列表
     */
    private TcpResponse listArchivedDataPoolItems(String body) throws JsonProcessingException {
        ArchivedDataPoolItem queryItem = objectMapper.readValue(body, ArchivedDataPoolItem.class);
        
        List<ArchivedDataPoolItem> items = archivedDataPoolItemService.selectArchivedDataPoolItemList(queryItem);
        
        log.info("[ArchivedDataPoolItemManagement] 查询归档数据列表成功，数量: {}", items.size());
        return TcpResponse.success("查询归档数据列表成功", items);
    }

    /**
     * 获取单个归档数据
     */
    private TcpResponse getArchivedDataPoolItem(String body) throws JsonProcessingException {
        ArchivedDataPoolItem queryItem = objectMapper.readValue(body, ArchivedDataPoolItem.class);
        
        if (queryItem.getId() == null) {
            return TcpResponse.error("缺少必要参数：id");
        }
        
        ArchivedDataPoolItem item = archivedDataPoolItemService.selectArchivedDataPoolItemById(queryItem.getId());
        
        if (item != null) {
            log.info("[ArchivedDataPoolItemManagement] 获取归档数据成功，ID: {}", queryItem.getId());
            return TcpResponse.success("获取归档数据成功", item);
        } else {
            log.warn("[ArchivedDataPoolItemManagement] 归档数据不存在，ID: {}", queryItem.getId());
            return TcpResponse.error("归档数据不存在");
        }
    }

    /**
     * 更新归档数据
     */
    private TcpResponse updateArchivedDataPoolItem(String body) throws JsonProcessingException {
        ArchivedDataPoolItem item = objectMapper.readValue(body, ArchivedDataPoolItem.class);
        
        int result = archivedDataPoolItemService.updateArchivedDataPoolItem(item);
        
        if (result > 0) {
            log.info("[ArchivedDataPoolItemManagement] 更新归档数据成功，ID: {}", item.getId());
            return TcpResponse.success("更新归档数据成功");
        } else {
            log.error("[ArchivedDataPoolItemManagement] 更新归档数据失败，ID: {}", item.getId());
            return TcpResponse.error("更新归档数据失败");
        }
    }

    /**
     * 删除归档数据
     */
    private TcpResponse deleteArchivedDataPoolItem(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        if (params.get("id") != null) {
            // 单个删除
            Long id = Long.valueOf(params.get("id").toString());
            int result = archivedDataPoolItemService.deleteArchivedDataPoolItemById(id);
            
            if (result > 0) {
                log.info("[ArchivedDataPoolItemManagement] 删除归档数据成功，ID: {}", id);
                return TcpResponse.success("删除归档数据成功");
            } else {
                log.error("[ArchivedDataPoolItemManagement] 删除归档数据失败，ID: {}", id);
                return TcpResponse.error("删除归档数据失败");
            }
        } else if (params.get("ids") != null) {
            // 批量删除
            @SuppressWarnings("unchecked")
            List<Long> idList = (List<Long>) params.get("ids");
            Long[] ids = idList.toArray(new Long[0]);
            
            int result = archivedDataPoolItemService.deleteArchivedDataPoolItemByIds(ids);
            
            log.info("[ArchivedDataPoolItemManagement] 批量删除归档数据成功，数量: {}", result);
            return TcpResponse.success("批量删除归档数据成功");
        } else {
            return TcpResponse.error("缺少必要参数");
        }
    }

    /**
     * 根据数据池ID查询归档数据
     */
    private TcpResponse getArchivedDataPoolItemByPoolId(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long poolId = Long.valueOf(params.get("poolId").toString());
        List<ArchivedDataPoolItem> items = archivedDataPoolItemService.selectArchivedDataPoolItemByPoolId(poolId);
        
        log.info("[ArchivedDataPoolItemManagement] 根据数据池ID查询归档数据成功，数量: {}", items.size());
        return TcpResponse.success("查询归档数据成功", items);
    }

    /**
     * 根据时间范围查询归档数据
     */
    private TcpResponse getArchivedDataPoolItemByTimeRange(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long poolId = null;
        if (params.get("poolId") != null) {
            poolId = Long.valueOf(params.get("poolId").toString());
        }
        
        String startTimeStr = (String) params.get("startTime");
        String endTimeStr = (String) params.get("endTime");
        
        // 这里可以根据需要解析时间格式
        // Date startTime = DateUtils.parseDate(startTimeStr);
        // Date endTime = DateUtils.parseDate(endTimeStr);
        
        List<ArchivedDataPoolItem> items = archivedDataPoolItemService.selectArchivedDataPoolItemByTimeRange(poolId, null, null);
        
        log.info("[ArchivedDataPoolItemManagement] 根据时间范围查询归档数据成功，数量: {}", items.size());
        return TcpResponse.success("查询归档数据成功", items);
    }

    /**
     * 根据状态查询归档数据
     */
    private TcpResponse getArchivedDataPoolItemByStatus(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long poolId = null;
        if (params.get("poolId") != null) {
            poolId = Long.valueOf(params.get("poolId").toString());
        }
        
        String finalStatus = params.get("finalStatus").toString();
        List<ArchivedDataPoolItem> items = archivedDataPoolItemService.selectArchivedDataPoolItemByStatus(poolId, finalStatus);
        
        log.info("[ArchivedDataPoolItemManagement] 根据状态查询归档数据成功，数量: {}", items.size());
        return TcpResponse.success("查询归档数据成功", items);
    }

    /**
     * 根据设备ID查询归档数据
     */
    private TcpResponse getArchivedDataPoolItemByDeviceId(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        String deviceId = params.get("deviceId").toString();
        List<ArchivedDataPoolItem> items = archivedDataPoolItemService.selectArchivedDataPoolItemByDeviceId(deviceId);
        
        log.info("[ArchivedDataPoolItemManagement] 根据设备ID查询归档数据成功，数量: {}", items.size());
        return TcpResponse.success("查询归档数据成功", items);
    }

    /**
     * 根据校验状态查询归档数据
     */
    private TcpResponse getArchivedDataPoolItemByVerificationStatus(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long poolId = null;
        if (params.get("poolId") != null) {
            poolId = Long.valueOf(params.get("poolId").toString());
        }
        
        String verificationStatus = params.get("verificationStatus").toString();
        List<ArchivedDataPoolItem> items = archivedDataPoolItemService.selectArchivedDataPoolItemByVerificationStatus(poolId, verificationStatus);
        
        log.info("[ArchivedDataPoolItemManagement] 根据校验状态查询归档数据成功，数量: {}", items.size());
        return TcpResponse.success("查询归档数据成功", items);
    }

    /**
     * 获取归档数据统计信息
     */
    private TcpResponse getArchivedDataStatistics(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long poolId = null;
        if (params.get("poolId") != null) {
            poolId = Long.valueOf(params.get("poolId").toString());
        }
        
        Map<String, Object> statistics = archivedDataPoolItemService.getArchivedDataStatistics(poolId);
        
        log.info("[ArchivedDataPoolItemManagement] 获取归档数据统计信息成功");
        return TcpResponse.success("获取统计信息成功", statistics);
    }

    /**
     * 清理指定时间之前的归档数据
     */
    private TcpResponse cleanArchivedDataBeforeTime(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long poolId = null;
        if (params.get("poolId") != null) {
            poolId = Long.valueOf(params.get("poolId").toString());
        }
        
        String beforeTimeStr = (String) params.get("beforeTime");
        // 这里可以根据需要解析时间格式
        // Date beforeTime = DateUtils.parseDate(beforeTimeStr);
        
        int cleanedCount = archivedDataPoolItemService.cleanArchivedDataBeforeTime(poolId, null);
        
        Map<String, Object> result = new HashMap<>();
        result.put("cleanedCount", cleanedCount);
        result.put("poolId", poolId);
        
        log.info("[ArchivedDataPoolItemManagement] 清理归档数据成功，数量: {}", cleanedCount);
        return TcpResponse.success("清理归档数据成功", result);
    }

    /**
     * 根据打印状态归档数据
     */
    private TcpResponse archiveByPrintStatus(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long poolId = null;
        if (params.get("poolId") != null) {
            poolId = Long.valueOf(params.get("poolId").toString());
        }
        
        int archivedCount = archivedDataPoolItemService.archiveByPrintStatus(poolId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("archivedCount", archivedCount);
        result.put("poolId", poolId);
        
        log.info("[ArchivedDataPoolItemManagement] 根据打印状态归档数据成功，数量: {}", archivedCount);
        return TcpResponse.success("归档数据成功", result);
    }

    /**
     * 更新校验信息
     */
    private TcpResponse updateVerificationInfo(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long id = Long.valueOf(params.get("id").toString());
        String verificationData = (String) params.get("verificationData");
        String verificationStatus = params.get("verificationStatus").toString();
        
        boolean result = archivedDataPoolItemService.updateVerificationInfo(id, verificationData, verificationStatus);
        
        if (result) {
            log.info("[ArchivedDataPoolItemManagement] 更新校验信息成功，ID: {}", id);
            return TcpResponse.success("更新校验信息成功");
        } else {
            log.error("[ArchivedDataPoolItemManagement] 更新校验信息失败，ID: {}", id);
            return TcpResponse.error("更新校验信息失败");
        }
    }

    /**
     * 根据设备ID批量更新校验状态
     */
    private TcpResponse updateVerificationStatusByDeviceId(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        String deviceId = params.get("deviceId").toString();
        String verificationStatus = params.get("verificationStatus").toString();
        
        int updateCount = archivedDataPoolItemService.updateVerificationStatusByDeviceId(deviceId, verificationStatus);
        
        Map<String, Object> result = new HashMap<>();
        result.put("updateCount", updateCount);
        result.put("deviceId", deviceId);
        
        log.info("[ArchivedDataPoolItemManagement] 批量更新校验状态成功，数量: {}", updateCount);
        return TcpResponse.success("批量更新校验状态成功", result);
    }

    /**
     * 获取导出数据
     */
    private TcpResponse getExportData(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        Long poolId = null;
        if (params.get("poolId") != null) {
            poolId = Long.valueOf(params.get("poolId").toString());
        }
        
        String startTimeStr = (String) params.get("startTime");
        String endTimeStr = (String) params.get("endTime");
        
        // 这里可以根据需要解析时间格式
        // Date startTime = DateUtils.parseDate(startTimeStr);
        // Date endTime = DateUtils.parseDate(endTimeStr);
        
        List<ArchivedDataPoolItem> exportData = archivedDataPoolItemService.getExportData(poolId, null, null);
        
        log.info("[ArchivedDataPoolItemManagement] 获取导出数据成功，数量: {}", exportData.size());
        return TcpResponse.success("获取导出数据成功", exportData);
    }
}
