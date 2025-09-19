package com.email.handler.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfiguration implements AsyncConfigurer {

    @Autowired
    private EmailHandlerProperties properties;

    @Bean(name = "emailProcessingExecutor")
    public Executor emailProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getProcessing().getThreadPoolSize());
        executor.setMaxPoolSize(properties.getProcessing().getThreadPoolSize() * 2);
        executor.setQueueCapacity(properties.getProcessing().getQueueCapacity());
        executor.setThreadNamePrefix("email-processor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return emailProcessingExecutor();
    }
}