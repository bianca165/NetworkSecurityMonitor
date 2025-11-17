package com.example.networksecuritymonitor;

import com.example.networksecuritymonitor.service.IPBlockerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.context.ConfigurableApplicationContext;

@EnableMongoRepositories(basePackages = "com.example.networksecuritymonitor.repo")
@SpringBootApplication(scanBasePackages = "com.example.networksecuritymonitor")
public class NetworkSecurityMonitorApplication {

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication.run(NetworkSecurityMonitorApplication.class, args);
        IPBlockerService ipBlockerService = context.getBean(IPBlockerService.class);
        ipBlockerService.monitorAndBlockIPs();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[SHUTDOWN] inchidere si oprire scheduler ");
            IPBlockerService.shutdownScheduler();
        }));
    }

}
