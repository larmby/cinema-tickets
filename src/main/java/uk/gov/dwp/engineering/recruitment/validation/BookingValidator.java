package uk.gov.dwp.engineering.recruitment.validation;

import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

@Component
public class BookingValidator {

  static final int MAX_TICKETS_PER_BOOKING = 25;

  public List<TicketRequest> validate(final Long accountId, final TicketRequest... ticketRequests) {
    validateAccountId(accountId);
    validateTicketRequestsStructure(ticketRequests);

    final List<TicketRequest> ticketRequestsList = List.of(ticketRequests);

    validateTicketCounts(ticketRequestsList);
    validateBusinessRules(ticketRequestsList);

    return ticketRequestsList;
  }

  private void validateAccountId(final Long accountId) {
    if (accountId == null) {
      throw new InvalidBookingException("Account id must be provided");
    }

    if (accountId <= 0) {
      throw new InvalidBookingException("Account id must be greater than zero");
    }
  }

  private void validateTicketRequestsStructure(final TicketRequest... ticketRequests) {
    if (ticketRequests == null) {
      throw new InvalidBookingException("Ticket requests must be provided");
    }

    if (ticketRequests.length == 0) {
      throw new InvalidBookingException("At least one ticket request must be provided");
    }

    for (final TicketRequest ticketRequest : ticketRequests) {
      if (ticketRequest == null) {
        throw new InvalidBookingException("Ticket requests must not contain null entries");
      }

      if (ticketRequest.type() == null) {
        throw new InvalidBookingException("Each ticket request must specify a ticket type");
      }
    }
  }

  private void validateTicketCounts(final List<TicketRequest> ticketRequests) {
    if (ticketRequests.stream().anyMatch(ticketRequest -> ticketRequest.ticketCount() < 0)) {
      throw new InvalidBookingException("Ticket count must not be negative");
    }

    final long totalTickets = totalTickets(ticketRequests);

    if (totalTickets == 0) {
      throw new InvalidBookingException("At least one ticket must be purchased");
    }

    if (totalTickets > MAX_TICKETS_PER_BOOKING) {
      throw new InvalidBookingException(
          "The number of tickets requested (" + totalTickets + ") exceeds the maximum "
              + MAX_TICKETS_PER_BOOKING + " for a single purchase.");
    }
  }

  private void validateBusinessRules(final List<TicketRequest> ticketRequests) {
    final long adults = countOf(ticketRequests, ADULT);

    if (adults == 0) {
      throw new InvalidBookingException("Child and infant tickets cannot be purchased without purchasing an adult ticket");
    }

    if (countOf(ticketRequests, INFANT) > adults) {
      throw new InvalidBookingException("Each infant must be accompanied by their own adult");
    }
  }

  private static long totalTickets(final List<TicketRequest> ticketRequests) {
    return ticketRequests.stream()
        .mapToLong(TicketRequest::ticketCount)
        .sum();
  }

  private static long countOf(final List<TicketRequest> ticketRequests, final TicketType type) {
    return ticketRequests.stream()
        .filter(ticketRequest -> ticketRequest.type() == type)
        .mapToLong(TicketRequest::ticketCount)
        .sum();
  }
}
