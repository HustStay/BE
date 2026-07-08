package com.example.booking_service.kafka;

import com.example.booking_service.dto.request.BookingRequestEvent;
import com.example.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingKafkaConsumer {

    private final BookingService bookingService;

    @KafkaListener(
            topics = "booking-request-topic",
            groupId = "booking-group",
            containerFactory = "bookingKafkaListenerContainerFactory"
    )
    public void handleBookingRequest(
            ConsumerRecord<String, BookingRequestEvent> record,
            Acknowledgment acknowledgment
    ) {
        BookingRequestEvent event = record.value();
        System.out.println("[Kafka Consumer] Nhận request: " + event.getRequestId()
                + " | key=" + record.key()
                + " | partition=" + record.partition());

        try {
            bookingService.processBookingEvent(event);
            System.out.println("[Kafka Consumer] Xử lý xong requestId=" + event.getRequestId());
        } catch (Exception e) {
            System.err.println("[Kafka Consumer] Lỗi khi xử lý requestId=" + event.getRequestId()
                    + ": " + e.getMessage());
        } finally {
            // Commit offset sau khi xử lý xong (dù thành công hay thất bại)
            acknowledgment.acknowledge();
        }
    }
}
