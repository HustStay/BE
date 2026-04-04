package com.payment_service.payment_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;
    
    @Column(nullable = false)
    private Long amount;
    
    @Column(length = 10)
    private String currency;
    
    @Column(length = 20)
    private String method;
    
    @Column(length = 20)
    private String status;
    
    @Column(name = "stripe_session_id", length = 255)
    private String stripeSessionId;
    
    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;
    
    @Column(length = 255)
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "check_in_date", length = 255)
    private String checkInDate;
    
    @Column(name = "check_out_date", length = 255)
    private String checkOutDate;
    
    @Column(name = "customer_email", length = 255)
    private String customerEmail;
    
    @Column(name = "customer_id")
    private Long customerId;
    
    @Column(name = "customer_name", length = 255)
    private String customerName;
    
    @Column(name = "email_sent", nullable = false)
    private Boolean emailSent = false;
    
    @Column(name = "hotel_name", length = 255)
    private String hotelName;
    
    @Column(name = "room_type", length = 255)
    private String roomType;
}