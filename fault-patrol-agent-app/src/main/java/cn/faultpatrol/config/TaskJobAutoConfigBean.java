package cn.faultpatrol.config;

//import cn.faultpatrol.task.job.TaskJob;
//import cn.faultpatrol.task.job.config.TaskJobAutoConfig;
//import cn.faultpatrol.task.job.config.TaskJobAutoProperties;
//import cn.faultpatrol.task.job.provider.ITaskDataProvider;
//import cn.faultpatrol.task.job.service.ITaskJobService;
//import cn.faultpatrol.task.job.service.TaskJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.List;

/**
 *
 * 2025/9/13 16:41
 */

//@Configuration
//@EnableScheduling
//@EnableConfigurationProperties({TaskJobAutoProperties.class})
//@ConditionalOnProperty(
//        prefix = "faultpatrol.task.job",
//        name = {"enabled"},
//        havingValue = "true",
//        matchIfMissing = true
//)
public class TaskJobAutoConfigBean {

//    private final Logger log = LoggerFactory.getLogger(TaskJobAutoConfig.class);
//
//    @Bean({"faultPatrolTaskScheduler"})
//    public TaskScheduler taskScheduler(TaskJobAutoProperties properties) {
//        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
//        scheduler.setPoolSize(properties.getPoolSize());
//        scheduler.setThreadNamePrefix(properties.getThreadNamePrefix());
//        scheduler.setWaitForTasksToCompleteOnShutdown(properties.isWaitForTasksToCompleteOnShutdown());
//        scheduler.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
//        scheduler.initialize();
//        this.log.info("fault-patrol，任务调度器初始化完成。线程池大小: {}, 线程名前缀: {}", properties.getPoolSize(), properties.getThreadNamePrefix());
//        return scheduler;
//    }
//
//    @Bean
//    public ITaskJobService taskJobService(TaskScheduler faultPatrolTaskScheduler, List<ITaskDataProvider> taskDataProviders) {
//        TaskJobService taskJobService = new TaskJobService(faultPatrolTaskScheduler, taskDataProviders);
//        taskJobService.initializeTasks();
//        return taskJobService;
//    }
//
//    @Bean
//    public TaskJob taskJob(TaskJobAutoProperties properties, ITaskJobService taskJobService) {
//        this.log.info("fault-patrol，任务调度作业初始化完成。刷新间隔: {}ms, 清理cron: {}", properties.getRefreshInterval(), properties.getCleanInvalidTasksCron());
//        return new TaskJob(properties, taskJobService);
//    }

}
