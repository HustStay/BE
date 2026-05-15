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
    
    @Column(name = "order_code", nullable = false)
    private String orderCode;
    
    @Column(name = "amount", nullable = false)
    private Long amount;
    
    @Column(name = "status", nullable = false)
    private String status;
    
    @Column(name = "provider", nullable = false)
    private String provider;
    
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    // Lưu chuỗi EMV QR Code từ PayOS để frontend render ảnh QR
    @Column(name = "qr_code", length = 2000)
    private String qrCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}