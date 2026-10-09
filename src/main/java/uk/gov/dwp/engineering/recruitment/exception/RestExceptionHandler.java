package uk.gov.dwp.engineering.recruitment.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

  static final String THIRD_PARTY_FAILURE_DETAIL =
      "The booking could not be completed. Please try again later.";

  static final String UNEXPECTED_ERROR_DETAIL = "An unexpected error occurred.";

  @ExceptionHandler(InvalidBookingException.class)
  protected ProblemDetail handleInvalidBookingException(final InvalidBookingException ex) {
    final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problemDetail.setDetail(ex.getMessage());
    return problemDetail;
  }

  @ExceptionHandler({PaymentException.class, SeatReservationException.class})
  protected ProblemDetail handleThirdPartyServiceException(final RuntimeException ex) {
    LOG.error("Third-party service failure", ex);
    final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
    problemDetail.setDetail(THIRD_PARTY_FAILURE_DETAIL);
    return problemDetail;
  }

  @ExceptionHandler(Exception.class)
  protected ProblemDetail handleUnexpectedException(final Exception ex) {
    LOG.error("Unexpected error", ex);
    final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problemDetail.setDetail(UNEXPECTED_ERROR_DETAIL);
    return problemDetail;
  }
}
