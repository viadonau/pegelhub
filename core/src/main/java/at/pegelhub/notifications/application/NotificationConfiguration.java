package at.pegelhub.notifications.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfiguration {

    @Bean
    ThreadPoolTaskScheduler notificationScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        // A dedicated sender isolates slow transports from QA and cannot reclaim its own in-flight lease.
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("notification-");
        scheduler.setWaitForTasksToCompleteOnShutdown(false);

        return scheduler;
    }
}
