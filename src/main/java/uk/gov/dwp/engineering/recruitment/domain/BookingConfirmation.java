package uk.gov.dwp.engineering.recruitment.domain;

import java.math.BigDecimal;

public record BookingConfirmation(Long accountId, BigDecimal totalAmount, Long seatsReserved) {

    public BookingConfirmation(Long accountId) {
        this(accountId, null, null);
    }
}
