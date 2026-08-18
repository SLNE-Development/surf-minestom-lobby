package dev.slne.minestom.lobby.server.instrumentation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.jar.JarFile;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jpenilla.gremlin.runtime.DependencyCache;
import xyz.jpenilla.gremlin.runtime.DependencyResolver;
import xyz.jpenilla.gremlin.runtime.DependencySet;
import xyz.jpenilla.gremlin.runtime.logging.Slf4jGremlinLogger;

@NullMarked
final class DependencyInstaller {

  private static final Logger LOGGER = LoggerFactory.getLogger(DependencyInstaller.class);

  static void install(Instrumentation instrumentation) {
    DependencySet deps = DependencySet.readDefault(DependencyInstaller.class.getClassLoader());
    DependencyCache cache = new DependencyCache(Path.of("libraries"));

    try (DependencyResolver resolver = new DependencyResolver(new Slf4jGremlinLogger(LOGGER))) {
      resolver.resolve(deps, cache)
          .jarFiles()
          .forEach(jar -> {
            try {
              JarFile jarFile = new JarFile(jar.toFile());
              instrumentation.appendToSystemClassLoaderSearch(jarFile);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    }

    cache.cleanup();
  }
}
