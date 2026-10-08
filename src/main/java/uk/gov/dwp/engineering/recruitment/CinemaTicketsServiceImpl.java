package uk.gov.dwp.engineering.recruitment;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.exception.PaymentException;
import uk.gov.dwp.engineering.recruitment.pricing.TicketCostCalculator;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;
import uk.gov.dwp.engineering.recruitment.validation.BookingValidator;

import java.math.BigDecimal;
import java.util.List;

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

    final BigDecimal totalCost = ticketCostCalculator.calculateTotalAmount(validTicketRequests);

    final ResponseEntity<String> paymentResponse = paymentService.debitAccount(accountId, totalCost);
    if (!paymentResponse.getStatusCode().is2xxSuccessful()) {
      throw new PaymentException("Payment failed with status " + paymentResponse.getStatusCode());
    }

    throw new InvalidBookingException("Not implemented yet");
  }
}
