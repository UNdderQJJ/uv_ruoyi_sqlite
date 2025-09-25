package com.ruoyi.business.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 任务线程池配置
 */
@Configuration
@EnableConfigurationProperties(TaskDispatchProperties.class)
public class TaskThreadPoolConfig {
    
    @Autowired
    private TaskDispatchProperties properties;
    
    @Bean("taskProducerExecutor")
    public ThreadPoolTaskExecutor taskProducerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getThreadPool().getProducer().getCoreSize());
        executor.setMaxPoolSize(properties.getThreadPool().getProducer().getMaxSize());
        executor.setQueueCapacity(properties.getThreadPool().getProducer().getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getThreadPool().getProducer().getKeepAliveSeconds());
        executor.setThreadNamePrefix("TaskProducer-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    @Bean("taskSenderExecutor")
    public ThreadPoolTaskExecutor taskSenderExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // --- 核心修改 开始 ---
        // 将核心线程数增加，例如增加到8个。这意味着系统会一直保持8个线程随时准备发送指令。
        executor.setCorePoolSize(8); 
        // 将最大线程数增加，例如增加到16个。在任务高峰期，线程池最多可以扩展到16个线程来处理指令发送。
        executor.setMaxPoolSize(16);
        // --- 核心修改 结束 ---
        executor.setQueueCapacity(properties.getThreadPool().getSender().getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getThreadPool().getSender().getKeepAliveSeconds());
        executor.setThreadNamePrefix("TaskSender-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    @Bean("taskHandlerExecutor")
    public ThreadPoolTaskExecutor taskHandlerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getThreadPool().getHandler().getCoreSize());
        executor.setMaxPoolSize(properties.getThreadPool().getHandler().getMaxSize());
        executor.setQueueCapacity(properties.getThreadPool().getHandler().getQueueCapacity());
        executor.setKeepAliveSeconds(properties.getThreadPool().getHandler().getKeepAliveSeconds());
        executor.setThreadNamePrefix("TaskHandler-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
