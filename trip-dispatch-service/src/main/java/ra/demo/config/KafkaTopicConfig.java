package ra.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
    // Topic 1: Dùng nội bộ trong Trip Service (Khách đặt chuyến -> Thuật toán Dispatcher quét tài xế)
    public static final String TRIP_DISPATCH_TOPIC = "trip-dispatch-topic";

    // Topic 2: Dùng giao tiếp với WebSocket Gateway (Thuật toán gửi đề nghị nổ chuyến -> App Tài xế)
    public static final String RIDE_REQUEST_TOPIC = "ride-request-topic";

    @Bean
    public NewTopic tripDispatchTopic() {
        return TopicBuilder.name(TRIP_DISPATCH_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic rideRequestTopic() {
        return TopicBuilder.name(RIDE_REQUEST_TOPIC)
                .partitions(3) // Chia 3 partition để xử lý đa luồng
                .replicas(1)
                .build();
    }
}
