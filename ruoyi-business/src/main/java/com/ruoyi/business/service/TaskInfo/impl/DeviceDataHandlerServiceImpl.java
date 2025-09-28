package com.ruoyi.business.service.TaskInfo.impl;

import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.domain.TaskInfo.DeviceTaskStatus;
import com.ruoyi.business.domain.TaskInfo.TaskDispatchStatus;
import com.ruoyi.business.domain.TaskInfo.PrintCommand;
import com.ruoyi.business.enums.*;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import com.ruoyi.business.service.TaskInfo.DeviceDataHandlerService;
import com.ruoyi.business.service.TaskInfo.ITaskDeviceLinkService;
import com.ruoyi.business.service.TaskInfo.TaskDispatcherService;
import com.ruoyi.business.service.TaskInfo.CommandQueueService;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import com.ruoyi.business.utils.StxEtxProtocolUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 设备数据处理服务实现
 * 负责处理设备上报的信息，监控设备状态
 */
@Service
public class DeviceDataHandlerServiceImpl implements DeviceDataHandlerService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceDataHandlerServiceImpl.class);
    
    // 设备通道映射
    private final ConcurrentHashMap<String, Channel> deviceChannels = new ConcurrentHashMap<>();
    
    // 设备统计信息
    private final ConcurrentHashMap<String, DeviceStatistics> deviceStatistics = new ConcurrentHashMap<>();
    
    // 心跳超时时间（毫秒）
    @Value("${task.dispatch.heartbeat-timeout:5000}")
    private long heartbeatTimeout;
    
    @Autowired
    private TaskDispatcherService dispatcher;
    
    @Autowired
    private IDeviceInfoService deviceInfoService;
    
    @Autowired
    private CommandQueueService commandQueueService;

     @Autowired
    private ISystemLogService systemLogService;


    
    // 正则表达式模式
    private static final Pattern SYSTEM_PATTERN = Pattern.compile("system:(\\d+)");
    private static final Pattern SETA_PATTERN = Pattern.compile("seta:(\\d+)(?::(.+))?");
    private static final Pattern DATA_ERROR_PATTERN = Pattern.compile("data_error");
    private static final Pattern BUFFER_COUNT_PATTERN = Pattern.compile("geta:(\\d+)");
    @Autowired
    private ITaskDeviceLinkService iTaskDeviceLinkService;

    @Override
    public void handleDeviceData(String deviceId, String data) {
        // 处理数据
        data = data.trim();
        try {
            // 打印原始数据（ASCII格式）
            if (data != null) {
                log.info("=========> 设备ID: {} 接收指令<===: {}",deviceId, data);
            }
            // 更新设备统计
            updateDeviceStatistics(deviceId, data);

            if (!data.contains("ping:")) {
                //记录通讯日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.COMMUNICATION.getCode());
                systemLog.setLogLevel(SystemLogLevel.INFO.getCode());
                systemLog.setTaskId(dispatcher.getDeviceTaskId(deviceId));
                systemLog.setDeviceId(Long.valueOf(deviceId));
                systemLog.setPoolId(dispatcher.getPoolId(systemLog.getTaskId()));
                systemLog.setContent("接收指令<==="+data);
                systemLogService.insert(systemLog);
            }

            // 解析并处理不同类型的设备数据
            if (data != null && !data.trim().isEmpty()) {
                String trimmedData = data.trim();
                
                // 处理system:2信号（打印完成）
                if (handleSystemSignal(deviceId, trimmedData)) {
                    return;
                }
                
                // 处理seta:0信号（错误）
                if (handleSetaSignal(deviceId, trimmedData)) {
                    return;
                }
                
                // 处理data_error信号
                if (handleDataErrorSignal(deviceId, trimmedData)) {
                    return;
                }
                
                // 处理缓冲区数量报告
                if (handleBufferCountReport(deviceId, trimmedData)) {
                    return;
                }
                
                // 处理其他设备数据
                handleOtherDeviceData(deviceId, trimmedData);
            }
            
        } catch (Exception e) {
            log.error("处理设备数据异常，设备ID: {}, 数据: {}", deviceId, data, e);
             //记录通讯日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.COMMUNICATION.getCode());
                systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
                systemLog.setTaskId(dispatcher.getDeviceTaskId(deviceId));
                systemLog.setDeviceId(Long.valueOf(deviceId));
                systemLog.setPoolId(dispatcher.getPoolId(systemLog.getTaskId()));
                systemLog.setContent("接收异常指令<==="+data+";"+"错误:"+e.getMessage());
                systemLogService.insert(systemLog);
        }
    }
    
    @Override
    public void handlePrintCompleted(String deviceId) {
        try {
            log.info("处理打印完成信号，设备ID: {}", deviceId);
            
            // 从TaskDispatcherService获取设备任务状态，获取当前任务ID
            DeviceTaskStatus deviceStatus = dispatcher.getDeviceTaskStatus(deviceId);
            Long currentTaskId = null;
            if (deviceStatus != null) {
                currentTaskId = deviceStatus.getCurrentTaskId();
                log.info("从设备任务状态获取当前任务ID: {}, 设备ID: {}", currentTaskId, deviceId);
            }
            
            // 如果从任务状态获取不到，尝试从数据库获取
            if (currentTaskId == null) {
                currentTaskId = getCurrentTaskId(deviceId);
                log.info("从数据库获取当前任务ID: {}, 设备ID: {}", currentTaskId, deviceId);
            }
            
            if (currentTaskId != null) {
                // 报告指令完成
                log.info("报告指令完成，设备ID: {}, 任务ID: {}", deviceId, currentTaskId);
                dispatcher.reportCommandCompleted(deviceId, currentTaskId);
            } else {
                log.warn("无法获取设备当前任务ID，跳过指令完成报告，设备ID: {}", deviceId);
            }
            
        } catch (Exception e) {
            log.error("处理打印完成信号异常，设备ID: {}", deviceId, e);
        }
    }
    
    @Override
    public void handleError(String deviceId, String errorMessage) {
        try {
            log.error("处理设备错误，设备ID: {}, 错误: {}", deviceId, errorMessage);
            
            // 报告错误
            dispatcher.reportError(deviceId, errorMessage);

            try {
                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
                systemLog.setTaskId(dispatcher.getDeviceTaskId(deviceId));
                systemLog.setDeviceId(Long.valueOf(deviceId));
                systemLog.setPoolId(dispatcher.getPoolId(systemLog.getTaskId()));
                systemLog.setContent("设备错误:" + errorMessage);
                systemLogService.insert(systemLog);

            } catch (Exception e) {
                log.warn("记录打印错误事件失败，设备ID: {}, 错误: {}", deviceId, errorMessage, e);
            }
            
            // 更新设备状态
            updateDeviceStatus(deviceId, DeviceStatus.ERROR.getCode());
            
        } catch (Exception e) {
            log.error("处理设备错误异常，设备ID: {}, 错误: {}", deviceId, errorMessage, e);
        }
    }
    
    @Override
    public void handleHeartbeat(String deviceId) {
        try {
            log.debug("处理设备心跳，设备ID: {}", deviceId);
            
            // 更新设备统计
            DeviceStatistics stats = deviceStatistics.get(deviceId);
            if (stats != null) {
                stats.setLastHeartbeat(System.currentTimeMillis());
                stats.setHeartbeatCount(stats.getHeartbeatCount() + 1);
            }
            
        } catch (Exception e) {
            log.error("处理设备心跳异常，设备ID: {}", deviceId, e);
        }
    }
    
    @Override
