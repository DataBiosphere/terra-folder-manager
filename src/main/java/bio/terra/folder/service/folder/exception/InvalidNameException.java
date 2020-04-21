package bio.terra.folder.service.folder.exception;

import bio.terra.folder.common.exception.BadRequestException;

public class InvalidNameException extends BadRequestException {

  public InvalidNameException(String message) {
    super(message);
  }

  public InvalidNameException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidNameException(Throwable cause) {
    super(cause);
  }
}
