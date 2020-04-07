package bio.terra.folder.db;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.folder.app.Main;
import bio.terra.folder.app.configuration.FolderManagerJdbcConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Tag("unit")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Main.class)
@SpringBootTest
@AutoConfigureMockMvc
public class FolderDaoTest {

  @Autowired FolderManagerJdbcConfiguration jdbcConfiguration;

  @Autowired FolderDao folderDao;

  private NamedParameterJdbcTemplate jdbcTemplate;

  private UUID folderId;
  private UUID spendProfileId;
  private String readSql =
      "SELECT folder_id, folder_name, parent_folder_id, spend_profile_id, spend_profile_inherited FROM folder WHERE folder_id = :id";

  @BeforeEach
  public void setup() {
    folderId = UUID.randomUUID();
    spendProfileId = UUID.randomUUID();
    jdbcTemplate = new NamedParameterJdbcTemplate(jdbcConfiguration.getDataSource());
  }

  @Test
  public void verifyCreatedFolderExists() throws Exception {
    String folderName = "fakeName";
    folderDao.createFolder(
        folderId.toString(),
        folderName,
        JsonNullable.undefined(),
        JsonNullable.of(spendProfileId.toString()),
        false);
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("id", folderId.toString());
    Map<String, Object> resultFolder = jdbcTemplate.queryForMap(readSql, paramMap);

    assertThat(resultFolder.get("folder_id"), equalTo(folderId.toString()));
    assertThat(resultFolder.get("folder_name"), equalTo(folderName));
    assertThat(resultFolder.get("spend_profile_id"), equalTo(spendProfileId.toString()));
    assertThat(resultFolder.get("spend_profile_inherited"), equalTo(false));
    assertThat(resultFolder.get("parent_folder_id"), equalTo(null));

    // This test doesn't clean up after itself - be sure it only runs on unit test DBs, which
    // are always re-created for tests.
  }

  @Test
  public void createFolderWithParent() throws Exception {
    String parentFolderName = "parentFolder";
    String childFolderName = "childFolder";
    folderDao.createFolder(
        folderId.toString(),
        parentFolderName,
        JsonNullable.undefined(),
        JsonNullable.of(spendProfileId.toString()),
        false);
    UUID childId = UUID.randomUUID();
    folderDao.createFolder(
        childId.toString(),
        childFolderName,
        JsonNullable.of(folderId.toString()),
        JsonNullable.undefined(),
        true);

    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("id", childId.toString());
    Map<String, Object> resultFolder = jdbcTemplate.queryForMap(readSql, paramMap);

    assertThat(resultFolder.get("parent_folder_id"), equalTo(folderId.toString()));
  }

  @Test
  public void createAndDeleteEmptyFolder() throws Exception {
    String folderName = "fakeName";
    folderDao.createFolder(
        folderId.toString(),
        folderName,
        JsonNullable.undefined(),
        JsonNullable.of(spendProfileId.toString()),
        false);

    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("id", folderId.toString());
    Map<String, Object> resultFolder = jdbcTemplate.queryForMap(readSql, paramMap);

    assertThat(resultFolder.get("folder_id"), equalTo(folderId.toString()));

    folderDao.deleteFolder(folderId.toString());

    // Assert that the folder can no longer be found.
    assertThrows(
        EmptyResultDataAccessException.class,
        () -> {
          jdbcTemplate.queryForMap(readSql, paramMap);
        });
  }

  @Test
  public void deleteNonExistentFolderFails() throws Exception {
    assertFalse(folderDao.deleteFolder(folderId.toString()));
  }
}
