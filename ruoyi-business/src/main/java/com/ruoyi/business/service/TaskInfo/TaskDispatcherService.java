package com.ruoyi.business.service.TaskInfo;

import com.ruoyi.business.domain.TaskInfo.TaskDispatchRequest;
import com.ruoyi.business.domain.TaskInfo.TaskDispatchStatus;
import com.ruoyi.business.domain.TaskInfo.DeviceTaskStatus;
import com.ruoyi.business.domain.TaskInfo.PrintCommand;

/**
 * 任务调度服务接口
 * 系统的"大脑"，负责协调各个服务组件
 */
public interface TaskDispatcherService {
    
    /**
     * 启动新任务 - 架构文档中的核心入口
     * 
     * @param request 任务调度请求
     */
    void startNewTask(TaskDispatchRequest request);
    
    /**
     * 停止任务调度
     * 
     * @param taskId 任务ID
     */
    void stopTaskDispatch(Long taskId);

    /**
     * 完成任务调度
     *
     * @param taskId 任务ID
     */
    void finishTaskDispatch(Long taskId);
    
    /**
     * 执行预检流程
     * 
     * @param taskId 任务ID
     * @param deviceIds 设备ID数组
     * @return 预检是否成功
     */
    boolean executePreFlightChecks(Long taskId, Long[] deviceIds);
    
    /**
     * 报告指令完成
     * 
     * @param deviceId 设备ID
     * @param taskId 任务ID
     */
    void reportCommandCompleted(String deviceId, Long taskId);
    
    /**
     * 报告错误
     * 
     * @param deviceId 设备ID
     * @param errorMessage 错误信息
     */
    void reportError(String deviceId, String errorMessage);
    
    // 队列相关方法已移至CommandQueueService
    
    /**
     * 检查设备是否可以接收指令
     * 
     * @param deviceId 设备ID
     * @return 是否可以接收指令
     */
    boolean canDeviceReceiveCommand(String deviceId);


    /**
     * 请求设备缓冲区数量
     *
     * @param deviceId 设备ID
     */
    void requestDeviceBufferCount(String deviceId);

    /**
     * 同步设备端上报的缓存池数量到调度器（覆盖本地计数器）
     *
     * @param deviceId 设备ID
     * @param bufferCount 设备端上报的缓存池数量
     */
    void updateDeviceBufferCount(String deviceId, Integer bufferCount);

    /**
     * 报告指令已发送
     * 
     * @param deviceId 设备ID
     */
    void reportCommandSent(String deviceId);
    
    /**
     * 获取任务调度状态
     * 
     * @param taskId 任务ID
     * @return 任务调度状态
     */
    TaskDispatchStatus getTaskDispatchStatus(Long taskId);
    
    /**
     * 获取设备任务状态
     * 
     * @param deviceId 设备ID
     * @return 设备任务状态
     */
    DeviceTaskStatus getDeviceTaskStatus(String deviceId);
    
    /**
     * 注册设备通道
     * 
     * @param deviceId 设备ID
     * @param channel 网络通道
     */
    void registerDeviceChannel(String deviceId, Object channel);
    
    /**
     * 注销设备通道
     * 
     * @param deviceId 设备ID
     */
    void unregisterDeviceChannel(String deviceId);
    
    /**
     * 获取设备通道
     * 
     * @param deviceId 设备ID
     * @return 网络通道
     */
    Object getDeviceChannel(String deviceId);
    
    /**
     * 获取所有运行中的任务
     * 
     * @return 任务ID集合
     */
    java.util.Set<Long> getRunningTasks();
    
    /**
     * 检查是否有正在运行的任务
     * 
     * @return 是否有运行中的任务
     */
    boolean hasRunningTasks();
    
    /**
     * 获取任务统计信息
     * 
     * @param taskId 任务ID
     * @return 统计信息
     */
    java.util.Map<String, Object> getTaskStatistics(Long taskId);
    
    /**
     * 为指令分配可用设备
     * 
     * @param command 打印指令
     * @return 分配的设备ID，如果无可用设备返回null
     */
    String assignDeviceForCommand(PrintCommand command);

    /**
     * 统一的指令发送方法
     * 优先通过已注册的设备通道下发（使用STX/ETX协议）；无通道时退回短连TCP。
     * 
     * @param deviceId 设备ID
     * @param command 指令内容
     * @return 是否发送成功
     */
    boolean sendCommandToDevice(String deviceId, String command);

    /**
     * 获取任务的数据池poolId
     */
    Long getPoolId(Long taskId);

    /**
     * 获取设备的任务id
     */
    Long getDeviceTaskId(String deviceId);
}
