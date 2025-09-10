package com.ruoyi.business.service.TaskInfo;

import com.ruoyi.business.domain.TaskInfo.PrintCommand;

/**
 * 指令队列服务接口
 * 用于解耦数据生成池和调度中心
 */
public interface CommandQueueService {
    
    /**
     * 添加指令到队列
     * 
     * @param command 打印指令
     */
    void addCommandToQueue(PrintCommand command);
    
    /**
     * 获取下一个指令
     * 
     * @return 打印指令
     * @throws InterruptedException 中断异常
     */
    PrintCommand getNextCommand() throws InterruptedException;
    
    /**
     * 获取队列大小
     * 
     * @return 队列大小
     */
    int getQueueSize();
    
    /**
     * 清空队列
     */
    void clearQueue();

    /**
     * 获取当前队列中所有指令的快照（不会移除）
     */
    java.util.List<PrintCommand> getAllCommandsSnapshot();
}
