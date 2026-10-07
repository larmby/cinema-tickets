package uk.gov.dwp.engineering.recruitment.pricing;

import org.junit.jupiter.api.Test;
import uk.gov.dwp.engineering.recruitment.domain.TicketType;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

public class TicketPricingValidationTest {

  @Test
  void givenNoPrices_whenConstructed_thenErrorIsThrown() {
    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> new TicketPricing(null));
    assertEquals("No ticket prices configured", exception.getMessage());
  }

  @Test
  void givenMissingTicketType_whenConstructed_thenErrorIsThrown() {
    final Map<TicketType, BigDecimal> prices = Map.of(
        ADULT, new BigDecimal("25.99"),
        CHILD, new BigDecimal("17.50"));

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> new TicketPricing(prices));
    assertEquals("No price configured for ticket type INFANT", exception.getMessage());
  }

  @Test
  void givenEmptyPrices_whenConstructed_thenErrorIsThrown() {
    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> new TicketPricing(Map.of()));
    assertEquals("No price configured for ticket type ADULT", exception.getMessage());
  }

  @Test
  void givenNullPrice_whenConstructed_thenErrorIsThrown() {
    final Map<TicketType, BigDecimal> prices = new EnumMap<>(TicketType.class);
    prices.put(ADULT, new BigDecimal("25.99"));
    prices.put(CHILD, null);
    prices.put(INFANT, new BigDecimal("0.00"));

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> new TicketPricing(prices));
    assertEquals("Invalid price configured for ticket type CHILD: null", exception.getMessage());
  }

  @Test
  void givenNegativePrice_whenConstructed_thenErrorIsThrown() {
    final Map<TicketType, BigDecimal> prices = Map.of(
        ADULT, new BigDecimal("-25.99"),
        CHILD, new BigDecimal("17.50"),
        INFANT, new BigDecimal("0.00"));

    final IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> new TicketPricing(prices));
    assertEquals("Invalid price configured for ticket type ADULT: -25.99", exception.getMessage());
  }

  @Test
  void givenCorrectlyConstructedTicketType_whenFetched_thenExpectedValueReturned() {
    final Map<TicketType, BigDecimal> prices = Map.of(
        ADULT, new BigDecimal("25.99"),
        CHILD, new BigDecimal("17.50"),
        INFANT, new BigDecimal("0.00"));

    final TicketPricing ticketPricing = new TicketPricing(prices);

    assertEquals(new BigDecimal("25.99"), ticketPricing.priceFor(ADULT));
    assertEquals(new BigDecimal("17.50"), ticketPricing.priceFor(CHILD));
    assertEquals(new BigDecimal("0.00"), ticketPricing.priceFor(INFANT));
  }
}
