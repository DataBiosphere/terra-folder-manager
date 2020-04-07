package bio.terra.folder.service.create.exception;

import bio.terra.folder.common.exception.BadRequestException;

public class NameConflictException extends BadRequestException {

  public NameConflictException(String message) {
    super(message);
  }

  public NameConflictException(String message, Throwable cause) {
    super(message, cause);
  }

  public NameConflictException(Throwable cause) {
    super(cause);
  }
}
