package com.ruoyi.tcp.business.DataPool;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.domain.DataPoolItem.DataPoolItem;
import com.ruoyi.business.domain.config.UDiskSourceConfig;
import com.ruoyi.business.enums.SourceType;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.enums.ConnectionState;
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
                dataPool.getTriggerConfigJson(),
                dataPool.getDataFetchInterval());
        if (!validationResult.isValid()) {
            return TcpResponse.error("数据源配置无效: " + validationResult.getErrorMessage());
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
     * 删除数据池
     */
    private TcpResponse deleteDataPool(String body) throws JsonProcessingException {
        Map<String, Object> params = objectMapper.readValue(body, new TypeReference<>() {});
        Long id = Long.valueOf(params.get("id").toString());

        DataPool dataPool = dataPoolService.selectDataPoolById(id);
        
        int result = dataPoolService.deleteDataPoolById(id);
        
        if (result > 0) {
            // 清理调度器中的定时任务
            dataPoolSchedulerService.cleanupExecuteTimeRecord(id);
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
        String sourceType = dataPoolRtsp.getSourceType();

        DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
        if (dataPool == null) {
            return TcpResponse.error("数据池不存在");
        }

        // 检查数据池状态
        if (PoolStatus.RUNNING.getCode().equals(dataPool.getStatus())) {
            return TcpResponse.error("数据池已处于运行状态，无法重新启动数据源");
        }

        //更新数据池状态
        dataPoolService.updateDataPoolStatus(poolId, PoolStatus.RUNNING.getCode());

        try {
            // 不直接启动定时任务，等待连接成功事件触发

            String successMessage;
            switch (sourceType) {
                case "U_DISK":
                    // U盘类型：触发文件读取
                    successMessage = uDiskDataSchedulerService.manualTriggerDataReading(poolId, null);
                    break;
                case "TCP_SERVER":
                    // TCP服务端：建立连接
                    tcpClientManager.getOrCreateProvider(poolId).ensureConnected();
                    log.info("[DataPoolManagement] TCP服务端数据源启动成功");
                    successMessage = "TCP服务端数据源启动成功";
                    break;
                case "TCP_CLIENT":
                    // TCP客户端：启动监听
                    tcpServerManager.getOrCreateProvider(poolId);
                    log.info("[DataPoolManagement] TCP客户端数据源启动成功");
                    successMessage = "TCP客户端数据源启动成功";
                    break;
                case "HTTP":
                    // HTTP：创建Provider（HTTP是请求驱动的，无需特殊启动）
                    dataPoolService.updateConnectionState(poolId,ConnectionState.CONNECTING.getCode());
                    httpManager.getOrCreateProvider(poolId);
                    log.info("[DataPoolManagement] HTTP数据源启动成功");
                    successMessage = "HTTP数据源启动成功";
                    break;
                case "MQTT":
                    // MQTT：建立连接
                    mqttManager.getOrCreateProvider(poolId);
                    log.info("[DataPoolManagement] MQTT数据源启动成功");
                    successMessage = "MQTT数据源启动成功";
                    break;
                case "WEBSOCKET":
                    // WebSocket：建立连接
                    webSocketManager.getOrCreateProvider(poolId);
                    log.info("[DataPoolManagement] WebSocket数据源启动成功");
                    successMessage = "WebSocket数据源启动成功";
                    break;
                default:
                    successMessage = "不支持的数据源类型: " + sourceType;
                    break;
            }
            // 启动定时任务
            dataPoolSchedulerService.startDataPoolWithScheduler(dataPool.getId());

            return TcpResponse.success(successMessage);
        } catch (Exception e) {
            log.error("[DataPoolManagement] 启动数据源失败: poolId={}, sourceType={}", poolId, sourceType, e);
            //更新数据池状态
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
        String sourceType = dataPoolRtsp.getSourceType();

        DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
        if (dataPool == null) {
            return TcpResponse.error("数据池不存在");
        }

        if(dataPool.getConnectionState().equals(ConnectionState.CONNECTING.getCode())){
            return TcpResponse.error("数据源正在连接中，请稍后再试");
        }

         //更新数据池状态
        if (!dataPool.getStatus().equals(PoolStatus.WINING.getCode())) {
            dataPoolService.updateDataPoolStatus(poolId, PoolStatus.IDLE.getCode());
        }

        try {
            // 停止数据池的定时任务
            dataPoolSchedulerService.stopDataPoolScheduler(poolId);
            
            switch (sourceType) {
                case "U_DISK":
                    // U盘类型：更新连接状态为断开
                    dataPoolService.updateConnectionState(poolId, ConnectionState.DISCONNECTED.getCode());
                    log.info("[DataPoolManagement] U盘数据源已停止");
                    return TcpResponse.success("U盘数据源已停止");
                    
                case "TCP_SERVER":
                    // TCP服务端：断开连接
                    tcpClientManager.removeProvider(poolId);
                    log.info("[DataPoolManagement] TCP服务端数据源停止成功");
                    return TcpResponse.success("TCP服务端数据源停止成功");
                    
                case "TCP_CLIENT":
                    // TCP客户端：停止监听
                    tcpServerManager.removeProvider(poolId);
                    log.info("[DataPoolManagement] TCP客户端数据源停止成功");
                    return TcpResponse.success("TCP客户端数据源停止成功");
                    
                case "HTTP":
                    // HTTP：移除Provider
                    httpManager.removeProvider(poolId);
                    log.info("[DataPoolManagement] HTTP数据源停止成功");
                    return TcpResponse.success("HTTP数据源停止成功");
                    
                case "MQTT":
                    // MQTT：断开连接
                    mqttManager.removeProvider(poolId);
                    log.info("[DataPoolManagement] MQTT数据源停止成功");
                    return TcpResponse.success("MQTT数据源停止成功");
                    
                case "WEBSOCKET":
                    // WebSocket：断开连接
                    webSocketManager.removeProvider(poolId);
                    log.info("[DataPoolManagement] WebSocket数据源停止成功");
                    return TcpResponse.success("WebSocket数据源停止成功");
                    
                default:
                    return TcpResponse.error("不支持的数据源类型: " + sourceType);
            }
        } catch (Exception e) {
            log.error("[DataPoolManagement] 停止数据源失败: poolId={}, sourceType={}", poolId, sourceType, e);
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
