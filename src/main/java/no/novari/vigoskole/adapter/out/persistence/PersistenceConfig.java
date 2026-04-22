package no.novari.vigoskole.adapter.out.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import no.novari.vigoskole.config.AppProperties;
import org.eclipse.store.storage.embedded.types.EmbeddedStorage;
import org.eclipse.store.storage.embedded.types.EmbeddedStorageManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class PersistenceConfig {

  @Bean(destroyMethod = "shutdown")
  EmbeddedStorageManager embeddedStorageManager(AppProperties appProperties) {
    return EmbeddedStorage.start(appProperties.storageDirectory());
  }

  @Bean
  EclipseStoreState eclipseStoreState() {
    return new EclipseStoreState();
  }

  @Bean
  EclipseStoreInitializer eclipseStoreInitializer(
      EmbeddedStorageManager embeddedStorageManager,
      EclipseStoreState state,
      AppProperties appProperties) {
    return new EclipseStoreInitializer(
        embeddedStorageManager, state, appProperties.storageDirectory());
  }

  static final class EclipseStoreInitializer {

    EclipseStoreInitializer(
        EmbeddedStorageManager embeddedStorageManager,
        EclipseStoreState state,
        Path storageDirectory) {
      try {
        Files.createDirectories(storageDirectory);
        Object root = embeddedStorageManager.root();
        if (!(root instanceof EclipseStoreState)) {
          embeddedStorageManager.setRoot(state);
          embeddedStorageManager.storeRoot();
        }
      } catch (java.io.IOException exception) {
        throw new IllegalStateException("Kunne ikke opprette Eclipse Store-katalog.", exception);
      }
    }
  }
}
