package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.pricing.TicketCostCalculator;
import uk.gov.dwp.engineering.recruitment.pricing.TicketPricing;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import uk.gov.dwp.engineering.recruitment.validation.BookingValidator;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

    private static final TicketPricing TICKET_PRICING = new TicketPricing(Map.of(
            ADULT, new BigDecimal("25.99"),
            CHILD, new BigDecimal("17.50"),
            INFANT, BigDecimal.ZERO));

    private final BookingValidator bookingValidator = new BookingValidator();

    private final TicketCostCalculator ticketCostCalculator = new TicketCostCalculator(TICKET_PRICING);

    @Mock
    private PaymentService mockPaymentService;

    @Mock
    private SeatReservationService mockSeatReservationService;

    private CinemaTicketsServiceImpl cinemaTicketsServiceUnderTest;

    @BeforeEach
    void setUp() {
        cinemaTicketsServiceUnderTest = new CinemaTicketsServiceImpl(
                mockPaymentService, mockSeatReservationService, bookingValidator, ticketCostCalculator);
    }

    @Test
    void givenInvalidAccountId_whenPurchased_thenRejectedAndNothingCharged() {
        assertThrows(InvalidBookingException.class,
                () -> cinemaTicketsServiceUnderTest.purchaseTickets(0L, new TicketRequest(ADULT, 1)));

        verifyNoInteractions(mockPaymentService, mockSeatReservationService);
    }

    @Test
    void givenBusinessRuleBroken_whenPurchased_thenRejectedAndNothingCharged() {
        assertThrows(InvalidBookingException.class,
                () -> cinemaTicketsServiceUnderTest.purchaseTickets(1234L, new TicketRequest(CHILD, 1)));

        verifyNoInteractions(mockPaymentService, mockSeatReservationService);
    }
}
