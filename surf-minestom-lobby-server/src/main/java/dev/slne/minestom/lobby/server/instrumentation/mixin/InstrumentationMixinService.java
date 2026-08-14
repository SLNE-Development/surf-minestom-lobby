package dev.slne.minestom.lobby.server.instrumentation.mixin;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IAdviceProvider;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IFeatureValidator;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.IMixinInternal;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

@NullMarked
public final class InstrumentationMixinService extends MixinServiceAbstract implements
    IClassProvider, IClassBytecodeProvider {

  private static volatile @Nullable Instrumentation instrumentation;
  private volatile @Nullable IMixinTransformer transformer;


  public static void setInstrumentation(Instrumentation instrumentation) {
    if (InstrumentationMixinService.instrumentation != null) {
      throw new IllegalStateException("Instrumentation has already been initialized");
    }

    InstrumentationMixinService.instrumentation = instrumentation;
  }

  @Override
  public String getName() {
    return "SurfLobbyInstrumentation";
  }

  @Override
  public boolean isValid() {
    return instrumentation != null;
  }

  @Override
  public void offer(IMixinInternal internal) {
    super.offer(internal);

    if (internal instanceof IMixinTransformerFactory factory) {
      if (transformer != null) {
        throw new IllegalStateException("Mixin transformer has already been installed");
      }

      transformer = factory.createTransformer();

      requireNonNull(instrumentation, "instrumentation")
          .addTransformer(new TransformerAdapter(requireNonNull(transformer)), false);
    }
  }

  @Override
  public IClassProvider getClassProvider() {
    return this;
  }

  @Override
  public IClassBytecodeProvider getBytecodeProvider() {
    return this;
  }

  @Override
  public @Nullable ITransformerProvider getTransformerProvider() {
    return null;
  }

  @Override
  public @Nullable IClassTracker getClassTracker() {
    return null;
  }

  @Override
  public @Nullable IMixinAuditTrail getAuditTrail() {
    return null;
  }

  @Override
  public IFeatureValidator getFeatureValidator() {
    return IFeatureValidator.ALLOW_ALL;
  }

  @Override
  public IAdviceProvider getAdviceProvider() {
    return IAdviceProvider.GENERIC;
  }

  @Override
  public CompatibilityLevel getMinCompatibilityLevel() {
    return CompatibilityLevel.JAVA_8;
  }

  @Override
  public CompatibilityLevel getMaxCompatibilityLevel() {
    return CompatibilityLevel.JAVA_25;
  }

  @Override
  public Collection<String> getPlatformAgents() {
    return List.of("org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault");
  }

  @Override
  public IContainerHandle getPrimaryContainer() {
    CodeSource codeSource = InstrumentationMixinService.class
        .getProtectionDomain()
        .getCodeSource();

    if (codeSource == null) {
      return new ContainerHandleURI(URI.create("file:/"));
    }

    try {
      return new ContainerHandleURI(codeSource.getLocation().toURI());
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to determine lobby container", exception);
    }
  }

  @Override
  public @Nullable InputStream getResourceAsStream(String name) {
    return getLoader().getResourceAsStream(name);
  }

  @Override
  public URL[] getClassPath() {
    return new URL[0];
  }

  @Override
  public Class<?> findClass(String name) throws ClassNotFoundException {
    return Class.forName(name, true, getLoader());
  }

  @Override
  public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
    return Class.forName(name, initialize, getLoader());
  }

  @Override
  public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
    return findClass(name, initialize);
  }

  @Override
  public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
    return getClassNode(name, true);
  }

  @Override
  public ClassNode getClassNode(String name, boolean runTransformers)
      throws ClassNotFoundException, IOException {
    return getClassNode(name, runTransformers, 0);
  }

  @Override
  public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags)
      throws ClassNotFoundException, IOException {
    String resourceName = name.replace('.', '/') + ".class";
    byte[] bytes;

    try (InputStream stream = getLoader().getResourceAsStream(resourceName)) {
      if (stream == null) {
        throw new ClassNotFoundException(name);
      }

      bytes = stream.readAllBytes();
    }

    ClassReader reader = new ClassReader(bytes);
    ClassNode node = new ClassNode();

    reader.accept(node, readerFlags);

    return node;
  }

  private static ClassLoader getLoader() {
    ClassLoader context = Thread.currentThread().getContextClassLoader();

    if (context != null) {
      return context;
    }

    return InstrumentationMixinService.class.getClassLoader();
  }

  private record TransformerAdapter(IMixinTransformer transformer) implements ClassFileTransformer {

    @Override
    public byte @Nullable [] transform(
        Module module,
        ClassLoader loader,
        @Nullable String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer
    ) throws IllegalClassFormatException {
      if (className == null) {
        return null;
      }

      if (className.startsWith("org/spongepowered/asm/")) {
        return null;
      }

      if (className.startsWith("dev/slne/minestom/lobby/server/mixin/")) {
        return null;
      }

      String binaryName = className.replace('/', '.');

      try {
        byte[] result = transformer.transformClassBytes(binaryName, binaryName, classfileBuffer);

        if (result == classfileBuffer) {
          return null;
        }

        return result;
      } catch (Throwable throwable) {
        throw new IllegalClassFormatException(
            "Mixin transformation failed for "
                + binaryName
                + ": "
                + throwable
        );
      }
    }
  }
}
