package bio.terra.folder.service.create;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.folder.app.Main;
import bio.terra.folder.generated.model.CreateFolderBody;
import bio.terra.folder.generated.model.CreatedFolder;
import bio.terra.folder.generated.model.ErrorReport;
import bio.terra.folder.generated.model.JobControl;
import bio.terra.folder.service.iam.AuthenticatedUserRequest;
import bio.terra.folder.service.iam.AuthenticatedUserRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Tag("unit")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CreateServiceTest {

  @Autowired private MockMvc mvc;

  // Mock MVC doesn't populate the fields used to build authenticated requests.
  @MockBean private AuthenticatedUserRequestFactory mockAuthenticatedUserRequestFactory;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private CreateService createService;

  @BeforeEach
  public void setup() {
    AuthenticatedUserRequest fakeAuthentication = new AuthenticatedUserRequest();
    fakeAuthentication
        .token(Optional.of("fake-token"))
        .email("fake@email.com")
        .subjectId("fakeID123");
    when(mockAuthenticatedUserRequestFactory.from(any())).thenReturn(fakeAuthentication);
  }

  @Test
  public void folderCreatedFromJobRequest() throws Exception {
    String jobId = UUID.randomUUID().toString();
    CreateFolderBody request =
        buildRequest("fakeFolderName", JsonNullable.undefined(), JsonNullable.undefined(), jobId);
    CreatedFolder folder = runCreateFolderCall(request, jobId);
    assertThat(folder.getId(), Matchers.not(blankOrNullString()));
  }

  @Test
  public void folderCreatedWithParent() throws Exception {
    String jobId = UUID.randomUUID().toString();
    CreateFolderBody parentRequest =
        buildRequest("parentName", JsonNullable.undefined(), JsonNullable.undefined(), jobId);
    CreatedFolder parentFolder = runCreateFolderCall(parentRequest, jobId);
    String parentId = parentFolder.getId();

    String childJobId = UUID.randomUUID().toString();
    CreateFolderBody childRequest =
        buildRequest("childName", JsonNullable.of(parentId), JsonNullable.undefined(), childJobId);
    CreatedFolder childFolder = runCreateFolderCall(childRequest, childJobId);
    assertThat(childFolder.getId(), Matchers.not(blankOrNullString()));
  }

  @Test
  public void duplicateSubfolderIsRejected() throws Exception {
    // First, create a parent and child folder.
    String jobId = UUID.randomUUID().toString();
    CreateFolderBody parentRequest =
        buildRequest("parentName", JsonNullable.undefined(), JsonNullable.undefined(), jobId);
    CreatedFolder parentFolder = runCreateFolderCall(parentRequest, jobId);
    String parentId = parentFolder.getId();

    String childJobId = UUID.randomUUID().toString();
    CreateFolderBody childRequest =
        buildRequest("childName", JsonNullable.of(parentId), JsonNullable.undefined(), childJobId);
    CreatedFolder childFolder = runCreateFolderCall(childRequest, childJobId);

    // Next, request another subfolder with the same name as the existing child.
    JobControl secondChildJobControl = new JobControl();
    String secondChildJobId = UUID.randomUUID().toString();
    secondChildJobControl.setJobid(secondChildJobId);
    childRequest.setJobControl(secondChildJobControl);

    MvcResult failedResult =
        mvc.perform(
                post("/api/v1/folders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(childRequest)))
            .andExpect(status().is(400))
            .andReturn();

    ErrorReport createError =
        objectMapper.readValue(failedResult.getResponse().getContentAsString(), ErrorReport.class);
    assertThat(createError.getMessage(), Matchers.containsString("already exists"));
  }

  @Test
  public void overridingSpendProfileFails() throws Exception {
    String jobId = UUID.randomUUID().toString();
    CreateFolderBody parentRequest =
        buildRequest(
            "parentName", JsonNullable.undefined(), JsonNullable.of("spend-profile-id"), jobId);
    CreatedFolder parentFolder = runCreateFolderCall(parentRequest, jobId);
    String parentId = parentFolder.getId();

    String childJobId = UUID.randomUUID().toString();
    CreateFolderBody childRequest =
        buildRequest(
            "childName",
            JsonNullable.of(parentId),
            JsonNullable.of("different-spend-profile-id"),
            childJobId);

    MvcResult createResult = callCreateEndpoint(childRequest);
    pollJobUntilComplete(childJobId);

    MvcResult failedResult =
        mvc.perform(get("/api/v1/jobs/" + childJobId + "/result"))
            .andExpect(status().is(400))
            .andReturn();
    ErrorReport createError =
        objectMapper.readValue(failedResult.getResponse().getContentAsString(), ErrorReport.class);
    assertThat(createError.getMessage(), Matchers.containsString("spend profile"));
  }

  private CreateFolderBody buildRequest(
      String name, JsonNullable<String> parentId, JsonNullable<String> spendProfile, String jobId) {
    CreateFolderBody output = new CreateFolderBody();
    output.setName(name);
    output.setParentFolderId(parentId);
    output.setSpendProfile(spendProfile);
    JobControl job = new JobControl();
    job.setJobid(jobId);
    output.setJobControl(job);
    return output;
  }

  private CreatedFolder runCreateFolderCall(CreateFolderBody request, String jobId)
      throws Exception {
    MvcResult initialResult = callCreateEndpoint(request);
    pollJobUntilComplete(jobId);
    return getCreateJobResult(jobId);
  }

  private MvcResult callCreateEndpoint(CreateFolderBody request) throws Exception {
    return mvc.perform(
            post("/api/v1/folders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().is(202))
        .andReturn();
  }

  private void pollJobUntilComplete(String jobId) throws Exception {
    HttpStatus pollStatus = HttpStatus.valueOf(202);
    while (pollStatus == HttpStatus.valueOf(202)) {
      MvcResult pollResult = mvc.perform(get("/api/v1/jobs/" + jobId)).andReturn();
      pollStatus = HttpStatus.valueOf(pollResult.getResponse().getStatus());
    }
    assertThat(pollStatus, equalTo(HttpStatus.OK));
  }

  private CreatedFolder getCreateJobResult(String jobId) throws Exception {
    MvcResult callResult =
        mvc.perform(get("/api/v1/jobs/" + jobId + "/result"))
            .andExpect(status().is(200))
            .andReturn();

    return objectMapper.readValue(
        callResult.getResponse().getContentAsString(), CreatedFolder.class);
  }
}
