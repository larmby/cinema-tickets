package uk.gov.dwp.engineering.recruitment;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.dwp.engineering.recruitment.pricing.TicketCostCalculator;
import uk.gov.dwp.engineering.recruitment.thirdparty.PaymentService;
import uk.gov.dwp.engineering.recruitment.thirdparty.SeatReservationService;

@SpringBootTest
class CinemaTicketsControllerTest {

  private static final String VALID_BOOKING =
      "{\"accountId\": 1234, \"ticketRequests\": [{\"type\": \"ADULT\", \"ticketCount\": 1}]}";

  private static final String THIRD_PARTY_FAILURE_DETAIL =
      "The booking could not be completed. Please try again later.";

  @Autowired
  private WebApplicationContext webApplicationContext;

  @MockitoBean
  private PaymentService mockPaymentService;

  @MockitoBean
  private SeatReservationService mockSeatReservationService;

  @MockitoSpyBean
  private TicketCostCalculator ticketCostCalculator;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  private ResultActions postBooking(final String json) throws Exception {
    return mockMvc.perform(post("/cinema/bookings")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json));
  }

  @Test
  void givenValidBooking_whenPosted_thenCreatedWithTotalAndSeats() throws Exception {
    when(mockPaymentService.debitAccount(anyLong(), any())).thenReturn(ResponseEntity.ok("OK"));
    when(mockSeatReservationService.reserveSeats(anyLong(), anyLong())).thenReturn(ResponseEntity.ok("OK"));

    postBooking("""
        {"accountId": 1234, "ticketRequests": [
          {"type": "ADULT", "ticketCount": 2},
          {"type": "CHILD", "ticketCount": 1},
          {"type": "INFANT", "ticketCount": 1}
        ]}""")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accountId").value(1234))
        .andExpect(jsonPath("$.totalAmount").value(69.48))
        .andExpect(jsonPath("$.seatsReserved").value(3));

    verify(mockPaymentService).debitAccount(1234L, new BigDecimal("69.48"));
    verify(mockSeatReservationService).reserveSeats(1234L, 3L);
  }

  static Stream<Arguments> invalidBookings() {
    return Stream.of(
        arguments("{\"ticketRequests\": [{\"type\": \"ADULT\", \"ticketCount\": 1}]}",
            "Account id must be provided"),
        arguments("{\"accountId\": 0, \"ticketRequests\": [{\"type\": \"ADULT\", \"ticketCount\": 1}]}",
            "Account id must be greater than zero"),
        arguments("{\"accountId\": 1234}",
            "Ticket requests must be provided"),
        arguments("{\"accountId\": 1234, \"ticketRequests\": []}",
            "At least one ticket request must be provided"),
        arguments("{\"accountId\": 1234, \"ticketRequests\": [null]}",
            "Ticket requests must not contain null entries"),
        arguments("{\"accountId\": 1234, \"ticketRequests\": [{\"type\": null, \"ticketCount\": 1}]}",
            "Each ticket request must specify a ticket type"),
        arguments("{\"accountId\": 1234, \"ticketRequests\": [{\"type\": \"ADULT\", \"ticketCount\": -1}]}",
            "Ticket count must not be negative"),
        arguments("{\"accountId\": 1234, \"ticketRequests\": [{\"type\": \"ADULT\", \"ticketCount\": 0}]}",
            "At least one ticket must be purchased"),
        arguments("{\"accountId\": 1234, \"ticketRequests\": [{\"type\": \"ADULT\", \"ticketCount\": 26}]}",
            "The number of tickets requested (26) exceeds the maximum 25 for a single purchase."),
        arguments("{\"accountId\": 1234, \"ticketRequests\": [{\"type\": \"CHILD\", \"ticketCount\": 1}]}",
            "Child and infant tickets cannot be purchased without purchasing an adult ticket"),
        arguments("{\"accountId\": 1234, \"ticketRequests\": [{\"type\": \"INFANT\", \"ticketCount\": 1}]}",
            "Child and infant tickets cannot be purchased without purchasing an adult ticket"),
        arguments("{\"accountId\": 1234, \"ticketRequests\": ["
                + "{\"type\": \"ADULT\", \"ticketCount\": 1}, {\"type\": \"INFANT\", \"ticketCount\": 2}]}",
            "Each infant must be accompanied by their own adult"));
  }

  @ParameterizedTest
  @MethodSource("invalidBookings")
  void givenInvalidBooking_whenPosted_thenBadRequestAndNothingCharged(
      final String json, final String expectedDetail) throws Exception {
    postBooking(json)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value(expectedDetail));

    verifyNoInteractions(mockPaymentService, mockSeatReservationService);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{\"accountId\": 1234, \"ticketRequests\": [{\"type\": \"PENSIONER\", \"ticketCount\": 1}]}",
      "{\"accountId\": 1234, \"ticketRequests\": [{\"type\": \"ADULT\", \"ticketCount\": \"two\"}]}",
      "{\"accountId\": 1234, \"ticketRequests\": [",
      ""})
  void givenUnreadableRequest_whenPosted_thenBadRequestAndNothingCharged(final String json)
      throws Exception {
    postBooking(json).andExpect(status().isBadRequest());

    verifyNoInteractions(mockPaymentService, mockSeatReservationService);
  }

  @Test
  void givenPaymentServiceFails_whenPosted_thenBadGatewayAndNoSeatsReserved() throws Exception {
    when(mockPaymentService.debitAccount(anyLong(), any()))
        .thenThrow(new ResourceAccessException("Read timed out"));

    postBooking(VALID_BOOKING)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value(THIRD_PARTY_FAILURE_DETAIL));

    verifyNoInteractions(mockSeatReservationService);
  }

  @Test
  void givenSeatReservationServiceFails_whenPosted_thenBadGateway() throws Exception {
    when(mockPaymentService.debitAccount(anyLong(), any())).thenReturn(ResponseEntity.ok("OK"));
    when(mockSeatReservationService.reserveSeats(anyLong(), anyLong()))
        .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error"));

    postBooking(VALID_BOOKING)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value(THIRD_PARTY_FAILURE_DETAIL));
  }

  @Test
  void givenUnexpectedError_whenPosted_thenInternalServerErrorWithoutDetails() throws Exception {
    doThrow(new IllegalStateException("Something internal went wrong"))
        .when(ticketCostCalculator).calculateTotalAmount(any());

    postBooking(VALID_BOOKING)
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));

    verifyNoInteractions(mockPaymentService, mockSeatReservationService);
  }
}
