package com.ruoyi.business.service.TaskInfo;

import java.util.Set;

/**
 * 数据生产服务接口
 * 负责从数据源高效地准备打印指令
 */
public interface DataPoolProducerService {
    
    /**
     * 启动数据生产 - 创建并启动Runner
     * 
     * @param taskId 任务ID
     * @param poolId 数据池ID
     */
    void startProduction(Long taskId, Long poolId);
    
    /**
     * 停止数据生产
     * 
     * @param taskId 任务ID
     */
    void stopProduction(Long taskId);
    
    /**
     * 获取生产状态
     * 
     * @param taskId 任务ID
     * @return 是否正在生产
     */
    boolean isProducing(Long taskId);
    
    /**
     * 获取已生产数量
     * 
     * @param taskId 任务ID
     * @return 已生产数量
     */
    int getProducedCount(Long taskId);
    
    /**
     * 获取所有运行中的任务
     * 
     * @return 任务ID集合
     */
    Set<Long> getRunningTasks();
    
    /**
     * 暂停生产
     * 
     * @param taskId 任务ID
     */
    void pauseProduction(Long taskId);
    
    /**
     * 恢复生产
     * 
     * @param taskId 任务ID
     */
    void resumeProduction(Long taskId);
    
    /**
     * 获取生产统计信息
     * 
     * @param taskId 任务ID
     * @return 统计信息
     */
    java.util.Map<String, Object> getProductionStatistics(Long taskId);
}
