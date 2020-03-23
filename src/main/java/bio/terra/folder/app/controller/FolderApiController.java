package bio.terra.folder.app.controller;

import bio.terra.folder.generated.controller.FolderApi;
import bio.terra.folder.generated.model.JobModel;
import bio.terra.folder.service.iam.AuthenticatedUserRequest;
import bio.terra.folder.service.iam.AuthenticatedUserRequestFactory;
import bio.terra.folder.service.job.JobService;
import bio.terra.folder.service.job.JobService.JobResultWithStatus;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class FolderApiController implements FolderApi {
  private JobService jobService;
  private AuthenticatedUserRequestFactory authenticatedUserRequestFactory;
  private final HttpServletRequest request;

  @Autowired
  public FolderApiController(
      JobService jobService,
      AuthenticatedUserRequestFactory authenticatedUserRequestFactory,
      HttpServletRequest request) {
    this.jobService = jobService;
    this.authenticatedUserRequestFactory = authenticatedUserRequestFactory;
    this.request = request;
  }

  private AuthenticatedUserRequest getAuthenticatedInfo() {
    return authenticatedUserRequestFactory.from(request);
  }

  @Override
  public ResponseEntity<Void> deleteJob(@PathVariable("id") String id) {
    AuthenticatedUserRequest userReq = getAuthenticatedInfo();
    jobService.releaseJob(id, userReq);
    return new ResponseEntity<>(HttpStatus.valueOf(204));
  }

  @Override
  public ResponseEntity<JobModel> pollAsyncJob(@PathVariable("id") String id) {
    AuthenticatedUserRequest userReq = getAuthenticatedInfo();
    JobModel job = jobService.retrieveJob(id, userReq);
    return new ResponseEntity<JobModel>(job, HttpStatus.valueOf(job.getStatusCode()));
  }

  @Override
  public ResponseEntity<Object> retrieveJobResult(@PathVariable("id") String id) {
    AuthenticatedUserRequest userReq = getAuthenticatedInfo();
    JobResultWithStatus<Object> jobResultHolder =
        jobService.retrieveJobResult(id, Object.class, userReq);
    return new ResponseEntity<>(jobResultHolder.getResult(), jobResultHolder.getStatusCode());
  }
}
