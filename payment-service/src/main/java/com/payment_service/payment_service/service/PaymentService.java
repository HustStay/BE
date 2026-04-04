package com.payment_service.payment_service.service;

import com.payment_service.payment_service.client.BookingServiceClient;
import com.payment_service.payment_service.model.Payment;
import com.payment_service.payment_service.repository.PaymentRepository;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingServiceClient;

    @Transactional
    public void handlePaymentSuccess(String sessionId, Session session) {
        try {
            // Tìm payment bằng session ID
            Payment payment = paymentRepository.findByStripeSessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("Payment not found for session: " + sessionId));

            // Cập nhật payment status
            payment.setStatus("SUCCESS");
            payment.setStripePaymentIntentId(session.getPaymentIntent());
            payment.setEmailSent(false); // Email functionality removed
            paymentRepository.save(payment);
            
            log.info("Payment success for booking: {}, payment ID: {}", payment.getBookingId(), payment.getId());

            // Cập nhật booking status thành CONFIRMED
            try {
                bookingServiceClient.updateBookingStatus(payment.getBookingId(), "CONFIRMED");
                log.info("Updated booking status to CONFIRMED for booking: {}", payment.getBookingId());
            } catch (Exception e) {
                log.error("Failed to update booking status for booking: {}", payment.getBookingId(), e);
                throw new RuntimeException("Critical: Failed to confirm booking after payment", e);
            }

        } catch (Exception e) {
            log.error("Error handling payment success for session: {}", sessionId, e);
            throw new RuntimeException("Failed to process payment success", e);
        }
    }
}
