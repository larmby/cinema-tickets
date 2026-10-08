package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.engineering.recruitment.pricing.TicketCostCalculator;
import uk.gov.dwp.engineering.recruitment.pricing.TicketPricing;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

import java.math.BigDecimal;
import java.util.Map;

import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

    private static final TicketPricing TICKET_PRICING = new TicketPricing(Map.of(
            ADULT, new BigDecimal("25.99"),
            CHILD, new BigDecimal("17.50"),
            INFANT, BigDecimal.ZERO));

    private final TicketCostCalculator ticketCostCalculator = new TicketCostCalculator(TICKET_PRICING);

    @Mock
    private PaymentService mockPaymentService;

    @Mock
    private SeatReservationService mockSeatReservationService;

    private CinemaTicketsServiceImpl cinemaTicketsServiceUnderTest;

    @BeforeEach
    void setUp() {
        cinemaTicketsServiceUnderTest = new CinemaTicketsServiceImpl(mockPaymentService, mockSeatReservationService, ticketCostCalculator);
    }

    @Test
    void given() {

    }
}
