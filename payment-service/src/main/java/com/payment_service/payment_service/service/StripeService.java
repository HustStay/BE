package com.payment_service.payment_service.service;

import com.payment_service.payment_service.dto.CreatePaymentRequest;
import com.payment_service.payment_service.model.Payment;
import com.payment_service.payment_service.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeService {

    private final PaymentRepository paymentRepository;
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @Value("${stripe.success.url}")
    private String successUrl;
    
    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    public Session createCheckoutSession(CreatePaymentRequest request) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        // Tạo payment record
        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .amount(request.getAmount())
                .currency("VND")
                .method("STRIPE")
                .status("PENDING")
                .description(request.getDescription())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .customerEmail(request.getCustomerEmail())
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .hotelName(request.getHotelName())
                .roomType(request.getRoomType())
                .emailSent(false)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Created payment record with ID: {} for booking: {}", payment.getId(), payment.getBookingId());

        // Tạo Stripe checkout session
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("vnd")
                                                .setUnitAmount(request.getAmount())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(request.getDescription() != null ? 
                                                                    request.getDescription() : "Hotel Booking #" + request.getBookingId())
                                                                .setDescription(String.format("Check-in: %s | Check-out: %s", 
                                                                    request.getCheckInDate(), request.getCheckOutDate()))
                                                                .build()
                                                )
                                                .build()
                                )
                                .setQuantity(1L)
                                .build()
                )
                .putMetadata("bookingId", String.valueOf(request.getBookingId()))
                .putMetadata("paymentId", String.valueOf(payment.getId()))
                .build();

        Session session = Session.create(params);
        
        // Cập nhật payment với session ID
        payment.setStripeSessionId(session.getId());
        paymentRepository.save(payment);
        
        log.info("Created Stripe session: {} for booking: {}", session.getId(), request.getBookingId());
        return session;
    }

    public Session getSession(String sessionId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        return Session.retrieve(sessionId);
    }
}
