package ra.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String DRIVER_LOCATION_TELEMETRY = "driver-location-telemetry";

    @Bean
    public NewTopic driverLocationTelemetryTopic() {
        return TopicBuilder.name(DRIVER_LOCATION_TELEMETRY)
                .partitions(3) // Chia tối thiểu 3 partitions để nhiều Consumer đọc song song
                .replicas(1)
                .build();
    }
}
