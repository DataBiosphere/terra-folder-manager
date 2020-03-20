package bio.terra.folder.service.job.exception;

import bio.terra.folder.common.exception.UnauthorizedException;

public class JobUnauthorizedException extends UnauthorizedException {
  public JobUnauthorizedException(String message) {
    super(message);
  }

  public JobUnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }

  public JobUnauthorizedException(Throwable cause) {
    super(cause);
  }
}
