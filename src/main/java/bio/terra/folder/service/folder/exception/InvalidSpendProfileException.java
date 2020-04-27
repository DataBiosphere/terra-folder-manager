package bio.terra.folder.service.folder.exception;

import bio.terra.folder.common.exception.BadRequestException;

public class InvalidSpendProfileException extends BadRequestException {

  public InvalidSpendProfileException(String message) {
    super(message);
  }

  public InvalidSpendProfileException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidSpendProfileException(Throwable cause) {
    super(cause);
  }
}
