package ra.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String DRIVER_STATUS_TOPIC = "driver-status-topic";

    @Bean
    public NewTopic driverStatusTopic() {
        return TopicBuilder.name(DRIVER_STATUS_TOPIC)
                .partitions(3) // Tạo 3 partitions để tối ưu hiệu năng đọc/ghi song song
                .replicas(1)   // Số bản sao (dùng 1 nếu chạy Single Broker Local)
                .build();
    }
}