//    @Scheduled(fixedRate = 2000) // 每2秒检查一次心跳，使用system:2作为心跳间隔
    public void checkHeartbeats() {
        try {
            // 只有在有任务运行时才进行心跳检查
            if (!dispatcher.hasRunningTasks()) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            
            for (Map.Entry<String, Channel> entry : deviceChannels.entrySet()) {
                String deviceId = entry.getKey();
                Channel channel = entry.getValue();
                
                // 检查通道是否仍然活跃
                if (channel == null || !channel.isActive()) {
                    log.warn("设备通道已断开，设备ID: {}", deviceId);
                    handleChannelDisconnected(deviceId);
                    continue;
                }
                
                // 检查设备是否有正在运行的任务
                DeviceTaskStatus deviceStatus = dispatcher.getDeviceTaskStatus(deviceId);
                if (deviceStatus == null || deviceStatus.getCurrentTaskId() == null || !deviceStatus.getStatus().equals(TaskDeviceStatus.PRINTING.getCode())) {
                    // 设备没有运行任务，跳过心跳检查
                    continue;
                }
                
                DeviceStatistics stats = deviceStatistics.get(deviceId);
                if (stats != null) {
                    // 使用最后一次收到system信号的时间来判断心跳超时
                    long lastSystemSignalTime = stats.getLastSystemSignalTime();
                    long timeSinceLastSystemSignal = currentTime - lastSystemSignalTime;

                    boolean isError = TaskDeviceStatus.ERROR.getCode().equals(stats.getStatus());

                    // 如果设备从来没有收到过system信号（lastSystemSignalTime为0），或者距离最后一次system信号超过心跳超时
                    if (lastSystemSignalTime == 0 || timeSinceLastSystemSignal > heartbeatTimeout) {
                        if (!isError || !stats.isPingSentWhileError()) {
                            // 设备心跳超时，发送ping检查连接（ERROR态下仅一次）
                            log.warn("设备心跳超时，设备ID: {}, 距离最后一次system信号: {}", deviceId, timeSinceLastSystemSignal);
                            sendPingToDevice(deviceId, channel);
                            if (isError) {
                                stats.setPingSentWhileError(true);
                            }
                        }

                        // 如果超时时间过长（超过2倍心跳超时），处理为真正的心跳超时（ERROR态下只处理一次）
                        if (lastSystemSignalTime > 0 && timeSinceLastSystemSignal > heartbeatTimeout * 2) {
                            if (!isError) {
                                handleHeartbeatTimeout(deviceId);
                            }
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("心跳检测异常", e);
        }
    }
    
    @Override
    public void registerDeviceChannel(String deviceId, Object channel) {
        if (channel instanceof Channel) {
            deviceChannels.put(deviceId, (Channel) channel);
            
            // 注册到调度器
            dispatcher.registerDeviceChannel(deviceId, channel);
            
            // 初始化设备统计
            DeviceStatistics stats = new DeviceStatistics();
            stats.setDeviceId(deviceId);
            long currentTime = System.currentTimeMillis();
            stats.setLastHeartbeat(currentTime);
            stats.setLastActivity(currentTime); // 初始化活动时间
            stats.setLastSystemSignalTime(0); // 初始化为0，表示还没有收到system信号
            stats.setHeartbeatCount(0);
            stats.setDataCount(0);
            stats.setErrorCount(0);
            stats.setStatus(TaskDeviceStatus.PRINTING.getCode());
            deviceStatistics.put(deviceId, stats);
            
            log.info("注册设备通道，设备ID: {}", deviceId);
        }
    }
    
    @Override
    public void unregisterDeviceChannel(String deviceId) {
        deviceChannels.remove(deviceId);
        deviceStatistics.remove(deviceId);
        
        // 从调度器注销
        dispatcher.unregisterDeviceChannel(deviceId);
        
        log.info("注销设备通道，设备ID: {}", deviceId);
    }
    
    @Override
    public Object getDeviceChannel(String deviceId) {
        return deviceChannels.get(deviceId);
    }
    
    @Override
    public void handleDeviceRegistration(String deviceId, String deviceInfo) {
        try {
            log.info("处理设备注册，设备ID: {}, 设备信息: {}", deviceId, deviceInfo);
            
            // 更新设备状态为在线
            updateDeviceStatus(deviceId, DeviceStatus.ONLINE_IDLE.getCode());
            
        } catch (Exception e) {
            log.error("处理设备注册异常，设备ID: {}", deviceId, e);
        }
    }
    
    @Override
    public void handleDeviceStatusReport(String deviceId, String status) {
        try {
            log.debug("处理设备状态报告，设备ID: {}, 状态: {}", deviceId, status);
            
            // 更新设备状态
            updateDeviceStatus(deviceId, status);
            
        } catch (Exception e) {
            log.error("处理设备状态报告异常，设备ID: {}", deviceId, e);
        }
    }
    
    @Override
    public void handleBufferCountReport(String deviceId, Integer bufferCount) {
        try {
            log.debug("处理缓冲区数量报告，设备ID: {}, 数量: {}", deviceId, bufferCount);
            
            // 更新设备统计
            DeviceStatistics stats = deviceStatistics.get(deviceId);
            if (stats != null) {
                stats.setLastBufferCount(bufferCount);
            }
            
        } catch (Exception e) {
            log.error("处理缓冲区数量报告异常，设备ID: {}", deviceId, e);
        }
    }
    
    @Override
    public Map<String, Object> getDeviceStatistics(String deviceId) {
        Map<String, Object> statistics = new java.util.HashMap<>();
        
        DeviceStatistics stats = deviceStatistics.get(deviceId);
        if (stats != null) {
            statistics.put("deviceId", deviceId);
            statistics.put("lastHeartbeat", stats.getLastHeartbeat());
            statistics.put("heartbeatCount", stats.getHeartbeatCount());
            statistics.put("dataCount", stats.getDataCount());
            statistics.put("errorCount", stats.getErrorCount());
            statistics.put("lastBufferCount", stats.getLastBufferCount());
            statistics.put("isOnline", deviceChannels.containsKey(deviceId));
        }
        
        return statistics;
    }
    
    /**
     * 处理system信号
     */
    private boolean handleSystemSignal(String deviceId, String data) {
        Matcher matcher = SYSTEM_PATTERN.matcher(data);
        if (matcher.matches()) {
            String signalCode = matcher.group(1);
            if ("2".equals(signalCode)) {
                // system:2 既是心跳信号，也是打印完成信号
                long currentTime = System.currentTimeMillis();
                
                // 更新心跳时间
                DeviceStatistics stats = deviceStatistics.get(deviceId);
                if (stats != null) {
                    stats.setLastHeartbeat(currentTime);
                    stats.setLastSystemSignalTime(currentTime); // 更新最后一次system信号时间
                    stats.setHeartbeatCount(stats.getHeartbeatCount() + 1);
                    // 心跳恢复：重置错误限流标记与状态
                    stats.setPingSentWhileError(false);
                    // 若曾标记为ERROR，恢复为打印中/空闲
                    try {
                        DeviceTaskStatus dts = dispatcher.getDeviceTaskStatus(deviceId);
                        Long currentTaskId = dts != null ? dts.getCurrentTaskId() : null;
                        if (currentTaskId != null) {
                            stats.setStatus(TaskDeviceStatus.PRINTING.getCode());
                            updateDeviceStatus(deviceId, DeviceStatus.ONLINE_PRINTING.getCode());
                            // 统计接收数量：收到一次system:2即视为接收一次
                            TaskDispatchStatus taskStatus = dispatcher.getTaskDispatchStatus(currentTaskId);
                            if (taskStatus != null) {
                                Integer received = taskStatus.getReceivedCommandCount();
                                taskStatus.setReceivedCommandCount((received == null ? 0 : received) + 1);
                            }
                        } else {
                            stats.setStatus(TaskDeviceStatus.WAITING.getCode());
                            updateDeviceStatus(deviceId, DeviceStatus.ONLINE_IDLE.getCode());
                        }
                    } catch (Exception ignore) {
                    }
                }
                
                handlePrintCompleted(deviceId);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 处理seta信号
     */
    private boolean handleSetaSignal(String deviceId, String data) {
        Matcher matcher = SETA_PATTERN.matcher(data);
        if (matcher.matches()) {
            String resultCode = matcher.group(1);
            String commandData = matcher.group(2); // 获取冒号后面的数据
            
            if ("0".equals(resultCode)) {
                // seta:0 表示指令执行失败
                if (commandData != null && !commandData.trim().isEmpty()) {
                    // 重新构建指令并放回数据池
                    handleSetaFailureWithRetry(deviceId, commandData);
                } else {
                    // 没有具体数据，只报告错误
                    handleError(deviceId, "SETA指令执行失败");
                }
                return true;
            } else if ("1".equals(resultCode)) {
                // seta:1 表示指令执行成功
                log.debug("SETA指令执行成功，设备ID: {}", deviceId);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 处理SETA指令失败并重试
     */
    private void handleSetaFailureWithRetry(String deviceId, String commandData) {
        try {
            log.info("SETA指令执行失败，准备重试，设备ID: {}, 指令数据: {}", deviceId, commandData);
            
            // 重新构建指令
            String retryCommand = "seta:" + commandData;
            
            // 获取设备当前任务ID
            DeviceTaskStatus deviceStatus = dispatcher.getDeviceTaskStatus(deviceId);
            Long currentTaskId = null;
            if (deviceStatus != null) {
                currentTaskId = deviceStatus.getCurrentTaskId();
            }
            
            if (currentTaskId == null) {
                currentTaskId = getCurrentTaskId(deviceId);
            }
            
            if (currentTaskId != null) {
                // 创建重试的打印指令
                PrintCommand retryPrintCommand = new PrintCommand();
                retryPrintCommand.setDeviceId(deviceId);
                retryPrintCommand.setCommand(retryCommand);
                retryPrintCommand.setTaskId(currentTaskId);
                retryPrintCommand.setStatus(PrintCommandStatusEnum.PENDING.getCode());
                retryPrintCommand.setRetryCount(0);
                retryPrintCommand.setMaxRetryCount(3); // 设置最大重试次数
                retryPrintCommand.setCreateTime(System.currentTimeMillis());
                
                // 将重试指令放回队列
                commandQueueService.addCommandToQueue(retryPrintCommand);
                log.info("将重试指令放回队列，设备ID: {}, 指令: {}", deviceId, retryCommand);

                
            } else {
                log.warn("无法获取设备当前任务ID，跳过重试，设备ID: {}", deviceId);
                handleError(deviceId, "SETA指令执行失败，无法重试");
            }
            
        } catch (Exception e) {
            log.error("处理SETA指令失败重试异常，设备ID: {}, 指令数据: {}", deviceId, commandData, e);
            handleError(deviceId, "SETA指令执行失败，重试异常: " + e.getMessage());
        }
    }
    
    /**
     * 处理数据错误信号
     */
    private boolean handleDataErrorSignal(String deviceId, String data) {
        if (DATA_ERROR_PATTERN.matcher(data).matches()) {
            handleError(deviceId, "数据错误");
            return true;
        }
        return false;
    }
    
    /**
     * 处理缓冲区数量报告
     */
    private boolean handleBufferCountReport(String deviceId, String data) {
        Matcher matcher = BUFFER_COUNT_PATTERN.matcher(data);
        if (matcher.matches()) {
            try {
                Integer bufferCount = Integer.valueOf(matcher.group(1));
                handleBufferCountReport(deviceId, bufferCount);
                return true;
            } catch (NumberFormatException e) {
                log.warn("解析缓冲区数量失败，设备ID: {}, 数据: {}", deviceId, data);
            }
        }
        return false;
    }
    
    /**
     * 处理其他设备数据
     */
    private void handleOtherDeviceData(String deviceId, String data) {
        // 处理其他类型的设备数据
    }
    
    /**
     * 处理心跳超时
     */
    private void handleHeartbeatTimeout(String deviceId) {
        try {
            // 报告错误
            dispatcher.reportError(deviceId, "心跳超时");
            DeviceStatistics stats = deviceStatistics.get(deviceId);
            // 更新设备状态
            if (!stats.getStatus().equals(TaskDeviceStatus.ERROR.getCode())) {
                stats.setStatus(TaskDeviceStatus.ERROR.getCode());
                updateDeviceStatus(deviceId, DeviceStatus.ERROR.getCode());
                //记录打印日志
                SystemLog systemLog = new SystemLog();
                systemLog.setLogType(SystemLogType.PRINT.getCode());
                systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
                systemLog.setTaskId(dispatcher.getDeviceTaskId(deviceId));
                systemLog.setDeviceId(Long.valueOf(deviceId));
                systemLog.setContent("设备心跳超时状态改为故障！");
            }

            // 不再主动关闭或注销通道，仅更新设备状态
            
        } catch (Exception e) {
            log.error("处理心跳超时异常，设备ID: {}", deviceId, e);
        }
    }
    
    /**
     * 处理通道断开
     */
    private void handleChannelDisconnected(String deviceId) {
        try {
            // 更新设备状态为离线
            updateDeviceStatus(deviceId, DeviceStatus.OFFLINE.getCode());
            
            log.info("设备通道已断开，设备ID: {}", deviceId);
            
        } catch (Exception e) {
            log.error("处理通道断开异常，设备ID: {}", deviceId, e);
        }
    }
    
    /**
     * 向设备发送ping检查连接
     */
    private void sendPingToDevice(String deviceId, Channel channel) {
        try {
            if (channel != null && channel.isActive()) {
                // 使用STX/ETX协议格式发送ping命令
                String pingCommand = "ping";
                byte[] commandBytes = StxEtxProtocolUtil.buildCommand(pingCommand);
                channel.writeAndFlush(Unpooled.wrappedBuffer(commandBytes));
                
                log.debug("向设备发送ping，设备ID: {}, 协议数据: {}", 
                         deviceId, com.ruoyi.business.utils.StxEtxProtocolUtil.toHexString(commandBytes));
                
                // 更新最后活动时间
                DeviceStatistics stats = deviceStatistics.get(deviceId);
                if (stats != null) {
                    stats.setLastActivity(System.currentTimeMillis());
                }
            }
        } catch (Exception e) {
            log.error("发送ping失败，设备ID: {}", deviceId, e);
        }
    }
    
    /**
     * 更新设备状态
     */
    private void updateDeviceStatus(String deviceId, String status) {
        try {
            Long deviceIdLong = Long.valueOf(deviceId);
            deviceInfoService.updateDeviceStatus(deviceIdLong, status);
        } catch (Exception e) {
            log.error("更新设备状态失败，设备ID: {}, 状态: {}", deviceId, status, e);
        }
    }
    
    /**
     * 更新设备统计
     */
    private void updateDeviceStatistics(String deviceId, String data) {
        DeviceStatistics stats = deviceStatistics.get(deviceId);
        if (stats != null) {
            stats.setDataCount(stats.getDataCount() + 1);
            stats.setLastDataTime(System.currentTimeMillis());
        }
    }
    
    /**
     * 获取设备当前任务ID
     */
    private Long getCurrentTaskId(String deviceId) {
        try {
            Long deviceIdLong = Long.valueOf(deviceId);
            DeviceInfo deviceInfo = deviceInfoService.selectDeviceInfoById(deviceIdLong);
            return deviceInfo != null ? deviceInfo.getCurrentTaskId() : null;
        } catch (Exception e) {
            log.error("获取设备当前任务ID失败，设备ID: {}", deviceId, e);
            return null;
        }
    }
    
    /**
     * 设备统计信息内部类
     */
    @Data
    private static class DeviceStatistics {
        private String deviceId;
        private long lastHeartbeat;
        private int heartbeatCount;
        private int dataCount;
        private int errorCount;
        private Integer lastBufferCount;
        private long lastDataTime;
        private long lastActivity;
        private long lastSystemSignalTime; // 最后一次收到system信号的时间
        private String status;
        private boolean pingSentWhileError; // ERROR状态下是否已发送过一次ping
    }
}
