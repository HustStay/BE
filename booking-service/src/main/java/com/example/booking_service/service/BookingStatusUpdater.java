package com.example.booking_service.service;

import com.example.booking_service.repository.BookingRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingStatusUpdater {

    private final BookingRequestRepository bookingRequestRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(String requestId, Integer bookingId) {
        bookingRequestRepository.findById(requestId).ifPresent(req -> {
            req.setStatus("SUCCESS");
            req.setBookingId(bookingId);
            req.setProcessedAt(LocalDateTime.now());
            bookingRequestRepository.save(req);
            System.out.println("[StatusUpdater] requestId=" + requestId + " → SUCCESS, bookingId=" + bookingId);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String requestId, String errorMessage) {
        bookingRequestRepository.findById(requestId).ifPresent(req -> {
            req.setStatus("FAILED");
            req.setErrorMessage(errorMessage);
            req.setProcessedAt(LocalDateTime.now());
            bookingRequestRepository.save(req);
            System.out.println("[StatusUpdater] requestId=" + requestId + " → FAILED: " + errorMessage);
        });
    }
}
