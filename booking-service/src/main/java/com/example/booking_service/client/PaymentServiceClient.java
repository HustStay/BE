package com.example.booking_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "payment-service", url = "${PAYMENT_SERVICE_URL:}")
public interface PaymentServiceClient {

    // Kiểm tra trạng thái thanh toán của một booking.
    @GetMapping("/payos/status/booking/{bookingId}")
    Map<String, Object> getPaymentStatusByBookingId(@PathVariable("bookingId") long bookingId);
}
