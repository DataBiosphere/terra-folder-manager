package bio.terra.folder.app.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "db.stairway")
public class StairwayJdbcConfiguration extends JdbcConfiguration {
  private String migrateUpgrade;
  private String forceClean;

  public String getMigrateUpgrade() {
    return migrateUpgrade;
  }

  public void setMigrateUpgrade(String migrateUpgrade) {
    this.migrateUpgrade = migrateUpgrade;
  }

  public String getForceClean() {
    return forceClean;
  }

  public void setForceClean(String forceClean) {
    this.forceClean = forceClean;
  }

  public boolean isMigrateUpgrade() {
    return Boolean.parseBoolean(migrateUpgrade);
  }

  public boolean isForceClean() {
    return Boolean.parseBoolean(forceClean);
  }
}
