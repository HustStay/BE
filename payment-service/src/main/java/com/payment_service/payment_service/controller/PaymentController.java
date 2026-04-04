package com.payment_service.payment_service.controller;

import com.payment_service.payment_service.dto.CreatePaymentRequest;
import com.payment_service.payment_service.dto.PaymentSessionResponse;
import com.payment_service.payment_service.service.PaymentService;
import com.payment_service.payment_service.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment-service/stripe")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final StripeService stripeService;
    private final PaymentService paymentService;

    @PostMapping("/create-payment-session")
    public ResponseEntity<PaymentSessionResponse> createPaymentSession(@ModelAttribute CreatePaymentRequest request) {
        try {
            log.info("Creating payment session for booking: {}, amount: {}", request.getBookingId(), request.getAmount());
            
            Session session = stripeService.createCheckoutSession(request);
            
            PaymentSessionResponse response = PaymentSessionResponse.builder()
                    .sessionUrl(session.getUrl())
                    .sessionId(session.getId())
                    .bookingId(request.getBookingId())
                    .amount(request.getAmount())
                    .status("CREATED")
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Stripe error creating session for booking: {}", request.getBookingId(), e);
            return ResponseEntity.status(500).body(
                PaymentSessionResponse.builder()
                    .status("ERROR")
                    .build()
            );
        }
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSession(@PathVariable String sessionId) {
        try {
            log.info("Retrieving session: {}", sessionId);
            Session session = stripeService.getSession(sessionId);
            
            // Nếu payment thành công, xử lý logic
            if ("complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus())) {
                paymentService.handlePaymentSuccess(sessionId, session);
            }
            
            return ResponseEntity.ok(session);
        } catch (StripeException e) {
            log.error("Error retrieving session: {}", sessionId, e);
            return ResponseEntity.status(404).body("Session not found");
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is running");
    }
}
