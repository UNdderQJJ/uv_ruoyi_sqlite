package com.ruoyi.business.service.TaskInfo.runner;

import com.ruoyi.business.domain.SystemLog.SystemLog;
import com.ruoyi.business.domain.TaskInfo.DeviceTaskStatus;
import com.ruoyi.business.domain.TaskInfo.PrintCommand;
import com.ruoyi.business.domain.TaskInfo.TaskDispatchStatus;
import com.ruoyi.business.enums.SystemLogLevel;
import com.ruoyi.business.enums.SystemLogType;
import com.ruoyi.business.service.TaskInfo.TaskDispatcherService;
import com.ruoyi.business.service.TaskInfo.DeviceDataHandlerService;
import com.ruoyi.business.service.TaskInfo.CommandQueueService;
import com.ruoyi.business.enums.PrintCommandStatusEnum;
import com.ruoyi.business.service.SystemLog.ISystemLogService;
import com.ruoyi.business.service.DeviceInfo.IDeviceInfoService;
import com.ruoyi.business.domain.DeviceInfo.DeviceInfo;
import io.netty.channel.Channel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 指令发送运行器
 * 负责将指令从缓冲池取出并发送给设备
 */
public class CommandSenderRunner implements Runnable {
    
    private static final Logger log = LoggerFactory.getLogger(CommandSenderRunner.class);
    
    private final Long taskId;
    private final TaskDispatcherService dispatcher;
    private final CommandQueueService commandQueueService;

    /**
     * -- GETTER --
     *  是否正在运行
     */
    @Getter
    private volatile boolean running = true;
    /**
     * -- GETTER --
     *  是否已暂停
     */
    @Getter
    private volatile boolean paused = false;
    private final AtomicInteger sentCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    
    public CommandSenderRunner(Long taskId, 
                              TaskDispatcherService dispatcher,
                              CommandQueueService commandQueueService) {
        this.taskId = taskId;
        this.dispatcher = dispatcher;
        this.commandQueueService = commandQueueService;
    }
    
