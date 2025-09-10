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
        executor.setCorePoolSize(properties.getThreadPool().getSender().getCoreSize());
        executor.setMaxPoolSize(properties.getThreadPool().getSender().getMaxSize());
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
