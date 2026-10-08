package uk.gov.dwp.engineering.recruitment;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.PaymentException;
import uk.gov.dwp.engineering.recruitment.exception.SeatReservationException;
import uk.gov.dwp.engineering.recruitment.pricing.TicketCostCalculator;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import uk.gov.dwp.engineering.recruitment.validation.BookingValidator;

import java.math.BigDecimal;
import java.util.List;

import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private final PaymentService paymentService;

  private final SeatReservationService seatReservationService;

  private final BookingValidator bookingValidator;

  private final TicketCostCalculator ticketCostCalculator;

  public CinemaTicketsServiceImpl(final PaymentService paymentService,
                                  final SeatReservationService seatReservationService,
                                  final BookingValidator bookingValidator,
                                  final TicketCostCalculator ticketCostCalculator) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
    this.bookingValidator = bookingValidator;
    this.ticketCostCalculator = ticketCostCalculator;
  }

  @Override
  public BookingConfirmation purchaseTickets(final Long accountId,
      final TicketRequest... ticketRequests)
      throws InvalidBookingException {

    final List<TicketRequest> validTicketRequests = bookingValidator.validate(accountId, ticketRequests);

    final BigDecimal totalAmount = ticketCostCalculator.calculateTotalAmount(validTicketRequests);
    final long numberOfSeats = seatCount(validTicketRequests);

    makePayment(accountId, totalAmount);
    reserveSeats(accountId, numberOfSeats);

    return new BookingConfirmation(accountId, totalAmount, numberOfSeats);
  }

  private long seatCount(final List<TicketRequest> ticketRequests) {
    return ticketRequests.stream()
        .filter(ticketRequest -> ticketRequest.type() != INFANT)
        .mapToLong(TicketRequest::ticketCount)
        .sum();
  }

  private void makePayment(final Long accountId, final BigDecimal totalAmount) {

    final ResponseEntity<String> paymentResponse;
    try {
      paymentResponse = paymentService.debitAccount(accountId, totalAmount);
    } catch (final RuntimeException e) {
      throw new PaymentException("Payment request failed for account " + accountId, e);
    }

    if (paymentResponse == null || !paymentResponse.getStatusCode().is2xxSuccessful()) {
      throw new PaymentException("Payment failed with status " +
          (paymentResponse == null ? "unknown" : paymentResponse.getStatusCode()));
    }
  }

  private void reserveSeats(final Long accountId, final long numberOfSeats) {

    final ResponseEntity<String> seatReservationResponse;
    try {
      seatReservationResponse = seatReservationService.reserveSeats(accountId, numberOfSeats);
    } catch (final RuntimeException e) {
      throw new SeatReservationException("Seat reservation failed for account " + accountId, e);
    }

    if (seatReservationResponse == null || !seatReservationResponse.getStatusCode().is2xxSuccessful()) {
      throw new SeatReservationException("Seat reservation failed with status " +
          (seatReservationResponse == null ? "unknown" : seatReservationResponse.getStatusCode()));
    }
  }
}
