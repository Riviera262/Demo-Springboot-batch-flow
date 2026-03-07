package org.example.transactionsmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "excelExecutor")
    public Executor excelExecuteor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        //Core pool
        executor.setCorePoolSize(2);

        //Max pool
        executor.setMaxPoolSize(5);

        //Maximum quene in batch process tasks
        executor.setQueueCapacity(500);

        //Thread name for debug
        executor.setThreadNamePrefix("ExcelImport-");

        //Wait for processing before shut down
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        //If quene is full, run in caller thread
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    @Bean(name = "fileProcessorExecutor")
    public Executor fileProcessExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("File-processor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
