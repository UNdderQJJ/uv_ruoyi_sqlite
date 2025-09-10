package com.ruoyi.business.service.TaskInfo;

import java.util.Map;
import java.util.Set;

/**
 * 指令发送服务接口
 * 负责将指令从缓冲池取出并发送给设备
 */
public interface CommandSenderService {
    
    /**
     * 启动指令发送 - 创建并启动Runner
     * 
     * @param taskId 任务ID
     */
    void startSending(Long taskId);
    
    /**
     * 停止指令发送
     * 
     * @param taskId 任务ID
     */
    void stopSending(Long taskId);
    
    /**
     * 获取发送状态
     * 
     * @param taskId 任务ID
     * @return 是否正在发送
     */
    boolean isSending(Long taskId);
    
    /**
     * 获取已发送数量
     * 
     * @param taskId 任务ID
     * @return 已发送数量
     */
    int getSentCount(Long taskId);
    
    /**
     * 获取所有运行中的任务
     * 
     * @return 任务ID集合
     */
    Set<Long> getRunningTasks();
    
    /**
     * 暂停发送
     * 
     * @param taskId 任务ID
     */
    void pauseSending(Long taskId);
    
    /**
     * 恢复发送
     * 
     * @param taskId 任务ID
     */
    void resumeSending(Long taskId);
    
    /**
     * 获取发送统计信息
     * 
     * @param taskId 任务ID
     * @return 统计信息
     */
    Map<String, Object> getSendingStatistics(Long taskId);
}
