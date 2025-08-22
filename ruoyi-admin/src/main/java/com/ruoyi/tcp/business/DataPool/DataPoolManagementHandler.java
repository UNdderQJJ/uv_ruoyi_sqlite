package com.ruoyi.tcp.business.DataPool;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DataPool;
import com.ruoyi.business.domain.DataPoolItem;
import com.ruoyi.business.domain.config.UDiskSourceConfig;
import com.ruoyi.business.enums.SourceType;
import com.ruoyi.business.service.DataPool.DataPoolConfigValidationService;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.ArchivedDataPoolItem.IArchivedDataPoolItemService;

import com.ruoyi.business.service.DataPool.UDisk.UDiskDataSchedulerService;
import com.ruoyi.business.service.DataPool.UDisk.UDiskFileReaderService;
import com.ruoyi.business.service.DataPool.TcpClient.tcp.TcpClientManager;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据池管理TCP处理器
 * 专门处理数据池相关的TCP请求
 */
@Component
public class DataPoolManagementHandler
{
    private static final Logger log = LoggerFactory.getLogger(DataPoolManagementHandler.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IDataPoolService dataPoolService;

    @Resource
    private DataPoolConfigValidationService dataPoolConfigValidationService;

    @Resource
    private UDiskDataSchedulerService uDiskDataSchedulerService;

    @Resource
    private UDiskFileReaderService uDiskFileReaderService;

    @Resource
    private IDataPoolItemService dataPoolItemService;

    @Resource
    private IArchivedDataPoolItemService archivedDataPoolItemService;

    @Resource
    private TcpClientManager tcpClientManager;



    /**
     * 处理数据池相关的TCP请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return TCP响应
     */
    public TcpResponse handleDataPoolRequest(String path, String body) {
        try {
            switch (path) {
                case "/business/dataPool/list":
                    return listDataPools(body);
                case "/business/dataPool/get":
                    return getDataPool(body);
                case "/business/dataPool/create":
                    return createDataPool(body);
                case "/business/dataPool/update":
                    return updateDataPool(body);
                case "/business/dataPool/delete":
                    return deleteDataPool(body);
                case "/business/dataPool/start":
                    return startDataPool(body);
                case "/business/dataPool/stop":
                    return stopDataPool(body);
                case "/business/dataPool/updatePoolItem":
                    return updatePoolItem(body);
                case "/business/dataPool/updateStatus":
                    return updateDataPoolStatus(body);
                case "/business/dataPool/updateCount":
                    return updateDataPoolCount(body);
                case "/business/dataPool/connect":
                    return connectDataPool(body);
                case "/business/dataPool/disconnect":
                    return disconnectDataPool(body);
                default:
                    log.warn("[DataPoolManagement] 未知的数据池操作路径: {}", path);
                    return TcpResponse.error("未知的数据池操作: " + path);
            }
        } catch (Exception e) {
            log.error("[DataPoolManagement] 处理数据池请求时发生异常: {}", path, e);
            return TcpResponse.error("数据池操作失败: " + e.getMessage());
        }
    }

    /**
     * 查询数据池列表
     */
    private TcpResponse listDataPools(String body) throws JsonProcessingException {
        DataPool queryParam = new DataPool();
        
        if (StringUtils.isNotEmpty(body)) {
            queryParam = objectMapper.readValue(body, DataPool.class);
        }

        List<DataPool> dataPools = dataPoolService.selectDataPoolList(queryParam);
        
        var result = new HashMap<String, Object>();
        result.put("dataPools", dataPools);
        result.put("total", dataPools.size());
        
        return TcpResponse.success("查询数据池列表成功", result);
    }

    /**
     * 获取数据池详情
     */
    private TcpResponse getDataPool(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        
        DataPool dataPool = dataPoolService.selectDataPoolById(id);
        
        if (dataPool == null) {
            return TcpResponse.error("数据池不存在");
        }

        var result = new HashMap<String, Object>();
        result.put("dataPool", dataPool);
        
        return TcpResponse.success("获取数据池详情成功", result);
    }

    /**
     * 创建数据池
     */
    private TcpResponse createDataPool(String body) throws JsonProcessingException {
        DataPool dataPool = objectMapper.readValue(body, DataPool.class);
        
        // 验证必填参数
        if (StringUtils.isEmpty(dataPool.getPoolName())) {
            return TcpResponse.error("缺少必要参数: 数据池名称");
        }
        if (StringUtils.isEmpty(dataPool.getSourceType())) {
            return TcpResponse.error("缺少必要参数: 数据源类型");
        }

        int result = dataPoolService.insertDataPool(dataPool);

        //判断数据源是否填写正确
        DataPoolConfigValidationService.ValidationResult validationResult =
                dataPoolConfigValidationService.validateDataPoolConfig(dataPool.getSourceType(),
                dataPool.getSourceConfigJson(),
                dataPool.getParsingRuleJson(),
                dataPool.getTriggerConfigJson());
        if (validationResult.isValid()) {
            // 只有在文件未读取完成时才拉取数据
            if (!"1".equals(dataPool.getFileReadCompleted())) {
                uDiskDataSchedulerService.manualTriggerDataReading(dataPool.getId(), null);
            }
        }
        
        if (result > 0) {
            var responseData = new HashMap<String, Object>();
            responseData.put("id", dataPool.getId());
            return TcpResponse.success("创建数据池成功", responseData);
        } else {
            return TcpResponse.error("创建数据池失败");
        }
    }

    /**
     * 更新数据池
     */
    private TcpResponse updateDataPool(String body) throws JsonProcessingException {
        DataPool dataPool = objectMapper.readValue(body, DataPool.class);
        
        if (dataPool.getId() == null) {
            return TcpResponse.error("缺少数据池ID参数");
        }

        //判断数据源是否填写正确
        DataPoolConfigValidationService.ValidationResult validationResult =
                dataPoolConfigValidationService.validateDataPoolConfig(dataPool.getSourceType(),
                dataPool.getSourceConfigJson(),
                dataPool.getParsingRuleJson(),
                dataPool.getTriggerConfigJson());
        if (!validationResult.isValid()) {
            return TcpResponse.error("数据源配置无效: " + validationResult.getErrorMessage());
        }
         int result = dataPoolService.updateDataPool(dataPool);

        // 只有在文件未读取完成时才拉取数据
        if (!"1".equals(dataPool.getFileReadCompleted())) {
            uDiskDataSchedulerService.manualTriggerDataReading(dataPool.getId(), null);
        }
        
        if (result > 0) {
            return TcpResponse.success("更新数据池成功");
        } else {
            return TcpResponse.error("更新数据池失败");
        }
    }

    /**
     * 更新数据池，拉取数据源数据
     */
    private TcpResponse updatePoolItem(String body) throws JsonProcessingException {
        DataPool dataPool11 = objectMapper.readValue(body, DataPool.class);

        DataPool dataPool = dataPoolService.selectDataPoolById(dataPool11.getId());
        //判断数据源是否填写正确
        DataPoolConfigValidationService.ValidationResult validationResult =
                dataPoolConfigValidationService.validateDataPoolConfig(dataPool.getSourceType(),
                dataPool.getSourceConfigJson(),
                dataPool.getParsingRuleJson(),
                dataPool.getTriggerConfigJson());
        if (!validationResult.isValid()) {
            return TcpResponse.error("数据源配置无效: " + validationResult.getErrorMessage());
        }
        //如果U盘数据池文件更新
        if(SourceType.U_DISK.getCode().equals(dataPool.getSourceType())){
             UDiskSourceConfig configNew = JSON.parseObject(dataPool11.getSourceConfigJson(), UDiskSourceConfig.class);
              UDiskSourceConfig configOld = JSON.parseObject(dataPool.getSourceConfigJson(), UDiskSourceConfig.class);
              if(!configNew.getFilePath().equals(configOld.getFilePath())){
                  uDiskFileReaderService.resetReadPosition(dataPool11.getId(), null);
              }
        }

          // 只有在文件未读取完成时才拉取数据
        if (!"1".equals(dataPool.getFileReadCompleted())) {
            uDiskDataSchedulerService.manualTriggerDataReading(dataPool.getId(), null);
        }

       return TcpResponse.success("更新数据池成功");
    }

    /**
     * 删除数据池
     */
    private TcpResponse deleteDataPool(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());

        DataPool dataPool = dataPoolService.selectDataPoolById(id);
        
        int result = dataPoolService.deleteDataPoolById(id);
        
        if (result > 0) {
            //将数据池中的数据删除，移动至归档表
            try {
                // 1. 查询该数据池下的所有热数据项
                List<DataPoolItem> dataPoolItems = dataPoolItemService.selectDataPoolItemByPoolId(id);
                
                if (!dataPoolItems.isEmpty()) {
                    log.info("[DataPoolManagement] 删除数据池 {} 时，发现 {} 条热数据项需要归档", id, dataPoolItems.size());
                    
                    // 2. 将所有热数据项归档（包括待打印、正在打印、已打印、失败的数据）
                    int archivedCount = archivedDataPoolItemService.batchArchiveDataPoolItems(dataPoolItems);
                    
                    // 3. 删除热数据项
                    int deletedCount = 0;
                    for (DataPoolItem item : dataPoolItems) {
                        if (dataPoolItemService.deleteDataPoolItemById(item.getId()) > 0) {
                            deletedCount++;
                        }
                    }
                    
                    log.info("[DataPoolManagement] 删除数据池 {} 时，成功归档 {} 条热数据项，删除 {} 条热数据项", id, archivedCount, deletedCount);
                } else {
                    log.info("[DataPoolManagement] 删除数据池 {} 时，没有发现需要归档的热数据项", id);
                }
                
                log.info("[DataPoolManagement] 删除数据池 {} 成功", id);
                
                // 构建响应数据，包含删除操作的详细信息
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("dataPoolId", id);
                responseData.put("dataPoolName", dataPool.getPoolName());
                responseData.put("archivedItemsCount", dataPoolItems.size());
                responseData.put("deletedItemsCount", dataPoolItems.size());
                responseData.put("message", "删除数据池成功");
                
                return TcpResponse.success("删除数据池成功", responseData);
                
            } catch (Exception e) {
                log.error("[DataPoolManagement] 删除数据池 {} 时归档数据失败", id, e);
                
                // 构建警告响应数据
                Map<String, Object> warningData = new HashMap<>();
                warningData.put("dataPoolId", id);
                warningData.put("dataPoolName", dataPool.getPoolName());
                warningData.put("warning", "数据归档过程中出现异常，请检查日志");
                warningData.put("errorMessage", e.getMessage());
                
                // 即使归档失败，数据池删除操作已经成功，返回成功但记录警告
                return TcpResponse.success("删除数据池成功，但数据归档过程中出现异常，请检查日志", warningData);
            }
        } else {
            return TcpResponse.error("删除数据池失败");
        }
    }

