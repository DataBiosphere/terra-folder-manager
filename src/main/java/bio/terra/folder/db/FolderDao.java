package bio.terra.folder.db;

import bio.terra.folder.db.exception.FolderNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FolderDao {
  private final NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public FolderDao(NamedParameterJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void createFolder(
      String folderId,
      String folderName,
      JsonNullable<String> parentFolderId,
      JsonNullable<String> spendProfile,
      boolean spendProfileInherited) {
    String sql =
        "INSERT INTO folder (folder_id, folder_name, parent_folder_id, spend_profile_id, spend_profile_inherited)"
            + "values (:folder_id, :folder_name, :parent_folder_id, :spend_profile_id, :spend_profile_inherited)";
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("folder_id", folderId);
    paramMap.put("folder_name", folderName);
    paramMap.put("spend_profile_inherited", spendProfileInherited);

    paramMap.put("parent_folder_id", parentFolderId.isPresent() ? parentFolderId.get() : null);
    paramMap.put("spend_profile_id", spendProfile.isPresent() ? spendProfile.get() : null);

    jdbcTemplate.update(sql, paramMap);
  }

  public boolean deleteFolder(String folderId) {
    String sql = "DELETE FROM folder WHERE folder_id = :id";
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("id", folderId);
    int rowsAffected = jdbcTemplate.update(sql, paramMap);
    return rowsAffected > 0;
  }

  public String getSpendProfileFromFolder(String folderId) {
    String sql = "SELECT spend_profile_id from folder WHERE folder_id = :id";
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("id", folderId);
    try {
      return jdbcTemplate.queryForObject(sql, paramMap, String.class);
    } catch (EmptyResultDataAccessException e) {
      throw new FolderNotFoundException("Folder not found in DB: " + folderId);
    }
  }

  public boolean getSpendProfileInheritedFromFolder(String folderId) {
    String sql = "SELECT spend_profile_inherited from folder WHERE folder_id = :id";
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("id", folderId);
    try {
      return jdbcTemplate.queryForObject(sql, paramMap, Boolean.class);
    } catch (EmptyResultDataAccessException e) {
      throw new FolderNotFoundException("Folder not found in DB: " + folderId);
    }
  }

  // Checks whether a given name is taken by a contained object within a folder.
  public boolean containedObjectNameExists(String parentFolderId, String name) {
    String sql =
        "SELECT object_id from contained_object WHERE folder_id = :id AND object_name = :name LIMIT 1";
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("id", parentFolderId);
    paramMap.put("name", name);

    List<Map<String, Object>> resultSet = jdbcTemplate.queryForList(sql, paramMap);
    return !resultSet.isEmpty();
  }

  // Checks whether a given name is taken by a sub-folder within a folder.
  public boolean containedFolderNameExists(String parentFolderId, String name) {
    String sql =
        "SELECT folder_id from folder WHERE parent_folder_id = :id AND folder_name = :name LIMIT 1";
    Map<String, Object> paramMap = new HashMap<>();
    paramMap.put("id", parentFolderId);
    paramMap.put("name", name);

    List<Map<String, Object>> resultSet = jdbcTemplate.queryForList(sql, paramMap);
    return !resultSet.isEmpty();
  }
}
