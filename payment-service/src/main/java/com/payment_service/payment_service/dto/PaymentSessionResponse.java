package com.payment_service.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSessionResponse {
    private String sessionUrl;      // checkoutUrl (trang PayOS)
    private String sessionId;       // orderCode
    private Long bookingId;
    private Long amount;
    private String status;
    private String qrCode;          // Chuỗi EMV QR Code từ PayOS (để render QR ảnh)
    private String accountName;     // Tên chủ tài khoản
    private String accountNumber;   // Số tài khoản ngân hàng
    private String bankName;        // Tên ngân hàng
    private String description;     // Nội dung chuyển khoản
}
