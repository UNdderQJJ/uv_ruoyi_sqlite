package com.ruoyi.business.service.TaskInfo;

/**
 * 设备数据处理服务接口
 * 负责处理设备上报的信息，监控设备状态
 */
public interface DeviceDataHandlerService {
    
    /**
     * 处理设备数据 - 由Netty Handler调用
     * 
     * @param deviceId 设备ID
     * @param data 设备数据
     */
    void handleDeviceData(String deviceId, String data);
    
    /**
     * 处理打印完成信号
     * 
     * @param deviceId 设备ID
     */
    void handlePrintCompleted(String deviceId);
    
    /**
     * 处理错误信号
     * 
     * @param deviceId 设备ID
     * @param errorMessage 错误信息
     */
    void handleError(String deviceId, String errorMessage);
    
    /**
     * 处理心跳信号
     * 
     * @param deviceId 设备ID
     */
    void handleHeartbeat(String deviceId);
    
    /**
     * 心跳检测 - 定时任务调用
     */
    void checkHeartbeats();
    
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
     * 处理设备注册
     * 
     * @param deviceId 设备ID
     * @param deviceInfo 设备信息
     */
    void handleDeviceRegistration(String deviceId, String deviceInfo);
    
    /**
     * 处理设备状态报告
     * 
     * @param deviceId 设备ID
     * @param status 设备状态
     */
    void handleDeviceStatusReport(String deviceId, String status);
    
    /**
     * 处理缓冲区数量报告
     * 
     * @param deviceId 设备ID
     * @param bufferCount 缓冲区数量
     */
    void handleBufferCountReport(String deviceId, Integer bufferCount);
    
    /**
     * 获取设备统计信息
     * 
     * @param deviceId 设备ID
     * @return 统计信息
     */
    java.util.Map<String, Object> getDeviceStatistics(String deviceId);
}
