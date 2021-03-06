package bio.terra.folder.app.configuration;

import bio.terra.folder.app.StartupInitializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "folder")
public class ApplicationConfiguration {

  // Configurable properties
  private int maxStairwayThreads;
  private int stairwayTimeoutSeconds;
  private String resourceId;

  // Not a property
  private PoolingDataSource<PoolableConnection> dataSource;

  public int getStairwayTimeoutSeconds() {
    return stairwayTimeoutSeconds;
  }

  public void setStairwayTimeoutSeconds(int stairwayTimeoutSeconds) {
    this.stairwayTimeoutSeconds = stairwayTimeoutSeconds;
  }

  public int getMaxStairwayThreads() {
    return maxStairwayThreads;
  }

  public void setMaxStairwayThreads(int maxStairwayThreads) {
    this.maxStairwayThreads = maxStairwayThreads;
  }

  public String getResourceId() {
    return resourceId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  @Bean("jdbcTemplate")
  public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate(
      FolderManagerJdbcConfiguration config) {
    return new NamedParameterJdbcTemplate(config.getDataSource());
  }

  @Bean("objectMapper")
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
        .registerModule(new ParameterNamesModule())
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .registerModule(new JsonNullableModule());
  }

  // This is a "magic bean": It supplies a method that Spring calls after the application is setup,
  // but before the port is opened for business. That lets us do database migration and stairway
  // initialization on a system that is otherwise fully configured. The rule of thumb is that all
  // bean initialization should avoid database access. If there is additional database work to be
  // done, it should happen inside this method.
  @Bean
  public SmartInitializingSingleton postSetupInitialization(ApplicationContext applicationContext) {
    return () -> {
      StartupInitializer.initialize(applicationContext);
    };
  }
}
