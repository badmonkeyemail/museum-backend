package com.hml.museum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 后台异步任务线程池。
 *
 * <p>缩略图生成属于 CPU + RustFS I/O 混合型任务，
 * 与 Web 请求线程池隔离，避免大图片处理阻塞接口请求。</p>
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "thumbnailTaskExecutor")
    public Executor thumbnailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("thumbnail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
