package uk.gov.dwp.engineering.recruitment.pricing;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

class TicketCostCalculatorTest {

  private final TicketCostCalculator calculator = new TicketCostCalculator(new TicketPricing(Map.of(
      ADULT, new BigDecimal("25.99"),
      CHILD, new BigDecimal("17.50"),
      INFANT, new BigDecimal("0.00"))));

  static Stream<Arguments> bookingRequests() {
    return Stream.of(
        arguments(List.of(
            new TicketRequest(ADULT, 1)), "25.99"),
        arguments(List.of(
            new TicketRequest(ADULT, 2)), "51.98"),
        arguments(List.of(
            new TicketRequest(ADULT, 2),
            new TicketRequest(CHILD, 1)), "69.48"),
        arguments(List.of(
            new TicketRequest(ADULT, 1),
            new TicketRequest(INFANT, 1)), "25.99"),
        arguments(List.of(
            new TicketRequest(ADULT, 2),
            new TicketRequest(CHILD, 3),
            new TicketRequest(INFANT, 2)),
            "104.48"),
        arguments(List.of(
            new TicketRequest(ADULT, 1),
            new TicketRequest(ADULT, 2)), "77.97"),
        arguments(List.of(
            new TicketRequest(ADULT, 25)), "649.75"));
  }

  @ParameterizedTest
  @MethodSource("bookingRequests")
  void givenTicketRequests_whenTotalCalculated_thenExpectedResultsReturned(final List<TicketRequest> requests, final String expectedTotal) {
    assertEquals(new BigDecimal(expectedTotal), calculator.calculateTotalAmount(requests));
  }
}
