package bio.terra.folder.service.create.flight;

import bio.terra.folder.db.FolderDao;
import bio.terra.folder.generated.model.CreateFolderBody;
import bio.terra.folder.service.iam.AuthenticatedUserRequest;
import bio.terra.folder.service.job.JobMapKeys;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import org.springframework.context.ApplicationContext;

public class FolderCreateFlight extends Flight {

  public FolderCreateFlight(FlightMap inputParameters, Object applicationContext) {
    super(inputParameters, applicationContext);

    ApplicationContext appContext = (ApplicationContext) applicationContext;
    FolderDao folderDao = (FolderDao) appContext.getBean("folderDao");

    AuthenticatedUserRequest userReq =
        inputParameters.get(JobMapKeys.AUTH_USER_INFO.getKeyName(), AuthenticatedUserRequest.class);
    CreateFolderBody folderBody =
        inputParameters.get(JobMapKeys.REQUEST.getKeyName(), CreateFolderBody.class);
    // TODO: Sam authentication step should go here.
    addStep(new CreateFolderStep(folderDao, folderBody));
  }
}
