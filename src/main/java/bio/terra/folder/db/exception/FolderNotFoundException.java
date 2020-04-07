package bio.terra.folder.db.exception;

import bio.terra.folder.common.exception.NotFoundException;

public class FolderNotFoundException extends NotFoundException {

  public FolderNotFoundException(String message) {
    super(message);
  }

  public FolderNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public FolderNotFoundException(Throwable cause) {
    super(cause);
  }
}
