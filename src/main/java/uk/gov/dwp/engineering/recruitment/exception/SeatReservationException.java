package uk.gov.dwp.engineering.recruitment.exception;

public class SeatReservationException extends RuntimeException {

  public SeatReservationException(String message) {
    super(message);
  }

  public SeatReservationException(String message, Throwable cause) {
    super(message, cause);
  }
}
