package bio.terra.folder.app;

import bio.terra.folder.app.configuration.FolderManagerJdbcConfiguration;
import bio.terra.folder.service.job.JobService;
import bio.terra.folder.service.migrate.MigrateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public final class StartupInitializer {
  private static final Logger logger = LoggerFactory.getLogger(StartupInitializer.class);
  private static final String changelogPath = "db/changelog.xml";

  public static void initialize(ApplicationContext applicationContext) {
    // Initialize or upgrade the database depending on the configuration
    MigrateService migrateService = (MigrateService) applicationContext.getBean("migrateService");
    FolderManagerJdbcConfiguration folderManagerJdbcConfiguration =
        (FolderManagerJdbcConfiguration)
            applicationContext.getBean("folderManagerJdbcConfiguration");
    JobService jobService = (JobService) applicationContext.getBean("jobService");

    if (folderManagerJdbcConfiguration.isInitializeOnStart()) {
      migrateService.initialize(changelogPath, folderManagerJdbcConfiguration.getDataSource());
    } else if (folderManagerJdbcConfiguration.isUpgradeOnStart()) {
      migrateService.upgrade(changelogPath, folderManagerJdbcConfiguration.getDataSource());
    }

    // The JobService initialization also handles Stairway initialization.
    jobService.initialize();

    // TODO: Fill in this method with any other initialization that needs to happen
    //  between the point of having the entire application initialized and
    //  the point of opening the port to start accepting REST requests.

  }
}
