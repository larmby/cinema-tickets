package uk.gov.dwp.engineering.recruitment.pricing;

import org.springframework.stereotype.Component;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

import java.math.BigDecimal;
import java.util.List;

@Component
public class TicketCostCalculator {

  private final TicketPricing ticketPricing;

  public TicketCostCalculator(final TicketPricing ticketPricing) {
    this.ticketPricing = ticketPricing;
  }

  public BigDecimal calculateTotalAmount(final List<TicketRequest> ticketRequests) {
    return ticketRequests.stream()
        .map(this::costFor)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal costFor(final TicketRequest request) {
    return ticketPricing.priceFor(request.type())
        .multiply(BigDecimal.valueOf(request.ticketCount()));
  }
}
