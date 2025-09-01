package com.ruoyi.tcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步处理配置
 * 为Netty TCP服务器提供业务线程池
 */
@Configuration
public class AsyncConfig {

    @Value("${netty.business-thread-pool-size:20}")
    private int businessThreadPoolSize;

    @Value("${netty.business-queue-capacity:1000}")
    private int businessQueueCapacity;

    /**
     * 配置业务处理线程池
     */
    @Bean("taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(businessThreadPoolSize / 2);
        // 最大线程数
        executor.setMaxPoolSize(businessThreadPoolSize);
        // 队列容量
        executor.setQueueCapacity(businessQueueCapacity);
        // 线程名前缀
        executor.setThreadNamePrefix("Netty-Business-");
        // 空闲线程存活时间
        executor.setKeepAliveSeconds(60);
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间
        executor.setAwaitTerminationSeconds(30);
        // 拒绝策略：调用者运行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
}