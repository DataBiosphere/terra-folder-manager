package bio.terra.folder.service.folder.flight;

import bio.terra.folder.common.utils.FlightUtils;
import bio.terra.folder.db.FolderDao;
import bio.terra.folder.generated.model.CreateFolderBody;
import bio.terra.folder.generated.model.CreatedFolder;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.exception.RetryException;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;

public class CreateFolderStep implements Step {

  private FolderDao folderDao;
  private CreateFolderBody folderBody;

  public CreateFolderStep(FolderDao folderDao, CreateFolderBody folderBody) {
    this.folderDao = folderDao;
    this.folderBody = folderBody;
  }

  @Override
  public StepResult doStep(FlightContext flightContext) throws RetryException {
    FlightMap inputMap = flightContext.getInputParameters();
    String folderId = inputMap.get(FolderFlightMapKeys.FOLDER_ID, String.class);
    String parentFolderId = folderBody.getParentFolderId().orElse(null);
    String spendProfile = folderBody.getSpendProfile().orElse(null);
    boolean spendProfileInherited = false;

    // Because we check the spend profile validity when a request is received, we do not need to
    // recheck it here.
    if (parentFolderId != null) {
      String spendProfileFromParent = folderDao.getSpendProfileFromFolder(parentFolderId);
      if (spendProfileFromParent != null) {
        spendProfile = spendProfileFromParent;
        spendProfileInherited = true;
      }
    }

    folderDao.createFolder(
        folderId,
        folderBody.getName(),
        JsonNullable.of(parentFolderId),
        JsonNullable.of(spendProfile),
        spendProfileInherited);

    CreatedFolder response = new CreatedFolder();
    response.setId(folderId);
    FlightUtils.setResponse(flightContext, response, HttpStatus.OK);
    return StepResult.getStepResultSuccess();
  }

  @Override
  public StepResult undoStep(FlightContext flightContext) {
    FlightMap inputMap = flightContext.getInputParameters();
    String folderId = inputMap.get(FolderFlightMapKeys.FOLDER_ID, String.class);
    folderDao.deleteFolder(folderId);
    return StepResult.getStepResultSuccess();
  }
}
