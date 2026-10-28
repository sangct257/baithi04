package ra.demo.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisWebSocketSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String body = new String(message.getBody(), StandardCharsets.UTF_8);

            log.info("[REDIS PUB/SUB] Channel {}: {}", channel, body);
            Map<String, Object> data = objectMapper.readValue(body, Map.class);

            if ("dispatch-offer-topic".equals(channel)) {
                String driverId = String.valueOf(data.get("driverId"));
                messagingTemplate.convertAndSend("/topic/driver/" + driverId + "/offer", (Object) data);
                log.info("[WEBSOCKET PUSH] Đã đẩy Offer Trip #{} tới Tài xế #{}", data.get("tripId"), driverId);

            } else if ("trip-status-topic".equals(channel)) {
                String tripId = String.valueOf(data.get("tripId"));
                messagingTemplate.convertAndSend("/topic/trip/" + tripId + "/status", (Object) data);
                log.info("[WEBSOCKET PUSH] Đã cập nhật trạng thái Trip #{} tới Khách hàng", tripId);
            }
        } catch (Exception e) {
            log.error("[WEBSOCKET ERROR] Lỗi xử lý tin nhắn Redis Pub/Sub", e);
        }
    }
}
