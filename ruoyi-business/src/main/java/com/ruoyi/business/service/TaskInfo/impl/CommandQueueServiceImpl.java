package com.ruoyi.business.service.TaskInfo.impl;

import com.ruoyi.business.domain.TaskInfo.PrintCommand;
import com.ruoyi.business.service.TaskInfo.CommandQueueService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 指令队列服务实现
 * 提供线程安全的指令队列管理
 */
@Service
public class CommandQueueServiceImpl implements CommandQueueService {
    
    // 指令缓冲池 - 生成池消费者桥梁
    private final BlockingQueue<PrintCommand> commandQueue = new LinkedBlockingQueue<>(1000);
    
    @Override
    public void addCommandToQueue(PrintCommand command) {
        try {
            boolean success = commandQueue.offer(command, 1, TimeUnit.SECONDS);
            if (!success) {
                // 队列已满，可以选择丢弃或者记录日志
                System.err.println("指令队列已满，丢弃指令: " + command.getId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("添加指令到队列被中断");
        }
    }
    
    @Override
    public PrintCommand getNextCommand() throws InterruptedException {
        return commandQueue.take();
    }
    
    @Override
    public int getQueueSize() {
        return commandQueue.size();
    }
    
    @Override
    public void clearQueue() {
        commandQueue.clear();
    }

    @Override
    public List<PrintCommand> getAllCommandsSnapshot() {
        return new ArrayList<>(commandQueue);
    }
}
