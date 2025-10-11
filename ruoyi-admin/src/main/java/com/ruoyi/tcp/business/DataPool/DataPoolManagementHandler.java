package com.ruoyi.tcp.business.DataPool;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.config.TaskStagingTransfer;
import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.business.domain.ArchivedDataPoolItem.ArchivedDataPoolItem;
import com.ruoyi.business.domain.config.TriggerConfig;
import com.ruoyi.business.domain.config.UDiskSourceConfig;
import com.ruoyi.business.enums.SourceType;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.service.DataPool.DataPoolConfigValidationService;
import com.ruoyi.business.service.DataPool.DataPoolSchedulerService;
import com.ruoyi.business.service.DataPool.IDataPoolService;
import com.ruoyi.business.service.DataPool.type.TcpClient.tcp.TcpServerManager;
import com.ruoyi.business.service.DataPool.type.WebSocket.WebSocketManager;
import com.ruoyi.business.service.DataPoolItem.IDataPoolItemService;
import com.ruoyi.business.service.ArchivedDataPoolItem.IArchivedDataPoolItemService;

import com.ruoyi.business.service.DataPool.type.UDisk.UDiskDataSchedulerService;
import com.ruoyi.business.service.DataPool.type.UDisk.UDiskFileReaderService;
import com.ruoyi.business.service.DataPool.type.TcpServer.tcp.TcpClientManager;
import com.ruoyi.business.service.DataPool.type.Http.HttpManager;
import com.ruoyi.business.service.DataPool.type.Mqtt.MqttManager;
import com.ruoyi.business.service.DataPool.DataSourceLifecycleService;
import com.ruoyi.common.core.TcpResponse;
import com.ruoyi.common.utils.StringUtils;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    
    @Resource
    private TcpServerManager tcpServerManager;
    
    @Resource
    private HttpManager httpManager;
    
    @Resource
    private MqttManager mqttManager;
    
    @Resource
    private WebSocketManager webSocketManager;
    
    @Resource
    private DataPoolSchedulerService dataPoolSchedulerService;

    @Resource
    private DataSourceLifecycleService dataSourceLifecycleService;

    @Resource
    private TaskStagingTransfer taskStagingTransfer;


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
                case "/business/dataPool/startDataSource":
                    return startDataSource(body);
                case "/business/dataPool/stopDataSource":
                    return stopDataSource(body);
                case "/business/dataPool/schedulerStatus":
                    return getSchedulerStatus(body);
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

        //刷新数据池待打印数量
        dataPoolService.refreshPendingCount();

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


        //判断数据源是否填写正确
        DataPoolConfigValidationService.ValidationResult validationResult =
                dataPoolConfigValidationService.validateDataPoolConfig(dataPool.getSourceType(),
                dataPool.getSourceConfigJson(),
                dataPool.getParsingRuleJson(),
                dataPool.getTriggerConfigJson(),
                dataPool.getDataFetchInterval());
        if (!validationResult.isValid()) {
            return TcpResponse.error("数据源配置无效: " + validationResult.getErrorMessage());
        }

        //创建数据源
        int result = dataPoolService.insertDataPool(dataPool);
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

        // 检查数据池当前状态
        DataPool currentDataPool = dataPoolService.selectDataPoolById(dataPool.getId());
        if (currentDataPool == null) {
            return TcpResponse.error("数据池不存在");
        }

        // 如果数据池正在运行，不允许修改时间配置
        if (PoolStatus.RUNNING.getCode().equals(currentDataPool.getStatus()) && 
            dataPool.getDataFetchInterval() != null && 
            !dataPool.getDataFetchInterval().equals(currentDataPool.getDataFetchInterval())) {
            return TcpResponse.error("数据池正在运行中，不允许修改数据获取间隔时间");
        }

        //判断数据源是否填写正确
        DataPoolConfigValidationService.ValidationResult validationResult =
                dataPoolConfigValidationService.validateDataPoolConfig(dataPool.getSourceType(),
                dataPool.getSourceConfigJson(),
                dataPool.getParsingRuleJson(),
                dataPool.getTriggerConfigJson(),
                dataPool.getDataFetchInterval());
        if (!validationResult.isValid()) {
            return TcpResponse.error("数据源配置无效: " + validationResult.getErrorMessage());
        }
        
        // 检查是否修改了阈值配置，如果修改了则清除pendingCount历史记录
        boolean thresholdChanged = isThresholdConfigChanged(currentDataPool, dataPool);
        if (thresholdChanged) {
            dataPoolSchedulerService.clearPendingCountHistory(dataPool.getId());
        }



         int result = dataPoolService.updateDataPool(dataPool);

        if (currentDataPool.getStatus().equals(PoolStatus.RUNNING.getCode())) {
            // 更新数据池配置后，刷新相关Provider的配置
            refreshProviderConfig(dataPool.getId(), dataPool.getSourceType());
        }


        if (result > 0) {
            return TcpResponse.success("更新数据池成功");
        } else {
            return TcpResponse.error("更新数据池失败");
        }
    }
    
    /**
     * 检查阈值配置是否发生变化
     */
    private boolean isThresholdConfigChanged(DataPool currentDataPool, DataPool newDataPool) {
        try {
            // 比较触发配置JSON字符串
            String currentTriggerConfigJson = currentDataPool.getTriggerConfigJson();
            String newTriggerConfigJson = newDataPool.getTriggerConfigJson();
            
            // 如果两个都为null，则认为没有变化
            if (currentTriggerConfigJson == null && newTriggerConfigJson == null) {
                return false;
            }
            
            // 如果其中一个为null，另一个不为null，则认为有变化
            if (currentTriggerConfigJson == null || newTriggerConfigJson == null) {
                return true;
            }
            
            // 比较JSON字符串内容
            if (!currentTriggerConfigJson.equals(newTriggerConfigJson)) {
                // 如果JSON字符串不同，进一步比较阈值
                try {
                    TriggerConfig currentTriggerConfig = JSON.parseObject(currentTriggerConfigJson, TriggerConfig.class);
                    TriggerConfig newTriggerConfig = JSON.parseObject(newTriggerConfigJson, TriggerConfig.class);
                    
                    if (currentTriggerConfig != null && newTriggerConfig != null) {
                        Integer currentThreshold = currentTriggerConfig.getThreshold();
                        Integer newThreshold = newTriggerConfig.getThreshold();
                        
                        // 比较阈值是否发生变化
                        if (currentThreshold == null && newThreshold == null) {
                            return false;
                        }
                        if (currentThreshold == null || newThreshold == null) {
                            return true;
                        }
                        return !currentThreshold.equals(newThreshold);
                    }
                } catch (Exception e) {
                    log.warn("[DataPoolManagement] 解析触发配置失败，使用字符串比较: {}", e.getMessage());
                }
                
                // 如果解析失败，使用字符串比较
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("[DataPoolManagement] 检查阈值配置变化时发生异常", e);
            return false; // 发生异常时保守处理，不清理历史记录
        }
    }
    
    /**
     * 刷新Provider配置
     */
    private void refreshProviderConfig(Long poolId, String sourceType) {
        try {
            switch (sourceType) {
                case "TCP_SERVER":
                    // 刷新TCP客户端Provider配置
                    tcpClientManager.getOrCreateProvider(poolId).reloadConfigs();
                    break;
                case "TCP_CLIENT":
                    // 刷新TCP服务端Provider配置
                    tcpServerManager.getOrCreateProvider(poolId).reloadConfigs();
                    break;
                case "HTTP":
                    // 刷新HTTP Provider配置
                    httpManager.getOrCreateProvider(poolId).reloadConfigs();
                    break;
                case "MQTT":
                    // 刷新MQTT Provider配置
                    mqttManager.getOrCreateProvider(poolId).reloadConfigs();
                    break;
                case "WEBSOCKET":
                    // 刷新WebSocket Provider配置
                    webSocketManager.getOrCreateProvider(poolId).reloadConfigs();
                    break;
                default:
                    log.debug("[DataPoolManagement] 未知的数据源类型，跳过配置刷新: {}", sourceType);
            }
            log.info("[DataPoolManagement] 已刷新数据池配置: poolId={}, sourceType={}", poolId, sourceType);
        } catch (Exception e) {
            log.warn("[DataPoolManagement] 刷新Provider配置失败: poolId={}, sourceType={}", poolId, sourceType, e);
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
                dataPool.getTriggerConfigJson(),
                dataPool.getDataFetchInterval());
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


       return TcpResponse.success("更新数据池成功");
    }

    /**
     * 删除数据池：改为分批归档 + 批量软删 + 异步分批真删
     */
    private TcpResponse deleteDataPool(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());

        DataPool dataPool = dataPoolService.selectDataPoolById(id);

        //正在运行的数据池不允许删除
        if (PoolStatus.RUNNING.getCode().equals(dataPool.getStatus())) {
            return TcpResponse.error("数据池正在运行中，不允许删除");
        }
        
        int result = dataPoolService.deleteDataPoolById(id);
        
        if (result > 0) {
            // 清理调度器中的定时任务
            dataPoolSchedulerService.cleanupExecuteTimeRecord(id);
            try {
                // 1) 分批归档（INSERT…SELECT + LIMIT 循环），并在每批后分批软删，确保数量递减
                int totalArchived = 0;
                int batchSize = 2000;
                while (true) {
                    int archived = archivedDataPoolItemService.insertFromDataPoolByPoolIdLimit(id, batchSize);
                    if (archived > 0) {
                        // 紧接着对源表进行同批量级的分批软删，避免下一轮仍选到相同数据
                        dataPoolItemService.softDeleteByPoolIdLimit(id, archived);
                        totalArchived += archived;
                    }
                    if (archived < batchSize) {
                        break;
                    }
                }

                // 2) 兜底软删剩余未被标记的记录（极少量）
                int softDeleted = dataPoolItemService.softDeleteByPoolId(id);

                // 3) 异步分批真删（后台执行，不阻塞响应）
                asyncHardDelete(id, 2000);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("dataPoolId", id);
                responseData.put("dataPoolName", dataPool.getPoolName());
                responseData.put("archivedItemsCount", totalArchived);
                responseData.put("softDeletedCount", softDeleted);
                responseData.put("message", "删除数据池已受理（已分批归档与软删，后台正在清理）");
                return TcpResponse.success("删除数据池成功（后台继续清理）", responseData);
            } catch (Exception e) {
                log.error("[DataPoolManagement] 删除数据池 {} 清理过程异常", id, e);
                return TcpResponse.success("删除数据池成功，但清理过程中出现异常，请检查日志", new HashMap<>());
            }
        } else {
            return TcpResponse.error("删除数据池失败");
        }
    }

    @Async
    protected void asyncHardDelete(Long poolId, int batchSize) {
        try {
            taskStagingTransfer.hardDeleteBatch(poolId, batchSize);
        } catch (Exception e) {
            log.warn("[DataPoolManagement] 后台硬删失败 poolId={}", poolId, e);
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
            // 不直接启动定时任务，等待连接成功事件触发
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
            // 停止数据池的定时任务
            dataPoolSchedulerService.stopDataPoolScheduler(id);
            //更新任务
            dataPoolService.updateDataPendingCount(id);
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
     * 启动数据源
     * 根据数据源类型启动对应的Provider或服务
     */
    private TcpResponse startDataSource(String body) throws JsonProcessingException {
        DataPool dataPoolRtsp = objectMapper.readValue(body, DataPool.class);
        Long poolId = dataPoolRtsp.getId();
        try {
            String result = dataSourceLifecycleService.startDataSource(poolId);
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("[DataPoolManagement] 启动数据源失败: poolId={}", poolId, e);
            dataPoolService.updateDataPoolStatus(poolId, PoolStatus.ERROR.getCode());
            return TcpResponse.error("启动数据源失败: " + e.getMessage());
        }
    }

    /**
     * 停止数据源
     * 根据数据源类型停止对应的Provider或服务
     */
    private TcpResponse stopDataSource(String body) throws JsonProcessingException {
        DataPool dataPoolRtsp = objectMapper.readValue(body, DataPool.class);
        Long poolId = dataPoolRtsp.getId();
        try {
            String result = dataSourceLifecycleService.stopDataSource(poolId);
            return TcpResponse.success(result);
        } catch (Exception e) {
            log.error("[DataPoolManagement] 停止数据源失败: poolId={}", poolId, e);
            return TcpResponse.error("停止数据源失败: " + e.getMessage());
        }
    }

    /**
     * 获取调度器状态
     */
    private TcpResponse getSchedulerStatus(String body) throws JsonProcessingException {
        try {
            var result = new HashMap<String, Object>();
            result.put("activeTaskCount", dataPoolSchedulerService.getActiveTaskCount());
            result.put("activePoolIds", dataPoolSchedulerService.getActivePoolIds());
            
            return TcpResponse.success("获取调度器状态成功", result);
        } catch (Exception e) {
            log.error("[DataPoolManagement] 获取调度器状态失败", e);
            return TcpResponse.error("获取调度器状态失败: " + e.getMessage());
        }
    }
}
