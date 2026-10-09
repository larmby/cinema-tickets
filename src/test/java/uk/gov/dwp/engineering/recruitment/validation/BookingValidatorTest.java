package uk.gov.dwp.engineering.recruitment.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.ADULT;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.CHILD;
import static uk.gov.dwp.engineering.recruitment.domain.TicketType.INFANT;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.dwp.engineering.recruitment.domain.TicketRequest;
import uk.gov.dwp.engineering.recruitment.exception.InvalidBookingException;

class BookingValidatorTest {

  private static final long VALID_ACCOUNT_ID = 1234L;

  private final BookingValidator bookingValidator = new BookingValidator();

  @Test
  void givenNullAccountId_whenValidated_thenRejected() {
    final InvalidBookingException exception = assertThrows(InvalidBookingException.class,
        () -> bookingValidator.validate(null, new TicketRequest(ADULT, 1)));

    assertEquals("Account id must be provided", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
  void givenNonPositiveAccountId_whenValidated_thenRejected(final long accountId) {
    final InvalidBookingException exception = assertThrows(InvalidBookingException.class,
        () -> bookingValidator.validate(accountId, new TicketRequest(ADULT, 1)));

    assertEquals("Account id must be greater than zero", exception.getMessage());
  }

  @Test
  void givenNullTicketRequests_whenValidated_thenRejected() {
    final InvalidBookingException exception = assertThrows(InvalidBookingException.class,
        () -> bookingValidator.validate(VALID_ACCOUNT_ID, (TicketRequest[]) null));

    assertEquals("Ticket requests must be provided", exception.getMessage());
  }

  @Test
  void givenNoTicketRequests_whenValidated_thenRejected() {
    final InvalidBookingException exception = assertThrows(InvalidBookingException.class,
        () -> bookingValidator.validate(VALID_ACCOUNT_ID));

    assertEquals("At least one ticket request must be provided", exception.getMessage());
  }

  @Test
  void givenNullTicketRequestEntry_whenValidated_thenRejected() {
    final InvalidBookingException exception = assertThrows(InvalidBookingException.class,
        () -> bookingValidator.validate(VALID_ACCOUNT_ID, new TicketRequest(ADULT, 1), null));

    assertEquals("Ticket requests must not contain null entries", exception.getMessage());
  }

  @Test
  void givenTicketRequestWithNullType_whenValidated_thenRejected() {
    final InvalidBookingException exception = assertThrows(InvalidBookingException.class,
        () -> bookingValidator.validate(VALID_ACCOUNT_ID,
            new TicketRequest(ADULT, 1), new TicketRequest(null, 1)));

    assertEquals("Each ticket request must specify a ticket type", exception.getMessage());
  }

  static Stream<Arguments> invalidTicketCountsAndBusinessRules() {
    return Stream.of(
        arguments("negative count",
            new TicketRequest[] {new TicketRequest(ADULT, 1), new TicketRequest(CHILD, -1)},
            "Ticket count must not be negative"),
        arguments("zero tickets in total",
            new TicketRequest[] {new TicketRequest(ADULT, 0)},
            "At least one ticket must be purchased"),
        arguments("more than 25 tickets",
            new TicketRequest[] {new TicketRequest(ADULT, 26)},
            "The number of tickets requested (26) exceeds the maximum 25 for a single purchase."),
        arguments("infants count towards the 25 ticket limit",
            new TicketRequest[] {new TicketRequest(ADULT, 25), new TicketRequest(INFANT, 1)},
            "The number of tickets requested (26) exceeds the maximum 25 for a single purchase."),
        arguments("int overflow cannot bypass the limit",
            new TicketRequest[] {
                new TicketRequest(ADULT, Integer.MAX_VALUE),
                new TicketRequest(ADULT, Integer.MAX_VALUE)},
            "The number of tickets requested (4294967294) exceeds the maximum 25 for a single purchase."),

        arguments("child without adult",
            new TicketRequest[] {new TicketRequest(CHILD, 1)},
            "Child and infant tickets cannot be purchased without purchasing an adult ticket"),
        arguments("infant without adult",
            new TicketRequest[] {new TicketRequest(INFANT, 1)},
            "Child and infant tickets cannot be purchased without purchasing an adult ticket"),
        arguments("zero adults declared alongside a child",
            new TicketRequest[] {new TicketRequest(ADULT, 0), new TicketRequest(CHILD, 1)},
            "Child and infant tickets cannot be purchased without purchasing an adult ticket"),
        arguments("more infants than adults",
            new TicketRequest[] {new TicketRequest(ADULT, 1), new TicketRequest(INFANT, 2)},
            "Each infant must be accompanied by their own adult"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidTicketCountsAndBusinessRules")
  void givenInvalidTicketRequests_whenValidated_thenRejected(
      final String description, final TicketRequest[] ticketRequests, final String expectedMessage) {
    final InvalidBookingException exception = assertThrows(InvalidBookingException.class,
        () -> bookingValidator.validate(VALID_ACCOUNT_ID, ticketRequests));

    assertEquals(expectedMessage, exception.getMessage());
  }

  static Stream<Arguments> validTicketRequests() {
    return Stream.of(
        arguments("single adult",
            new TicketRequest[] {new TicketRequest(ADULT, 1)}),
        arguments("exactly 25 tickets",
            new TicketRequest[] {new TicketRequest(ADULT, 25)}),
        arguments("exactly 25 tickets including infants",
            new TicketRequest[] {new TicketRequest(ADULT, 13), new TicketRequest(INFANT, 12)}),
        arguments("one infant per adult",
            new TicketRequest[] {new TicketRequest(ADULT, 2), new TicketRequest(INFANT, 2)}),
        arguments("one adult with many children",
            new TicketRequest[] {new TicketRequest(ADULT, 1), new TicketRequest(CHILD, 24)}),
        arguments("zero count entry alongside valid tickets",
            new TicketRequest[] {new TicketRequest(ADULT, 1), new TicketRequest(CHILD, 0)}),
        arguments("same ticket type repeated",
            new TicketRequest[] {new TicketRequest(ADULT, 1), new TicketRequest(ADULT, 2)}));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validTicketRequests")
  void givenValidTicketRequests_whenValidated_thenReturnedAsList(
      final String description, final TicketRequest[] ticketRequests) {
    final List<TicketRequest> validated =
        assertDoesNotThrow(() -> bookingValidator.validate(VALID_ACCOUNT_ID, ticketRequests));

    assertEquals(List.of(ticketRequests), validated);
  }
}
