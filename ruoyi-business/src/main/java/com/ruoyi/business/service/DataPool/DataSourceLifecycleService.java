package com.ruoyi.business.service.DataPool;

import com.ruoyi.business.domain.DataPool.DataPool;
import com.ruoyi.business.enums.ConnectionState;
import com.ruoyi.business.enums.PoolStatus;
import com.ruoyi.business.service.DataPool.type.Http.HttpManager;
import com.ruoyi.business.service.DataPool.type.Mqtt.MqttManager;
import com.ruoyi.business.service.DataPool.type.TcpClient.tcp.TcpServerManager;
import com.ruoyi.business.service.DataPool.type.TcpServer.tcp.TcpClientManager;
import com.ruoyi.business.service.DataPool.type.UDisk.UDiskDataSchedulerService;
import com.ruoyi.business.service.DataPool.type.WebSocket.WebSocketManager;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 公共数据源生命周期服务：用于启动和停止各类数据源，供各模块调用
 */
@Service
public class DataSourceLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(DataSourceLifecycleService.class);

    @Resource
    private IDataPoolService dataPoolService;

    @Resource
    private UDiskDataSchedulerService uDiskDataSchedulerService;

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
     * 启动指定数据池的数据源
     * @param poolId 数据池ID
     * @return 成功信息
     */
    public String startDataSource(Long poolId) {
        DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
        if (dataPool == null) {
            throw new RuntimeException("数据池不存在");
        }

        String sourceType = dataPool.getSourceType();

        if (PoolStatus.RUNNING.getCode().equals(dataPool.getStatus())) {
            throw new RuntimeException("数据池已处于运行状态，无法重新启动数据源");
        }

        // 更新数据池状态为运行中
        dataPoolService.updateDataPoolStatus(poolId, PoolStatus.RUNNING.getCode());

        String successMessage;
        switch (sourceType) {
            case "U_DISK":
                successMessage = uDiskDataSchedulerService.manualTriggerDataReading(poolId, null);
                break;
            case "TCP_SERVER":
                tcpClientManager.getOrCreateProvider(poolId).ensureConnected();
                log.info("[DataSourceLifecycle] TCP服务端数据源启动成功");
                successMessage = "TCP服务端数据源启动成功";
                break;
            case "TCP_CLIENT":
                tcpServerManager.getOrCreateProvider(poolId);
                log.info("[DataSourceLifecycle] TCP客户端数据源启动成功");
                successMessage = "TCP客户端数据源启动成功";
                break;
            case "HTTP":
                dataPoolService.updateConnectionState(poolId, ConnectionState.CONNECTING.getCode());
                httpManager.getOrCreateProvider(poolId);
                log.info("[DataSourceLifecycle] HTTP数据源启动成功");
                successMessage = "HTTP数据源启动成功";
                break;
            case "MQTT":
                mqttManager.getOrCreateProvider(poolId);
                log.info("[DataSourceLifecycle] MQTT数据源启动成功");
                successMessage = "MQTT数据源启动成功";
                break;
            case "WEBSOCKET":
                webSocketManager.getOrCreateProvider(poolId);
                log.info("[DataSourceLifecycle] WebSocket数据源启动成功");
                successMessage = "WebSocket数据源启动成功";
                break;
            default:
                successMessage = "不支持的数据源类型: " + sourceType;
                break;
        }

        // 启动调度任务
        dataPoolSchedulerService.startDataPoolWithScheduler(dataPool.getId());

        return successMessage;
    }

    /**
     * 停止指定数据池的数据源
     * @param poolId 数据池ID
     * @return 成功信息
     */
    public String stopDataSource(Long poolId) {
        DataPool dataPool = dataPoolService.selectDataPoolById(poolId);
        if (dataPool == null) {
            throw new RuntimeException("数据池不存在");
        }

        String sourceType = dataPool.getSourceType();

        if (ConnectionState.CONNECTING.getCode().equals(dataPool.getConnectionState())) {
            throw new RuntimeException("数据源正在连接中，请稍后再试");
        }

        // 非待赢单状态则置空闲
        if (!PoolStatus.WINING.getCode().equals(dataPool.getStatus())) {
            dataPoolService.updateDataPoolStatus(poolId, PoolStatus.IDLE.getCode());
        }

        // 停止调度
        dataPoolSchedulerService.stopDataPoolScheduler(poolId);

        switch (sourceType) {
            case "U_DISK":
                dataPoolService.updateConnectionState(poolId, ConnectionState.DISCONNECTED.getCode());
                log.info("[DataSourceLifecycle] U盘数据源已停止");
                return "U盘数据源已停止";
            case "TCP_SERVER":
                tcpClientManager.removeProvider(poolId);
                log.info("[DataSourceLifecycle] TCP服务端数据源停止成功");
                return "TCP服务端数据源停止成功";
            case "TCP_CLIENT":
                tcpServerManager.removeProvider(poolId);
                log.info("[DataSourceLifecycle] TCP客户端数据源停止成功");
                return "TCP客户端数据源停止成功";
            case "HTTP":
                httpManager.removeProvider(poolId);
                log.info("[DataSourceLifecycle] HTTP数据源停止成功");
                return "HTTP数据源停止成功";
            case "MQTT":
                mqttManager.removeProvider(poolId);
                log.info("[DataSourceLifecycle] MQTT数据源停止成功");
                return "MQTT数据源停止成功";
            case "WEBSOCKET":
                webSocketManager.removeProvider(poolId);
                log.info("[DataSourceLifecycle] WebSocket数据源停止成功");
                return "WebSocket数据源停止成功";
            default:
                return "不支持的数据源类型: " + sourceType;
        }
    }
}


