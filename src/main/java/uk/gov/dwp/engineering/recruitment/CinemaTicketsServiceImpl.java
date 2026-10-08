package uk.gov.dwp.engineering.recruitment;

import org.springframework.stereotype.Service;
import uk.gov.dwp.engineering.recruitment.domain.BookingConfirmation;
import uk.gov.dwp.engineering.recruitment.pricing.TicketCostCalculator;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
public class CinemaTicketsServiceImpl implements CinemaTicketsService {

  private final PaymentService paymentService;

  private final SeatReservationService seatReservationService;

  private final TicketCostCalculator ticketCostCalculator;

  public CinemaTicketsServiceImpl(final PaymentService paymentService,
                                  final SeatReservationService seatReservationService,
                                  final  TicketCostCalculator ticketCostCalculator) {
    this.paymentService = paymentService;
    this.seatReservationService = seatReservationService;
    this.ticketCostCalculator = ticketCostCalculator;
  }

  @Override
  public BookingConfirmation purchaseTickets(final Long accountId,
      final TicketRequest... ticketRequests)
      throws InvalidBookingException {

    final List<TicketRequest> ticketRequestList = Arrays.asList(ticketRequests);

    final BigDecimal totalCost = ticketCostCalculator.calculateTotalAmount(ticketRequestList);



    throw new InvalidBookingException("Not implemented yet");
  }


}
