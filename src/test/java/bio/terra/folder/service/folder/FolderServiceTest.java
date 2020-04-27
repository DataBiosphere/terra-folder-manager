package bio.terra.folder.service.folder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.terra.folder.app.Main;
import bio.terra.folder.generated.model.CreateFolderBody;
import bio.terra.folder.generated.model.CreatedFolder;
import bio.terra.folder.generated.model.ErrorReport;
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
public class FolderServiceTest {

  @Autowired private MockMvc mvc;

  // Mock MVC doesn't populate the fields used to build authenticated requests.
  @MockBean private AuthenticatedUserRequestFactory mockAuthenticatedUserRequestFactory;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private FolderService folderService;

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
        buildRequest("fakeFolderName", JsonNullable.undefined(), JsonNullable.undefined());
    CreatedFolder folder = runCreateFolderCall(request, jobId);
    assertThat(folder.getId(), Matchers.not(blankOrNullString()));
  }

  @Test
  public void folderCreatedWithParent() throws Exception {
    String jobId = UUID.randomUUID().toString();
    CreateFolderBody parentRequest =
        buildRequest("parentName", JsonNullable.undefined(), JsonNullable.undefined());
    CreatedFolder parentFolder = runCreateFolderCall(parentRequest, jobId);
    String parentId = parentFolder.getId();

    String childJobId = UUID.randomUUID().toString();
    CreateFolderBody childRequest =
        buildRequest("childName", JsonNullable.of(parentId), JsonNullable.undefined());
    CreatedFolder childFolder = runCreateFolderCall(childRequest, childJobId);
    assertThat(childFolder.getId(), Matchers.not(blankOrNullString()));
  }

  @Test
  public void nullNameRejected() throws Exception {
    String jobId = UUID.randomUUID().toString();
    CreateFolderBody request =
        buildRequest(null, JsonNullable.undefined(), JsonNullable.undefined());
    MvcResult failedResult =
        mvc.perform(
                post("/api/v1/folders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is(400))
            .andReturn();
    ErrorReport requestError =
        objectMapper.readValue(failedResult.getResponse().getContentAsString(), ErrorReport.class);
    assertThat(requestError.getMessage(), Matchers.containsString("Validation failed"));
  }

  @Test
  public void duplicateSubfolderIsRejected() throws Exception {
    // First, create a parent and child folder.
    String jobId = UUID.randomUUID().toString();
    CreateFolderBody parentRequest =
        buildRequest("parentName", JsonNullable.undefined(), JsonNullable.undefined());
    CreatedFolder parentFolder = runCreateFolderCall(parentRequest, jobId);
    String parentId = parentFolder.getId();

    String childJobId = UUID.randomUUID().toString();
    CreateFolderBody childRequest =
        buildRequest("childName", JsonNullable.of(parentId), JsonNullable.undefined());
    CreatedFolder childFolder = runCreateFolderCall(childRequest, childJobId);

    // Next, request another subfolder with the same name as the existing child.
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
        buildRequest("parentName", JsonNullable.undefined(), JsonNullable.of("spend-profile-id"));
    CreatedFolder parentFolder = runCreateFolderCall(parentRequest, jobId);
    String parentId = parentFolder.getId();

    CreateFolderBody childRequest =
        buildRequest(
            "childName", JsonNullable.of(parentId), JsonNullable.of("different-spend-profile-id"));

    MvcResult failedResult =
        mvc.perform(
                post("/api/v1/folders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(childRequest)))
            .andExpect(status().is(400))
            .andReturn();

    ErrorReport createError =
        objectMapper.readValue(failedResult.getResponse().getContentAsString(), ErrorReport.class);
    assertThat(createError.getMessage(), Matchers.containsString("spend profile"));
  }

  @Test
  public void badFolderNameFails() throws Exception {
    String jobId = UUID.randomUUID().toString();
    CreateFolderBody badRequest =
        buildRequest("!!!bad name!!!", JsonNullable.undefined(), JsonNullable.undefined());
    MvcResult failedResult =
        mvc.perform(
                post("/api/v1/folders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(badRequest)))
            .andExpect(status().is(400))
            .andReturn();
    ErrorReport createError =
        objectMapper.readValue(failedResult.getResponse().getContentAsString(), ErrorReport.class);
    assertThat(createError.getMessage(), Matchers.containsString("name"));
  }

  private CreateFolderBody buildRequest(
      String name, JsonNullable<String> parentId, JsonNullable<String> spendProfile) {
    CreateFolderBody output = new CreateFolderBody();
    output.setName(name);
    output.setParentFolderId(parentId);
    output.setSpendProfile(spendProfile);
    return output;
  }

  private CreatedFolder runCreateFolderCall(CreateFolderBody request, String jobId)
      throws Exception {
    MvcResult initialResult =
        mvc.perform(
                post("/api/v1/folders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is(200))
            .andReturn();
    return objectMapper.readValue(
        initialResult.getResponse().getContentAsString(), CreatedFolder.class);
  }
}
