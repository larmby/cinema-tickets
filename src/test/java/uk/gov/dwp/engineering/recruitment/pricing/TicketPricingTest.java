package uk.gov.dwp.engineering.recruitment.pricing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

@SpringBootTest(classes = TicketPricingTest.Config.class)
public class TicketPricingTest {

  @EnableConfigurationProperties(TicketPricing.class)
  static class Config {
  }

  @Autowired
  private TicketPricing ticketPricing;

  @Test
  void givenApplicationProperties_whenBound_thenPricesMatchBrief() {
    assertEquals(new BigDecimal("25.99"), ticketPricing.priceFor(ADULT));
    assertEquals(new BigDecimal("17.50"), ticketPricing.priceFor(CHILD));
    assertEquals(new BigDecimal("0.00"), ticketPricing.priceFor(INFANT));
  }
}
