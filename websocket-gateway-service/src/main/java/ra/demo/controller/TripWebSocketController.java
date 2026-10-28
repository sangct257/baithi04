package ra.demo.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TripWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Endpoint 2: Khách hàng hoặc Tài xế gửi tin nhắn CHAT cho nhau trong chuyến đi
     * Destination từ Client: /app/trip/chat
     */
    @MessageMapping("/trip/chat")
    public void handleTripChat(ChatMessage message) {
        if (message == null || message.getTripId() == null) return;

        // Broadcast tin nhắn tới kênh chung của Trip để cả Khách và Tài xế đều nhận được
        String destination = "/topic/trip/" + message.getTripId() + "/chat";
        messagingTemplate.convertAndSend(destination, message);
        log.info("💬 [WS CHAT] Trip #{}: [{}] gửi -> {}", message.getTripId(), message.getSenderRole(), message.getContent());
    }

    /**
     * DTO Tin nhắn Chat giữa Khách và Tài xế
     */
    @Data
    public static class ChatMessage {
        private Long tripId;
        private Long senderId;
        private String senderRole; // "CUSTOMER" hoặc "DRIVER"
        private String content;
        private Long timestamp;
    }
}
