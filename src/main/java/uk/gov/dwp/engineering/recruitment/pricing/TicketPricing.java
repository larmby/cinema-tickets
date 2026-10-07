package uk.gov.dwp.engineering.recruitment.pricing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

import java.math.BigDecimal;
import java.util.Map;

@ConfigurationProperties(prefix = "ticket")
public record TicketPricing(Map<TicketType, BigDecimal> prices) {

  public TicketPricing {
    if (prices == null) {
      throw new IllegalStateException("No ticket prices configured");
    }
    for (final TicketType type : TicketType.values()) {
      if (!prices.containsKey(type)) {
        throw new IllegalStateException("No price configured for ticket type " + type);
      }
      final BigDecimal price = prices.get(type);
      if (price == null || price.signum() < 0) {
        throw new IllegalStateException("Invalid price configured for ticket type " + type + ": " + price);
      }
    }
    prices = Map.copyOf(prices);
  }

  public BigDecimal priceFor(final TicketType type) {
    return prices.get(type);
  }
}
