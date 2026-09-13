package at.pegelhub.quality.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(QualityProperties.class)
public class QualityConfiguration {

    @Bean
    ThreadPoolTaskScheduler qualityScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        // This is the process-wide QA slot; increasing the pool requires redesigning recovery and retention.
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("quality-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);

        return scheduler;
    }
}
