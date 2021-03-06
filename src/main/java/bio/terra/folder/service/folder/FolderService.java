package bio.terra.folder.service.folder;

import bio.terra.folder.db.FolderDao;
import bio.terra.folder.generated.model.CreateFolderBody;
import bio.terra.folder.generated.model.CreatedFolder;
import bio.terra.folder.service.folder.exception.InvalidNameException;
import bio.terra.folder.service.folder.exception.InvalidSpendProfileException;
import bio.terra.folder.service.folder.exception.NameConflictException;
import bio.terra.folder.service.folder.flight.FolderCreateFlight;
import bio.terra.folder.service.folder.flight.FolderFlightMapKeys;
import bio.terra.folder.service.iam.AuthenticatedUserRequest;
import bio.terra.folder.service.job.JobBuilder;
import bio.terra.folder.service.job.JobService;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

// Service for managing actions related to Folder CRUD operations. Operations for managing contained
// objects belong in a separate service.
@Component
public class FolderService {

  public static Pattern validNamePattern = Pattern.compile("^[\\w\\-\\s_]+$");

  private JobService jobService;
  private FolderDao folderDao;

  public FolderService(JobService jobService, FolderDao folderDao) {
    this.jobService = jobService;
    this.folderDao = folderDao;
  }

  public CreatedFolder createFolder(CreateFolderBody folderBody, AuthenticatedUserRequest userReq) {
    validateRequest(folderBody);

    String folderId = UUID.randomUUID().toString();
    String description = "Create folder " + folderId;
    JobBuilder jobBuilder =
        jobService.newJob(
            description,
            UUID.randomUUID().toString(), // JobId does not need persistence for sync calls.
            FolderCreateFlight.class,
            folderBody,
            userReq);
    jobBuilder.addParameter(FolderFlightMapKeys.FOLDER_ID, folderId);
    return jobBuilder.submitAndWait(CreatedFolder.class);
  }

  private void validateRequest(CreateFolderBody request) {
    String folderName = request.getName();
    String parentFolderId = request.getParentFolderId().orElse(null);
    // Require names only include alphanumeric characters, spaces, - and _
    if (!validNamePattern.matcher(folderName).matches()) {
      throw new InvalidNameException(
          "Provided name "
              + folderName
              + " contains invalid character(s). Valid names must only include alphanumeric"
              + " characters, spaces, -, and _ characters, and cannot be empty.");
    }
    // Max length of text column type in Postgres
    if (folderName.length() > (10 * 1024 * 1024)) {
      throw new InvalidNameException(
          "Provided name exceeds maximum length of " + (10 * 1024 * 1024));
    }
    // Validate name uniqueness
    if (folderDao.containedFolderNameExists(parentFolderId, folderName)) {
      throw new NameConflictException(
          "Folder with name "
              + folderName
              + ""
              + " already exists inside "
              + (parentFolderId == null ? "top-level folder" : parentFolderId));
    }
    if (folderDao.containedObjectNameExists(parentFolderId, folderName)) {
      throw new NameConflictException(
          "Contained object with name "
              + folderName
              + " already exists inside folder "
              + (parentFolderId == null ? "top-level folder" : parentFolderId));
    }

    validateSpendProfile(request);
  }

  private void validateSpendProfile(CreateFolderBody request) {
    // Generally, it's invalid to specify a spend profile when one is inherited from the parent
    // folder. However, when the requested spend profile matches the inherited spend profile,
    // this is just a hassle to callers.
    String parentFolderId = request.getParentFolderId().orElse(null);
    String spendProfile = request.getSpendProfile().orElse(null);
    if (parentFolderId != null) {
      String spendProfileFromParent = folderDao.getSpendProfileFromFolder(parentFolderId);
      if (spendProfileFromParent != null
          && spendProfile != null
          && spendProfileFromParent != spendProfile) {
        throw new InvalidSpendProfileException(
            "You cannot override a parent folder's spend profile. Request provided spend profile: "
                + spendProfile
                + " but parent folder has spend profile: "
                + spendProfileFromParent);
      }
    }
  }
}
