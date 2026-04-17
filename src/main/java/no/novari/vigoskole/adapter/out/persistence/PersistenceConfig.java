package no.novari.vigoskole.adapter.out.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import no.novari.vigoskole.config.AppProperties;
import no.novari.vigoskole.domain.model.SubmissionWindow;
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
  EclipseStoreState eclipseStoreState(AppProperties appProperties) {
    SubmissionWindow defaultWindow =
        new SubmissionWindow(
            appProperties.submissionWindow().from(), appProperties.submissionWindow().to());
    return new EclipseStoreState(defaultWindow);
  }

  @Bean
  EclipseStoreInitializer eclipseStoreInitializer(
      EmbeddedStorageManager embeddedStorageManager,
      EclipseStoreState state,
      AppProperties appProperties) {
    SubmissionWindow defaultWindow =
        new SubmissionWindow(
            appProperties.submissionWindow().from(), appProperties.submissionWindow().to());
    return new EclipseStoreInitializer(
        embeddedStorageManager, state, appProperties.storageDirectory(), defaultWindow);
  }

  static final class EclipseStoreInitializer {

    EclipseStoreInitializer(
        EmbeddedStorageManager embeddedStorageManager,
        EclipseStoreState state,
        Path storageDirectory,
        SubmissionWindow defaultWindow) {
      try {
        Files.createDirectories(storageDirectory);
        Object root = embeddedStorageManager.root();
        EclipseStoreState rootState;
        boolean updated = false;
        if (root instanceof EclipseStoreState loadedState) {
          rootState = loadedState;
        } else {
          rootState = state;
          embeddedStorageManager.setRoot(state);
          updated = true;
        }
        SubmissionWindow existing = rootState.submissionWindow();
        if (existing == null || existing.from() == null || existing.to() == null) {
          rootState.submissionWindow(defaultWindow);
          updated = true;
        }
        if (updated) {
          embeddedStorageManager.storeRoot();
        }
      } catch (java.io.IOException exception) {
        throw new IllegalStateException("Kunne ikke opprette Eclipse Store-katalog.", exception);
      }
    }
  }
}