    /**
     * 启动数据池
     */
    private TcpResponse startDataPool(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        
        int result = dataPoolService.startDataPool(id);
        
        if (result > 0) {
            return TcpResponse.success("启动数据池成功");
        } else {
            return TcpResponse.error("启动数据池失败");
        }
    }

    /**
     * 停止数据池
     */
    private TcpResponse stopDataPool(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        
        int result = dataPoolService.stopDataPool(id);
        
        if (result > 0) {
            return TcpResponse.success("停止数据池成功");
        } else {
            return TcpResponse.error("停止数据池失败");
        }
    }

    /**
     * 更新数据池状态
     */
    private TcpResponse updateDataPoolStatus(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        String status = (String) params.get("status");
        
        int result = dataPoolService.updateDataPoolStatus(id, status);
        
        if (result > 0) {
            return TcpResponse.success("更新数据池状态成功");
        } else {
            return TcpResponse.error("更新数据池状态失败");
        }
    }

    /**
     * 更新数据池计数
     */
    private TcpResponse updateDataPoolCount(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());
        Long totalCount = params.containsKey("totalCount") ? Long.valueOf(params.get("totalCount").toString()) : null;
        Long pendingCount = params.containsKey("pendingCount") ? Long.valueOf(params.get("pendingCount").toString()) : null;
        
