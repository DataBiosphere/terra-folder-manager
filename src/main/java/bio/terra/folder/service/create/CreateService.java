package bio.terra.folder.service.create;

import bio.terra.folder.db.FolderDao;
import bio.terra.folder.generated.model.CreateFolderBody;
import bio.terra.folder.service.create.exception.NameConflictException;
import bio.terra.folder.service.create.flight.FolderCreateFlight;
import bio.terra.folder.service.create.flight.FolderFlightMapKeys;
import bio.terra.folder.service.iam.AuthenticatedUserRequest;
import bio.terra.folder.service.job.JobBuilder;
import bio.terra.folder.service.job.JobService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CreateService {

  private JobService jobService;
  private FolderDao folderDao;

  public CreateService(JobService jobService, FolderDao folderDao) {
    this.jobService = jobService;
    this.folderDao = folderDao;
  }

  public String createFolder(CreateFolderBody folderBody, AuthenticatedUserRequest userReq) {
    String folderId = UUID.randomUUID().toString();
    String folderName = folderBody.getName();
    String parentFolderId = folderBody.getParentFolderId().orElse(null);

    if (parentFolderId != null && folderDao.containedFolderNameExists(parentFolderId, folderName)) {
      throw new NameConflictException(
          "Folder with name " + folderName + " already exists inside folder " + parentFolderId);
    }
    if (parentFolderId != null && folderDao.containedObjectNameExists(parentFolderId, folderName)) {
      throw new NameConflictException(
          "Contained object with name "
              + folderName
              + " already exists inside folder "
              + parentFolderId);
    }

    String description = "Create folder " + folderId;
    JobBuilder jobBuilder =
        jobService.newJob(
            description,
            folderBody.getJobControl().getJobid(),
            FolderCreateFlight.class,
            folderBody,
            userReq);
    jobBuilder.addParameter(FolderFlightMapKeys.FOLDER_ID, folderId);
    jobBuilder.submit();
    return folderId;
  }
}
