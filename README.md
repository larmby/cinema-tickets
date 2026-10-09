# Cinema Tickets Code Test

## Objective

To assess your ability to build a simple API.

The scenario and requirements will be provided separately.

## Approach

1.  Prices are externalised to `application.properties` and bound via `TicketPricing` in a separate `pricing` package, so the `domain` package stays unmodified as required. The application fails to start if any ticket type has a missing, empty or negative price.
2.  The total cost is calculated by `TicketCostCalculator`, a small stateless class that is unit tested independently of the service.
3.  All monetary values use `BigDecimal` rather than `double` to avoid floating point rounding errors.
4.  The OpenAPI spec (`cinema-tickets.yaml`) used different field names (`tickets` and `count` instead of `ticketRequests` and `ticketCount`) to the field names in the code, so the OpenAPI spec has been corrected as the brief stated the domain code was not to be changed.
5.  `BookingConfirmation` has been extended with `totalAmount` and `seatsReserved`. The original single-argument constructor is retained so the change is non-breaking.
6.  Every request is fully validated before any call is made to the `PaymentService` or `SeatReservationService`, so a rejected request never results in a payment or a seat reservation.
7.  Invalid requests are rejected with `400 Bad Request` and an RFC 9457 Problem Detail body describing the reason.

## Assumptions

In addition to the assumptions given in the brief:

1.  **Infants count towards the 25 ticket limit.** The brief states a maximum of 25 _tickets_; an infant ticket is still a ticket, even though it is free and is not allocated a seat.
2.  **Each infant requires their own adult.** Infants sit on an adult's lap, and an adult has only one lap, so a booking with more infants than adults is rejected.
3.  **A ticket request with a count of zero is allowed** (the OpenAPI spec specifies `minimum: 0`) and is treated as no tickets of that type. A booking must still contain at least one ticket in total.
4.  **A missing or null `ticketCount` is treated as zero.** `ticketCount` is a primitive `int` in the domain record, which cannot be changed, so it defaults to zero when the JSON is parsed.
5.  **The same ticket type may appear more than once in a booking**, e.g. `ADULT × 1` and `ADULT × 2`. The counts are combined.
6.  **There is no maximum ratio of children to adults.** The brief does not specify one, so none is enforced; e.g. one adult with 24 children is valid.
7.  **Unknown JSON fields are ignored** rather than rejected, so clients sending additional fields are not broken.

## Invalid purchase requests

The following requests are rejected. No payment is taken and no seats are reserved.

### Account

1.  The account id is missing or null.
2.  The account id is zero or negative.

### Request structure

1.  The ticket requests are missing or null.
2.  The ticket requests are empty.
3.  The ticket requests contain a null entry.
4.  A ticket request has a missing or null ticket type.
5.  A ticket request has an unknown ticket type, e.g. `PENSIONER`. This is rejected when the JSON is parsed, before reaching the service.
6.  The request body is malformed JSON or contains values of the wrong type, e.g. `"ticketCount": "two"`. This is also rejected when the JSON is parsed.

### Ticket counts

1.  A ticket request has a negative ticket count.
2.  The total number of tickets is zero.
3.  The total number of tickets is greater than 25 (including infants).
4.  The total number of tickets is too large to be represented, e.g. several requests each for `Integer.MAX_VALUE` tickets. The total is calculated without integer overflow, so this cannot bypass the 25 ticket limit.

### Business rules

1.  CHILD or INFANT tickets are requested without at least one ADULT ticket.
2.  More INFANT tickets are requested than ADULT tickets.

## Known issues

1.  `PaymentServiceImpl` and `SeatReservationServiceImpl` read their `@Value` URL field inside the constructor. Spring injects `@Value` fields only after the constructor has run, so the `RestClient` is built with a `null` base URL. This has no effect while the methods are stubs, but would need fixing (by injecting the URL as a constructor parameter) before the real HTTP calls are implemented. These classes have been left unchanged, as the brief treats them as external providers.
