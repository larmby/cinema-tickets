package uk.gov.dwp.engineering.recruitment.pricing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;

class TicketPricingBindingTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(Config.class);

  @EnableConfigurationProperties(TicketPricing.class)
  static class Config {
  }

  @Test
  void givenAllPricesConfigured_whenContextStarts_thenPricesAreBound() {
    contextRunner
        .withPropertyValues(
            "ticket.prices.ADULT=25.99",
            "ticket.prices.CHILD=17.50",
            "ticket.prices.INFANT=0.00")
        .run(context -> {
          assertThat(context).hasNotFailed();
          assertThat(context.getBean(TicketPricing.class).priceFor(CHILD))
              .isEqualTo(new BigDecimal("17.50"));
        });
  }

  @Test
  void givenNoPricesInProperties_whenContextStarts_thenStartupFails() {
    contextRunner.run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure())
          .rootCause()
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("No ticket prices configured");
    });
  }

  @Test
  void givenTicketTypeMissingFromProperties_whenContextStarts_thenStartupFails() {
    contextRunner
        .withPropertyValues(
            "ticket.prices.ADULT=25.99",
            "ticket.prices.CHILD=17.50")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .rootCause()
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("No price configured for ticket type INFANT");
        });
  }

  @Test
  void givenEmptyPriceInProperties_whenContextStarts_thenStartupFails() {
    contextRunner
        .withPropertyValues(
            "ticket.prices.ADULT=25.99",
            "ticket.prices.CHILD=",
            "ticket.prices.INFANT=0.00")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .rootCause()
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("No price configured for ticket type CHILD");
        });
  }

  @Test
  void givenNegativePriceInProperties_whenContextStarts_thenStartupFails() {
    contextRunner
        .withPropertyValues(
            "ticket.prices.ADULT=-25.99",
            "ticket.prices.CHILD=17.50",
            "ticket.prices.INFANT=0.00")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .rootCause()
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("Invalid price configured for ticket type ADULT: -25.99");
        });
  }
}