        int result = dataPoolService.updateDataPoolCount(id, totalCount, pendingCount);
        
        if (result > 0) {
            return TcpResponse.success("更新数据池计数成功");
        } else {
            return TcpResponse.error("更新数据池计数失败");
        }
    }

    /**
     * 手动连接TCP_CLIENT类型数据池
     */
    private TcpResponse connectDataPool(String body) throws JsonProcessingException {
        DataPool dataPool = objectMapper.readValue(body, DataPool.class);

        DataPool pool = dataPoolService.selectDataPoolById(dataPool.getId());
        if (pool == null) {
            return TcpResponse.error("数据池不存在");
        }
        if (!SourceType.TCP_CLIENT.getCode().equals(pool.getSourceType())) {
            return TcpResponse.error("数据池类型不是TCP_CLIENT");
        }
//        // 仅在运行状态下允许连接
//        if (!"RUNNING".equals(pool.getStatus())) {
//            return TcpResponse.error("数据池不在运行状态，无法连接");
//        }

        tcpClientManager.getOrCreateProvider(dataPool.getId()).ensureConnected();
        return TcpResponse.success("已触发连接");
    }

    /**
     * 手动断开TCP_CLIENT类型数据池
     */
    private TcpResponse disconnectDataPool(String body) throws JsonProcessingException {
        DataPool dataPool = objectMapper.readValue(body, DataPool.class);
        DataPool pool = dataPoolService.selectDataPoolById(dataPool.getId());
        if (pool == null) {
            return TcpResponse.error("数据池不存在");
        }
        if (!SourceType.TCP_CLIENT.getCode().equals(pool.getSourceType())) {
            return TcpResponse.error("数据池类型不是TCP_CLIENT");
        }

        // 移除并关闭客户端连接
        tcpClientManager.removeProvider(dataPool.getId());
        // 写回连接状态
        dataPoolService.updateConnectionState(dataPool.getId(), "DISCONNECTED");

        return TcpResponse.success("已断开连接");
    }
}
