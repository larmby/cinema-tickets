# Cinema Tickets Code Test

## Task

Provide a working implementation of a Cinema Tickets Booking Service.

## Running the application

Requires Java 21 and Maven.

```shell
mvn test              # run all tests
mvn spring-boot:run   # start the application on port 8080
```

Example request for a booking for two adults, one child and one infant:

```shell
curl -X POST http://localhost:8080/cinema/bookings \
  -H 'Content-Type: application/json' \
  -d '{"accountId": 1234, "ticketRequests": [
        {"type": "ADULT", "ticketCount": 2},
        {"type": "CHILD", "ticketCount": 1},
        {"type": "INFANT", "ticketCount": 1}]}'
```

Successful response:

```json
{"accountId": 1234, "totalAmount": 69.48, "seatsReserved": 3}
```

An invalid booking, e.g. a child without an adult, returns `400 Bad Request`:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Child and infant tickets cannot be purchased without purchasing an adult ticket",
  "instance": "/cinema/bookings"
}
```

## Approach

`CinemaTicketsServiceImpl` carries out the booking in four steps, each delegated to a small class with a single responsibility:

1.  Validate the account id and ticket requests with the `BookingValidator`. Every rule is checked before any other step so a rejected request never results in a payment or a seat reservation.
2.  Calculate the total cost with `TicketCostCalculator` using the prices in `TicketPricing` (configured at startup with values from `application.properties`). Infants are free.
3.  Take payment by passing account and total amount to the `PaymentService` Rest Client.
4.  Reserve seats for adults and children through the `SeatReservationService`. Infants sit on an adult's lap and are not allocated a seat.

It then returns a `BookingConfirmation` containing the total amount and the number of seats reserved.

Design decisions:

1.  Prices are externalised to `application.properties` and bound via `TicketPricing` in the `pricing` package, so the `domain` package stays unmodified as required. The application fails to start if any ticket type has a missing, empty or negative price.
2.  The maximum of 25 tickets is a constant in `BookingValidator` rather than configuration. This is because the brief states it as a fixed business rule, whereas prices are expected to change.
3.  All monetary values use `BigDecimal` rather than `double` to avoid floating point rounding errors.
4.  `BookingConfirmation` has been extended with `totalAmount` and `seatsReserved`. The original single-argument constructor is retained so the change is non-breaking.
5.  The OpenAPI spec (`cinema-tickets.yaml`) used different field names to the code (`tickets` and `count` instead of `ticketRequests` and `ticketCount`). As the brief states the domain code must not be changed the spec has been corrected to match the code, and updated with the new `BookingConfirmation` fields and error responses.

## Error handling

All errors are returned as an RFC 9457 Problem Detail body.

| Status                      | When                                                                                            | Detail                                      |
|-----------------------------|-------------------------------------------------------------------------------------------------|---------------------------------------------|
| `400 Bad Request`           | The booking breaks a purchase requests rule or the request cannot be read                       | The rule that was broken                    |
| `502 Bad Gateway`           | The payment or seat reservation service throws an exception or returns an unsuccessful response | A generic message; the full error is logged |
| `500 Internal Server Error` | Any other unexpected error                                                                      | A generic message; the full error is logged |

The brief states that both external provider services always succeed so the `502` handling is defensive. As nothing is known about how these services fail, any exception, `null` response or non-`2xx` status from them is treated as a failure and a payment/seat reservation error thrown.

Payment is taken before seats are reserved and seats are never reserved if payment fails. Neither service offers a way to undo a request so a failure cannot be compensated automatically:

1.  If the payment request fails with an exception (e.g. a timeout) it is not known whether the account was debited, so the amount and account id are logged for further investigation.
2.  If payment succeeds but seat reservation fails then the account has been charged without seats being reserved, so the amount and account id are logged for further investigation.

Failed requests are not retried because retrying a payment could charge the account twice. A production system would need an idempotent payment API or refund and release operations to handle these cases automatically.

## Testing

1.  `BookingValidatorTest` tests every validation rule including boundaries such as maximum 25 tickets.
2.  `TicketCostCalculatorTest` and the `TicketPricing` tests cover price calculation and loading prices from configuration.
3.  `CinemaTicketsServiceImplTest` tests the service with the third-party services mocked, including that nothing is charged for an invalid booking and that seats are not reserved if payment fails.
4.  `CinemaTicketsControllerTest` tests the endpoint over HTTP with only the third-party services mocked, covering a successful booking, one rejected request per business rule, unreadable requests, third-party failures and unexpected errors.

## Assumptions

In addition to the assumptions given in the brief:

1.  Infants count towards the 25 ticket limit. The brief states a maximum of 25 _tickets_; an infant ticket is still a ticket even though they are free and not allocated a seat.
2.  Each infant requires their own adult. Infants sit on an adult's lap and as an adult has only one lap a booking with more infants than adults is rejected.
3.  A ticket request with a count of zero is allowed (the OpenAPI spec specifies `minimum: 0`) and is treated as no tickets of that type. A booking must still contain at least one ticket in total.
4.  A missing or null `ticketCount` is treated as zero. `ticketCount` is a primitive `int` in the domain record, which cannot be changed, so it defaults to zero when the JSON is parsed.
5.  The same ticket type may appear more than once in a booking, e.g. `ADULT × 1` and `ADULT × 2` (the counts are combined).
6.  There is no maximum ratio of children to adults (not specified in the brief). E.g. one adult with 24 children is valid.
7.  Unknown JSON fields are ignored rather than rejected - this is so that clients sending additional fields are not broken.
8.  A non-`2xx` response from an external provider service means the request failed. The brief does not describe their responses, so a `2xx` status is taken to mean success.

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
