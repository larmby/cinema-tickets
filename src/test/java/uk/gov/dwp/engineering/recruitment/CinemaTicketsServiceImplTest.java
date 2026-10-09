package uk.gov.dwp.engineering.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.exception.PaymentException;
import uk.gov.dwp.engineering.recruitment.exception.SeatReservationException;
import uk.gov.dwp.engineering.recruitment.pricing.TicketCostCalculator;
import uk.gov.dwp.engineering.recruitment.pricing.TicketPricing;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import uk.gov.dwp.engineering.recruitment.validation.BookingValidator;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

@ExtendWith(MockitoExtension.class)
class CinemaTicketsServiceImplTest {

    private static final long ACCOUNT_ID = 1234L;

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
    void givenValidBooking_whenPurchased_thenCorrectAmountDebitedAndSeatsReserved() {
        // 2 x 25.99 + 1 x 17.50 + 1 x 0.00 = 69.48. The infant is not allocated a seat.
        final BigDecimal expectedAmount = new BigDecimal("69.48");
        final long expectedSeats = 3L;
        when(mockPaymentService.debitAccount(ACCOUNT_ID, expectedAmount)).thenReturn(ResponseEntity.ok("OK"));
        when(mockSeatReservationService.reserveSeats(ACCOUNT_ID, expectedSeats)).thenReturn(ResponseEntity.ok("OK"));

        final BookingConfirmation confirmation = cinemaTicketsServiceUnderTest.purchaseTickets(ACCOUNT_ID,
                new TicketRequest(ADULT, 2), new TicketRequest(CHILD, 1), new TicketRequest(INFANT, 1));

        assertEquals(new BookingConfirmation(ACCOUNT_ID, expectedAmount, expectedSeats), confirmation);

        final InOrder inOrder = inOrder(mockPaymentService, mockSeatReservationService);
        inOrder.verify(mockPaymentService).debitAccount(ACCOUNT_ID, expectedAmount);
        inOrder.verify(mockSeatReservationService).reserveSeats(ACCOUNT_ID, expectedSeats);
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
                () -> cinemaTicketsServiceUnderTest.purchaseTickets(ACCOUNT_ID, new TicketRequest(CHILD, 1)));

        verifyNoInteractions(mockPaymentService, mockSeatReservationService);
    }

    @Test
    void givenPaymentServiceThrows_whenPurchased_thenPaymentExceptionAndNoSeatsReserved() {
        final ResourceAccessException cause = new ResourceAccessException("Read timed out");
        when(mockPaymentService.debitAccount(anyLong(), any())).thenThrow(cause);

        final PaymentException exception = assertThrows(PaymentException.class,
                () -> cinemaTicketsServiceUnderTest.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1)));

        assertSame(cause, exception.getCause());
        verifyNoInteractions(mockSeatReservationService);
    }

    @Test
    void givenPaymentServiceReturnsError_whenPurchased_thenPaymentExceptionAndNoSeatsReserved() {
        when(mockPaymentService.debitAccount(anyLong(), any()))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error"));

        final PaymentException exception = assertThrows(PaymentException.class,
                () -> cinemaTicketsServiceUnderTest.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1)));

        assertEquals("Payment failed with status 500 INTERNAL_SERVER_ERROR", exception.getMessage());
        verifyNoInteractions(mockSeatReservationService);
    }

    @Test
    void givenPaymentServiceReturnsNull_whenPurchased_thenPaymentExceptionAndNoSeatsReserved() {
        when(mockPaymentService.debitAccount(anyLong(), any())).thenReturn(null);

        final PaymentException exception = assertThrows(PaymentException.class,
                () -> cinemaTicketsServiceUnderTest.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1)));

        assertEquals("Payment failed with status unknown", exception.getMessage());
        verifyNoInteractions(mockSeatReservationService);
    }

    @Test
    void givenSeatReservationServiceThrows_whenPurchased_thenSeatReservationException() {
        final ResourceAccessException cause = new ResourceAccessException("Connection refused");
        when(mockPaymentService.debitAccount(anyLong(), any())).thenReturn(ResponseEntity.ok("OK"));
        when(mockSeatReservationService.reserveSeats(anyLong(), anyLong())).thenThrow(cause);

        final SeatReservationException exception = assertThrows(SeatReservationException.class,
                () -> cinemaTicketsServiceUnderTest.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1)));

        assertSame(cause, exception.getCause());
    }

    @Test
    void givenSeatReservationServiceReturnsError_whenPurchased_thenSeatReservationException() {
        when(mockPaymentService.debitAccount(anyLong(), any())).thenReturn(ResponseEntity.ok("OK"));
        when(mockSeatReservationService.reserveSeats(anyLong(), anyLong()))
                .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error"));

        final SeatReservationException exception = assertThrows(SeatReservationException.class,
                () -> cinemaTicketsServiceUnderTest.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1)));

        assertEquals("Seat reservation failed with status 503 SERVICE_UNAVAILABLE", exception.getMessage());
    }

    @Test
    void givenSeatReservationServiceReturnsNull_whenPurchased_thenSeatReservationException() {
        when(mockPaymentService.debitAccount(anyLong(), any())).thenReturn(ResponseEntity.ok("OK"));
        when(mockSeatReservationService.reserveSeats(anyLong(), anyLong())).thenReturn(null);

        final SeatReservationException exception = assertThrows(SeatReservationException.class,
                () -> cinemaTicketsServiceUnderTest.purchaseTickets(ACCOUNT_ID, new TicketRequest(ADULT, 1)));

        assertEquals("Seat reservation failed with status unknown", exception.getMessage());
    }
}