    @Override
    public void run() {
        log.info("指令发送器启动，任务ID: {}", taskId);

        try {
            // 等待1秒，等待设备初始化完成
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        while (running) {
            try {
                if (paused) {
                    Thread.sleep(1000); // 暂停时等待1秒
                    continue;
                }
                
                // 从指令队列获取下一个指令
                PrintCommand command = commandQueueService.getNextCommand(taskId);
                if (command != null) {
                    sendCommand(command);
                } else {
                    // 没有指令时，短暂等待后继续检查
                    Thread.sleep(100);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("指令发送器被中断，任务ID: {}", taskId);
                break;
            } catch (Exception e) {
                log.error("指令发送异常，任务ID: {}", taskId, e);
                dispatcher.reportError("SENDER", "指令发送异常: " + e.getMessage());
                try {
                    Thread.sleep(5000); // 异常后等待5秒再重试
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.info("指令发送器停止，任务ID: {}, 总发送数量: {}, 失败数量: {}", 
                taskId, sentCount.get(), failedCount.get());
    }
    
    /**
     * 发送指令
     */
    private void sendCommand(PrintCommand command) {
        // 动态分配设备
        String deviceId = dispatcher.assignDeviceForCommand(command);
        if (deviceId == null) {
//            log.debug("无可用设备，重新放回队列，指令ID: {}", command.getId());
            // 无可用设备，重新放回队列
            commandQueueService.addCommandToQueue(command);
            return;
        }
        
        // 设置分配的设备ID
        command.setDeviceId(deviceId);
        
        try {
            // 检查设备是否可以接收指令
            if (!dispatcher.canDeviceReceiveCommand(deviceId)) {
//                log.debug("设备缓存已满，重新放回队列，设备ID: {}", deviceId);
                // 设备缓存已满，重新放回队列
                commandQueueService.addCommandToQueue(command);
                return;
            }
            
            // 获取设备通道
                Object channelObj = dispatcher.getDeviceChannel(deviceId);
            if (!(channelObj instanceof Channel channel)) {
                log.warn("设备通道不可用，设备ID: {}", deviceId);
                handleSendFailure(command, "设备通道不可用");
                return;
            }

            if (!channel.isActive()) {
                log.warn("设备通道未激活，设备ID: {}", deviceId);
                handleSendFailure(command, "设备通道未激活");
                return;
            }

            // 发送指令
            String commandStr = command.getCommand();
            if (commandStr == null || commandStr.trim().isEmpty()) {
                log.warn("指令内容为空，设备ID: {}", deviceId);
                handleSendFailure(command, "指令内容为空");
                return;
            }

            // 使用统一的发送方法（STX/ETX协议格式）
            boolean sendSuccess = dispatcher.sendCommandToDevice(deviceId, commandStr);
            if (!sendSuccess) {
                log.warn("指令发送失败，设备ID: {}, 指令: {}", deviceId, commandStr);
                handleSendFailure(command, "指令发送失败");
                return;
            }
            
            // 更新指令状态
            command.setStatus(PrintCommandStatusEnum.SENT.getCode());
            command.setSentTime(System.currentTimeMillis());
            
            // 记录轻量的已发送ID，避免回填整个指令对象导致性能下降
            try {
                String dataPoolItemData =command.getData();
                Long dataPoolItemId = Long.valueOf(command.getId());
                Long taskId = command.getTaskId();
                Long poolId = dispatcher.getPoolId(taskId);
                commandQueueService.addSentRecord(taskId, dataPoolItemId,dataPoolItemData,deviceId,poolId);
            } catch (NumberFormatException ignore) {
            }
            
            // 报告指令已发送
            dispatcher.reportCommandSent(deviceId);


            sentCount.incrementAndGet();

            log.debug("指令发送成功，设备ID: {}, 指令: {}", deviceId, commandStr);
            
        } catch (Exception e) {
            log.error("发送指令异常，设备ID: {}, 指令: {}", deviceId, command.getCommand(), e);
            //记录打印日志
            SystemLog systemLog = new SystemLog();
            systemLog.setLogType(SystemLogType.PRINT.getCode());
            systemLog.setLogLevel(SystemLogLevel.ERROR.getCode());
            systemLog.setTaskId(taskId);
            systemLog.setDeviceId(Long.valueOf(deviceId));
            systemLog.setContent("发送指令异常:"+e.getMessage());
            handleSendFailure(command, "发送异常: " + e.getMessage());
        }
    }

    
    /**
     * 处理发送失败
     */
    private void handleSendFailure(PrintCommand command, String errorMessage) {
        command.setErrorMessage(errorMessage);
        command.setRetryCount(command.getRetryCount() + 1);
        
        if (command.getRetryCount() < command.getMaxRetryCount()) {
            // 重试
            command.setStatus(PrintCommandStatusEnum.RETRYING.getCode());
            commandQueueService.addCommandToQueue(command);
            log.info("指令重试，设备ID: {}, 重试次数: {}/{}", 
                    command.getDeviceId(), command.getRetryCount(), command.getMaxRetryCount());
        } else {
            // 重试次数用完，标记为失败
            command.setStatus(PrintCommandStatusEnum.FAILED.getCode());
            command.setCompletedTime(System.currentTimeMillis());
            failedCount.incrementAndGet();
            
            // 报告错误
            dispatcher.reportError(command.getDeviceId(), "指令发送失败: " + errorMessage);
            log.error("指令发送最终失败，设备ID: {}, 错误: {}", command.getDeviceId(), errorMessage);
        }
    }
    
    /**
     * 统一的下发指令方法（基于设备通道）
     * 使用统一的发送方法，支持STX/ETX协议格式和回退机制。
     */
    public boolean sendRawCommandToDevice(String deviceId, String commandStr) {
        return dispatcher.sendCommandToDevice(deviceId, commandStr);
    }

    /**
     * 统一的通道发送方法（避免使用会短连的 DeviceCommandServiceImpl）
     * 与上面方法同义，提供更直观命名。
     */
    public boolean sendCommand(String deviceId, String commandStr) {
        return sendRawCommandToDevice(deviceId, commandStr);
    }
    
    /**
     * 停止发送
     */
    public void stop() {
        this.running = false;
        log.info("指令发送器停止请求，任务ID: {}", taskId);
    }
    
    /**
     * 暂停发送
     */
    public void pause() {
        this.paused = true;
        log.info("指令发送器暂停，任务ID: {}", taskId);
    }
    
    /**
     * 恢复发送
     */
    public void resume() {
        this.paused = false;
        log.info("指令发送器恢复，任务ID: {}", taskId);
    }
    
    /**
     * 获取已发送数量
     */
    public int getSentCount() {
        return sentCount.get();
    }
    
    /**
     * 获取失败数量
     */
    public int getFailedCount() {
        return failedCount.get();
    }

}
